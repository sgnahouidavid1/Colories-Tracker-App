package com.example.coloriestrackerremastered;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryScreen extends Fragment {

    private BarChart barChart;
    private TextView tvAverageCalories, tvHighestCalories, tvLowestCalories;
    private Button btnWeek, btnMonth, btnYear;

    private DatabaseReference mDatabase;

    //for tracking the current date range selection
    private enum DateRange { WEEK, MONTH, YEAR }
    private DateRange currentRange = DateRange.WEEK;

    //for storing the calorie data from Firebase
    private Map<String, Integer> calorieDataByDate = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.history, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //initialize firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        String userId = currentUser.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(userId).child("foodEntries");

        initViews(view);
        configureBarChart();
        setButtonListeners();
        loadCalorieData();
    }

    private void initViews(View view) {
        barChart = view.findViewById(R.id.barChart);
        tvAverageCalories = view.findViewById(R.id.tvAverageCalories);
        tvHighestCalories = view.findViewById(R.id.tvHighestCalories);
        tvLowestCalories = view.findViewById(R.id.tvLowestCalories);
        btnWeek = view.findViewById(R.id.btnWeek);
        btnMonth = view.findViewById(R.id.btnMonth);
        btnYear = view.findViewById(R.id.btnYear);
    }

    private void configureBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setTouchEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setExtraBottomOffset(5f);
        barChart.setNoDataTextColor(Color.BLACK);

        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setTextSize(15f);
        barChart.getXAxis().setAxisLineWidth(2f);
        barChart.getXAxis().setAxisLineColor(Color.parseColor("#555555"));

        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisLeft().setTextSize(15f);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setAxisLineWidth(2f);
        barChart.getAxisLeft().setAxisLineColor(Color.parseColor("#555555"));
    }

    private void setButtonListeners() {
        btnWeek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentRange = DateRange.WEEK;
                loadCalorieData();
            }
        });

        btnMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentRange = DateRange.MONTH;
                loadCalorieData();
            }
        });

        btnYear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentRange = DateRange.YEAR;
                loadCalorieData();
            }
        });
    }

    private void loadCalorieData() {
        //show loading state
        barChart.setNoDataText("Loading data...");

        //get date range for query
        final Date startDate = getStartDate();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        //clear previous data
        calorieDataByDate.clear();

        //access Firebase
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //process all dates in the database
                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    String dateKey = dateSnapshot.getKey();

                    if (dateKey != null) {
                        try {
                            //parse the date from the key
                            Date date = dateFormat.parse(dateKey);

                            //check if this date is within our range
                            if (date != null && !date.before(startDate)) {
                                //process all food entries for this date
                                int totalCalories = 0;
                                for (DataSnapshot entrySnapshot : dateSnapshot.getChildren()) {
                                    //get calories value from entry
                                    Integer calories = entrySnapshot.child("calories").getValue(Integer.class);
                                    if (calories != null) {
                                        totalCalories += calories;
                                    }
                                }

                                calorieDataByDate.put(dateKey, totalCalories);
                            }
                        } catch (ParseException e) {
                            //skip invalid date format
                            e.printStackTrace();
                        }
                    }
                }

                //fill in missing dates with zero calories
                fillMissingDates(startDate);

                //update chart and statistics
                updateBarChart();
                updateStatistics();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getView().getContext(),
                        "Failed to load data: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                barChart.setNoDataText("Failed to load data");
            }
        });
    }

    private Date getStartDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        switch (currentRange) {
            case WEEK:
                calendar.add(Calendar.DAY_OF_YEAR, -6); // Last 7 days (including today)
                break;
            case MONTH:
                calendar.add(Calendar.MONTH, -1);
                break;
            case YEAR:
                calendar.add(Calendar.YEAR, -1);
                break;
        }

        return calendar.getTime();
    }

    private void fillMissingDates(Date startDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        //get the end date (today)
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.set(Calendar.HOUR_OF_DAY, 0);
        endCalendar.set(Calendar.MINUTE, 0);
        endCalendar.set(Calendar.SECOND, 0);
        endCalendar.set(Calendar.MILLISECOND, 0);

        //fill all dates between start and end
        while (!calendar.after(endCalendar)) {
            String dateString = sdf.format(calendar.getTime());

            //check if date has data yet
            if (!calorieDataByDate.containsKey(dateString)) {
                calorieDataByDate.put(dateString, 0);
            }

            //go to next day
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void updateBarChart() {
        List<BarEntry> barEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        SimpleDateFormat displayFormat;
        boolean showMonthlyAverages = false;

        //choose format based on date range
        switch (currentRange) {
            case YEAR:
                displayFormat = new SimpleDateFormat("MMM", Locale.US);
                showMonthlyAverages = true;
                break;
            case MONTH:
                displayFormat = new SimpleDateFormat("dd", Locale.US);
                break;
            default:
                displayFormat = new SimpleDateFormat("EEE", Locale.US);
                break;
        }

        //convert data into chart format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        int index = 0;

        //sort the dates
        List<String> sortedDates = new ArrayList<>(calorieDataByDate.keySet());
        Collections.sort(sortedDates);

        if (showMonthlyAverages) {
            //for year view, group by month and calculate averages
            Map<String, List<Integer>> caloriesByMonth = new HashMap<>();

            //group calories by month
            for (String dateString : sortedDates) {
                try {
                    Date date = dateFormat.parse(dateString);
                    if (date == null) continue;

                    //extract month
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    String monthKey = new SimpleDateFormat("yyyy-MM", Locale.US).format(date);

                    //add to month group
                    if (!caloriesByMonth.containsKey(monthKey)) {
                        caloriesByMonth.put(monthKey, new ArrayList<>());
                    }

                    int calories = calorieDataByDate.get(dateString);
                    //only include non-zero values in monthly average
                    if (calories > 0) {
                        caloriesByMonth.get(monthKey).add(calories);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            //calculate monthly averages
            SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
            List<String> sortedMonths = new ArrayList<>(caloriesByMonth.keySet());
            Collections.sort(sortedMonths);

            for (String monthKey : sortedMonths) {
                List<Integer> monthValues = caloriesByMonth.get(monthKey);
                float average = 0;

                //calculate average if there are values
                if (!monthValues.isEmpty()) {
                    int sum = 0;
                    for (int value : monthValues) {
                        sum += value;
                    }
                    average = (float) sum / monthValues.size();
                }

                try {
                    Date monthDate = monthFormat.parse(monthKey);
                    if (monthDate != null) {
                        barEntries.add(new BarEntry(index, average));
                        labels.add(displayFormat.format(monthDate));
                        index++;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        } else {
            //for week and month views, show daily values
            for (String dateString : sortedDates) {
                try {
                    Date date = dateFormat.parse(dateString);
                    if (date != null) {
                        float calories = calorieDataByDate.get(dateString);
                        barEntries.add(new BarEntry(index, calories));

                        //format the label based on the date range
                        labels.add(displayFormat.format(date));

                        index++;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        //create the data set
        BarDataSet dataSet = new BarDataSet(barEntries, currentRange == DateRange.YEAR ? "Average Calories Per Month" : "Calories");
        dataSet.setColor(Color.parseColor("#348F85"));
        dataSet.setValueTextSize(15f);

        //create the bar data
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);

        //set labels for x-axis
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        //set data to chart
        barChart.setData(barData);

        //for year view (showing monthly averages), display fewer decimal places
        if (currentRange == DateRange.YEAR) {
            barData.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.format(Locale.US, "%.0f", value);
                }
            });
        }

        barChart.invalidate(); //force refresh
    }

    private void updateStatistics() {
        if (calorieDataByDate.isEmpty()) {
            tvAverageCalories.setText("0");
            tvHighestCalories.setText("0");
            tvLowestCalories.setText("0");
            return;
        }

        //calculate sum and find highest/lowest
        int sum = 0;
        int highest = Integer.MIN_VALUE;
        int lowest = Integer.MAX_VALUE;
        int count = 0;

        for (int calories : calorieDataByDate.values()) {
            //exclude blank days from calculations
            if (calories > 0) {
                sum += calories;
                count++;

                if (calories > highest) highest = calories;
                if (calories < lowest) lowest = calories;
            }
        }

        //calculate average (avoiding division by zero)
        int average;
        if(count > 0)
            average = sum / count;
        else
            average = 0;

        tvAverageCalories.setText(String.valueOf(average));
        tvHighestCalories.setText(String.valueOf(highest == Integer.MIN_VALUE ? 0 : highest));
        tvLowestCalories.setText(String.valueOf(lowest == Integer.MAX_VALUE ? 0 : lowest));
    }
}