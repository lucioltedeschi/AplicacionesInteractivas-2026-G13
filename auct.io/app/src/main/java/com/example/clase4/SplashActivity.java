package com.example.clase4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 1800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences preferences = getSharedPreferences("sesion", MODE_PRIVATE);
            int userId = preferences.getInt("userId", 0);

            Intent intent;

            if (userId > 0) {
                intent = new Intent(SplashActivity.this, HomeActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(intent);
            finish();

        }, SPLASH_TIME);
    }
}