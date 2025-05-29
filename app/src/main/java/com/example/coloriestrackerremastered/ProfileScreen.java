package com.example.coloriestrackerremastered;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class ProfileScreen extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference usersRef;

    public TextView nameText, emailText, goalText;
    private Button editEmailButton, editGoalButton, logoutButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        usersRef = firebaseDatabase.getReference("users");

        nameText = view.findViewById(R.id.nameText);
        emailText = view.findViewById(R.id.emailText);
        goalText = view.findViewById(R.id.goalText);
        logoutButton = view.findViewById(R.id.logoutButton);

        editEmailButton = view.findViewById(R.id.editEmail);
        editGoalButton = view.findViewById(R.id.editGoal);
        editEmailButton.setOnClickListener(v -> showEditEmailDialog());
        editGoalButton.setOnClickListener(v -> showEditGoalDialog());
        logoutButton.setOnClickListener(v -> logout());

        updateUserInfo();
    }

    private void updateUserInfo() {
        //get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            //retrieve email and set
            String userEmail = currentUser.getEmail();
            emailText.setText(userEmail);

            //retrieve name from database
            final String userId = currentUser.getUid();

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            rootRef.child(".info/connected").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean connected = snapshot.getValue(Boolean.class);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    android.util.Log.e("ProfileScreen", "Connection status check failed: " + error.getMessage());
                }
            });

            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (dataSnapshot.exists()) {
                        boolean hasGoal = dataSnapshot.hasChild("goal");
                        String goal;

                        if(hasGoal)
                            goal = dataSnapshot.child("goal").getValue(String.class);
                        else
                            goal = "No goal set";

                        String firstName = dataSnapshot.child("firstName").getValue(String.class);
                        String lastName = dataSnapshot.child("lastName").getValue(String.class); // Fixed typo here

                        final String fullName = firstName + " " + lastName;
                        nameText.setText(fullName);
                        goalText.setText(goal);
                    } else {
                        nameText.setText("User data not found");
                        goalText.setText("User data not found");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    nameText.setText("Database error");
                }
            });

            //add timeout to detect if Firebase is not responding
            new android.os.Handler().postDelayed(() -> {
                if (nameText.getText().toString().equals("Loading...")) {
                    nameText.setText("Data load timeout");
                }
            }, 5000); // 5 second timeout
        } else {
            nameText.setText("Not signed in");
            emailText.setText("");
            android.util.Log.d("ProfileScreen", "No user signed in");
        }
    }

    private void showEditEmailDialog() {
        // Create EditText for the dialog
        final EditText input = new EditText(getContext());
        input.setText(emailText.getText());

        // Build and show the dialog
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Email")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newEmail = input.getText().toString().trim();
                    if (!newEmail.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                        updateUserEmail(newEmail);
                    } else {
                        Toast.makeText(getContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditGoalDialog() {
        // Create EditText for the dialog
        final EditText input = new EditText(getContext());
        input.setText(goalText.getText());

        // Build and show the dialog
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Calorie Goal")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newGoal = input.getText().toString().trim();
                    if (validateNumber(newGoal)) {
                        updateUserGoal(newGoal);
                    } else {
                        Toast.makeText(getContext(), "Please enter a valid goal", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean validateNumber(String input) {
        if(input.isEmpty())
            return false;
        try {
            Integer.parseInt(input);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }
    private void updateUserEmail(String newEmail) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            //update email in Firebase Auth
            user.verifyBeforeUpdateEmail(newEmail)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Email updated successfully
                            emailText.setText(newEmail);
                            Toast.makeText(getContext(), "Email updated successfully", Toast.LENGTH_SHORT).show();

                            // Also update email in the database if needed
                            String userId = user.getUid();
                            usersRef.child(userId).child("email").setValue(newEmail);
                        } else {
                            // Email update failed
                            Toast.makeText(getContext(), "Failed to update email: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updateUserGoal(String newGoal) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            // Update goal in Firebase Database
            usersRef.child(userId).child("goal").setValue(newGoal)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Goal updated successfully
                            goalText.setText(newGoal);
                            Toast.makeText(getContext(), "Calorie goal updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            // Goal update failed
                            Toast.makeText(getContext(), "Failed to update goal: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void logout() {
        mAuth.signOut();
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireContext(), Login.class);
        startActivity(intent);
        requireActivity().finish();
    }
}