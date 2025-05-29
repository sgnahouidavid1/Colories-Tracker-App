package com.example.coloriestrackerremastered;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainScreen extends Fragment {
    private FirebaseAuth mAuth;
    private TextView helloText, dateText;
    private DateAdapter date;
    private DatabaseReference userRef, entriesRef;
    private Integer dailyCalorieGoal = 2000; //default goal, only shown if firebase fails
    private Integer caloriesConsumed = 0;
    private FrameLayout progressContainer;
    private TextView tvCaloriesConsumed, tvCaloriesRemaining, tvNoGoal;
    private ProgressBar progressCalories;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_activity, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        helloText = view.findViewById(R.id.helloText);
        dateText = view.findViewById(R.id.dateText);
        progressCalories = view.findViewById(R.id.progress_calories);
        tvCaloriesConsumed = view.findViewById(R.id.tv_calories_consumed);
        tvCaloriesRemaining = view.findViewById(R.id.tv_calories_remaining);
        tvNoGoal = view.findViewById(R.id.tv_no_goal);
        progressContainer = view.findViewById(R.id.progress_container);

        helloText.setText("Hello, " + mAuth.getCurrentUser().getDisplayName());
        date = new DateAdapter(dateText, DateFormat.LONG);
        date.updateDateField(System.currentTimeMillis());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getUid());

        userRef.child("goal").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    //user has a goal set
                    dailyCalorieGoal = Integer.parseInt(dataSnapshot.getValue(String.class));
                    showProgressView();
                    collectDailyCalories();
                } else {
                    //user doesn't have a goal set
                    showNoGoalView();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //if Firebase read fails, fall back to the default goal
                updateCalorieProgress(caloriesConsumed);
            }
        });

    }

    public void collectDailyCalories() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            updateCalorieProgress(0);
            return;
        }

        //get current date in format YYYY-MM-DD
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String today = dateFormat.format(new Date());

        //reference to today's food entries
        DatabaseReference foodEntriesRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getUid())
                .child("foodEntries")
                .child(today);

        foodEntriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int totalCalories = 0;

                // Iterate through all entries for today
                for (DataSnapshot entrySnapshot : dataSnapshot.getChildren()) {
                    // Get calories value for this entry
                    DataSnapshot caloriesSnapshot = entrySnapshot.child("calories");
                    if (caloriesSnapshot.exists()) {
                        Integer entryCalories = caloriesSnapshot.getValue(Integer.class);
                        if (entryCalories != null) {
                            totalCalories += entryCalories;
                        }
                    }
                }

                // Update UI with the total calories
                updateCalorieProgress(totalCalories);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Main Screen", "Error fetching food entries: ", databaseError.toException());
            }
        });
    }

    public void updateCalorieProgress(int caloriesConsumed) {
        this.caloriesConsumed = caloriesConsumed;

        // Calculate remaining calories
        int remaining = dailyCalorieGoal - caloriesConsumed;

        // Update text views
        tvCaloriesConsumed.setText(String.valueOf(caloriesConsumed));
        tvCaloriesRemaining.setText("remaining: " + remaining);

        // Calculate and set progress percentage
        int progressPercentage = (int) ((caloriesConsumed / (float) dailyCalorieGoal) * 100);
        progressCalories.setProgress(progressPercentage, true);
    }

    private void showProgressView() {
        progressContainer.setVisibility(View.VISIBLE);
        tvCaloriesRemaining.setVisibility(View.VISIBLE);
        tvNoGoal.setVisibility(View.GONE);
    }

    private void showNoGoalView() {
        progressContainer.setVisibility(View.GONE);
        tvCaloriesRemaining.setVisibility(View.GONE);
        tvNoGoal.setVisibility(View.VISIBLE);
    }
}