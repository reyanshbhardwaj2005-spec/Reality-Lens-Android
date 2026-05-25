package com.example.realiylens;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

public class DashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        drawerLayout = findViewById(R.id.drawer_layout);
        ImageButton btnMenu = findViewById(R.id.btn_hamburger_menu);
        NavigationView navigationView = findViewById(R.id.nav_view_sidebar);

        // Open the drawer from the right side when hamburger icon is clicked
        btnMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.END);
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                // Return to login screen
                finish();
            }
            // Close drawer after selection
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        // If drawer is open, close it first
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}
