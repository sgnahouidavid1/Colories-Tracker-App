package com.example.coloriestrackerremastered;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity {

    // UI Elements
    private TextInputLayout loginEmail, loginPassword;

    // Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
    }

    public void logIn(View view) {
        // Get email and password
        String email = loginEmail.getEditText().getText().toString().trim();
        String password = loginPassword.getEditText().getText().toString().trim();

        // Validate inputs
        if (validateForm(email, password)) {

            // Attempt login
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success
                                Toast.makeText(Login.this, "Login successful!",
                                        Toast.LENGTH_SHORT).show();
                                // Go to main screen
                                goToMainScreen();
                            } else {
                                // If sign in fails, display a message to the user
                                Toast.makeText(Login.this, "Authentication failed: " +
                                        task.getException().getMessage(), Toast.LENGTH_SHORT).show();

                                // Clear password field
                                loginPassword.getEditText().setText("");
                            }
                        }
                    });
        }
    }

    private boolean validateForm(String email, String password) {
        boolean valid = true;

        // Validate email
        if (TextUtils.isEmpty(email)) {
            loginEmail.setError("Email is required");
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginEmail.setError("Please enter a valid email address");
            valid = false;
        } else {
            loginEmail.setError(null);
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            loginPassword.setError("Password is required");
            valid = false;
        } else {
            loginPassword.setError(null);
        }

        return valid;
    }

    private void goToMainScreen() {
        Intent intent = new Intent(this, FrameActivity.class);
        // Clear the back stack so user can't go back to login
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void goToCreateAccount(View view) {
        Intent intent = new Intent(this, CreateAccount.class);
        startActivity(intent);
    }

    // Method to handle forgotten password - could add a TextView in XML for this
    public void resetPassword(View view) {
        String email = loginEmail.getEditText().getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            loginEmail.setError("Enter your email to reset password");
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(Login.this,
                                    "Password reset email sent!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Login.this,
                                    "Failed to send reset email: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}