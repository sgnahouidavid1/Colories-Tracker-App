package com.example.coloriestrackerremastered;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreen extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.splash_screen);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is signed in, redirect to MainScreen
            goHome();
        }
        else {
            goToCreateAccount();
        }
    }

    private void goHome() {
        Intent intent = new Intent(this, FrameActivity.class);
        startActivity(intent);
        finish(); // Close this activity so user can't go back to splash screen
    }

    public void goToCreateAccount() {
        Intent intent = new Intent(this, CreateAccount.class);
        startActivity(intent);
        finish(); // Close this activity so user can't go back to splash screen
    }
}