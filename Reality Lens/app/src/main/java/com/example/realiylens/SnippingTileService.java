package com.example.realiylens;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class SnippingTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("START_SNIP", true);
        // Ensure the activity is brought to the front
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (Build.VERSION.SDK_INT >= 34) { // Android 14+ (UPSIDE_DOWN_CAKE)
            // Mandatory for Android 14+ to allow background activity start from a Tile
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 
                    1001, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE,
                    options.toBundle()
            );
            startActivityAndCollapse(pendingIntent);
        } else {
            // Fallback for older Android versions
            try {
                startActivityAndCollapse(intent);
            } catch (Exception e) {
                // Some Android 12/13 devices also prefer PendingIntent
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        this, 
                        1001, 
                        intent, 
                        PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_IMMUTABLE : 0)
                );
                startActivityAndCollapse(pendingIntent);
            }
        }
    }
}
