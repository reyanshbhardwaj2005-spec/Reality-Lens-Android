package com.example.realiylens;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.realiylens.network.RetrofitClient;
import com.example.realiylens.network.SubmitResponse;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SnippingService extends Service {
    private static final String CHANNEL_ID = "SnippingServiceChannel";
    private WindowManager windowManager;
    private SelectionView selectionView;
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private int mScreenWidth, mScreenHeight, mScreenDensity;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RealityLens Active")
                .setContentText("Tap and drag to capture screen")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(1, notification);
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
        mScreenDensity = metrics.densityDpi;

        setupOverlay();
    }

    private void setupOverlay() {
        selectionView = new SelectionView(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        windowManager.addView(selectionView, params);
        selectionView.setOnSelectionListener(this::captureAndHandle);
    }

    private void captureAndHandle(RectF rect) {
        if (rect.width() < 10 || rect.height() < 10) return;

        selectionView.setVisibility(android.view.View.GONE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                Bitmap bitmap = processImage(image, rect);
                if (bitmap != null) {
                    // Save to gallery
                    MainActivity.saveImageToGallery(this, bitmap);
                    
                    // Submit to API
                    submitCapturedImage(bitmap);
                }
                image.close();
            } else {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                finishWork();
            }
        }, 150);
    }

    private void submitCapturedImage(Bitmap bitmap) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");

        if (token.isEmpty()) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show();
            finishWork();
            return;
        }

        // Convert Bitmap to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        // Create RequestBody and MultipartBody.Part
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/png"), byteArray);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", "capture.png", requestFile);

        String authHeader = "Bearer " + token;

        // Perform network request
        RetrofitClient.getApiService().submitImage(authHeader, body).enqueue(new Callback<SubmitResponse>() {
            @Override
            public void onResponse(Call<SubmitResponse> call, Response<SubmitResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String jobId = response.body().getJobId();
                    
                    // Store job_id locally
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("last_job_id", jobId);
                    editor.apply();

                    // Launch VerificationResultActivity
                    Intent resultIntent = new Intent(SnippingService.this, VerificationResultActivity.class);
                    resultIntent.putExtra("job_id", jobId);
                    resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(resultIntent);
                } else {
                    Toast.makeText(SnippingService.this, "Submission failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
                finishWork();
            }

            @Override
            public void onFailure(Call<SubmitResponse> call, Throwable t) {
                Toast.makeText(SnippingService.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finishWork();
            }
        });
    }

    private void finishWork() {
        stopSelf();
        Intent broadcastIntent = new Intent("com.example.realiylens.FINISH_ACTIVITY");
        sendBroadcast(broadcastIntent);
    }

    private Bitmap processImage(Image image, RectF rect) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * mScreenWidth;

        Bitmap bitmap = Bitmap.createBitmap(mScreenWidth + rowPadding / pixelStride, mScreenHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        try {
            int left = Math.max(0, (int) rect.left);
            int top = Math.max(0, (int) rect.top);
            int width = Math.min(bitmap.getWidth() - left, (int) rect.width());
            int height = Math.min(bitmap.getHeight() - top, (int) rect.height());
            return Bitmap.createBitmap(bitmap, left, top, width, height);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        int resultCode = intent.getIntExtra("resultCode", 0);
        Intent data = intent.getParcelableExtra("data");

        if (data != null) {
            MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mediaProjection = mpManager.getMediaProjection(resultCode, data);
            mediaProjection.registerCallback(new MediaProjection.Callback() {}, new Handler(Looper.getMainLooper()));

            imageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 2);
            virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                    mScreenWidth, mScreenHeight, mScreenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(), null, null);
        }
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Snipping Service Channel", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (selectionView != null) windowManager.removeView(selectionView);
        if (virtualDisplay != null) virtualDisplay.release();
        if (imageReader != null) imageReader.close();
        if (mediaProjection != null) mediaProjection.stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
