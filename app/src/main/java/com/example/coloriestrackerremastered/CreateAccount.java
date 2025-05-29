package com.example.coloriestrackerremastered;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class CreateAccount extends AppCompatActivity {

    // UI Elements
    private TextInputLayout regFirstName, regLastName, regEmail, regPassword;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.create_account);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference().child("users");

        // Initialize UI elements
        regFirstName = findViewById(R.id.reg_first_name);
        regLastName = findViewById(R.id.reg_last_name);
        regEmail = findViewById(R.id.reg_email);
        regPassword = findViewById(R.id.reg_password);
    }

    public void registerUser(View view) {
        // Get values from form fields
        String firstName = regFirstName.getEditText().getText().toString().trim();
        String lastName = regLastName.getEditText().getText().toString().trim();
        String email = regEmail.getEditText().getText().toString().trim();
        String password = regPassword.getEditText().getText().toString().trim();

        // Validate form fields
        if (validateForm(firstName, lastName, email, password)) {
            // Create user with email and password
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Get current user ID
                                String userId = mAuth.getCurrentUser().getUid();
                                // Create user data map
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("firstName", firstName);
                                userData.put("lastName", lastName);
                                userData.put("email", email);

                                // Save user data to Firebase Database
                                usersRef.child(userId).setValue(userData)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task <Void> task) {
                                                if (task.isSuccessful()) {
                                                    // Set display name to first name
                                                    mAuth.getCurrentUser().updateProfile(
                                                            new UserProfileChangeRequest.Builder()
                                                            .setDisplayName(firstName)
                                                            .build());

                                                    //Sent toast
                                                    Toast.makeText(CreateAccount.this,
                                                            "Account created successfully!",
                                                            Toast.LENGTH_SHORT).show();

                                                    // Navigate to main screen
                                                    goHome();
                                                } else {
                                                    // Failed to save user data
                                                    Toast.makeText(CreateAccount.this,
                                                            "Failed to save user data: " + task.getException().getMessage(),
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            } else {
                                // Failed to create user
                                Toast.makeText(CreateAccount.this,
                                        "Registration failed: " + task.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private boolean validateForm(String firstName, String lastName, String email, String password) {
        boolean valid = true;

        // Validate full name
        if (TextUtils.isEmpty(firstName)) {
            regFirstName.setError("First name is required");
            valid = false;
        } else {
            regFirstName.setError(null);
        }
        if (TextUtils.isEmpty(lastName)) {
            regLastName.setError("Last name is required");
            valid = false;
        } else {
            regLastName.setError(null);
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            regEmail.setError("Email is required");
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            regEmail.setError("Please enter a valid email address");
            valid = false;
        } else {
            regEmail.setError(null);
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            regPassword.setError("Password is required");
            valid = false;
        } else if (password.length() < 6) {
            regPassword.setError("Password must be at least 6 characters");
            valid = false;
        } else {
            regPassword.setError(null);
        }

        return valid;
    }

    public void goHome() {
        Intent intent = new Intent(this, FrameActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void goToLogin(View view) {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
    }
}