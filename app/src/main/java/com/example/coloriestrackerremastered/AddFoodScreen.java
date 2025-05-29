package com.example.coloriestrackerremastered;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AddFoodScreen extends AppCompatActivity {

    private String selectedDate;
    private TextInputEditText dateEditText;
    private TextInputEditText foodNameEditText;
    private TextInputEditText caloriesEditText;
    private DatabaseReference databaseReference;

    private TextView ingredientText;

    private TextView listOfNutritionDetails;

   String nutrientName;

   Integer fdcIdNum;
   String ingredients;
   Double value;
   String unit;

   String fullNutritionDetails = "";

   String foodName;

   String description;

   String brand;
    private DateAdapter date;
    private SoundPool soundPool;
    private int soundId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_food);

        dateEditText = findViewById(R.id.dateEditText);
        foodNameEditText = findViewById(R.id.foodNameEditText);
        caloriesEditText = findViewById(R.id.caloriesEditText);
        ingredientText = findViewById(R.id.listOfIngredients);
        listOfNutritionDetails = findViewById(R.id.listOfNutrition);
        listOfNutritionDetails.setEnabled(false);
        ingredientText.setEnabled(false);

        //set default date to today
        date = new DateAdapter(dateEditText, DateFormat.MEDIUM);
        date.updateDateField(System.currentTimeMillis());
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        //set up the date picker dialog
        dateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        //pass data
        Intent intent = getIntent();

        fdcIdNum = intent.getIntExtra("fdcId",0);
        Log.d("USDA Data", "Food's fdcId: " + fdcIdNum);

        foodName = intent.getStringExtra("Food's name");
        Log.d("USDA Data", "Food's name: " + foodName);
        foodNameEditText.setText(foodName);

        description = intent.getStringExtra("description");
        brand = intent.getStringExtra("brand");

        fetchUSDADataBrandedFoods(description,brand);

        //init firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5) //maximum concurrent sounds
                .setAudioAttributes(audioAttributes)
                .build();

        soundId = soundPool.load(this, R.raw.upload, 1);

    }

    private void showDatePickerDialog() {
        //create the Material Date Picker
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        //set up the listener for when the user selects a date
        datePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
            @Override
            public void onPositiveButtonClick(Long selection) {
                date.updateDateField(selection);
                //update our selected date in the format Firebase expects
                selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(selection));
            }
        });

        //show the date picker
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    public void addFoodToFirebase(View view) {

        //play sound
        soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);


        //retrieve data from EditTexts
        String foodName = foodNameEditText.getText().toString().trim();
        String caloriesStr = caloriesEditText.getText().toString().trim();

        //check if the fields are empty
        if (foodName.isEmpty() || caloriesStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return; //exit the method if any field is empty
        }

        //parse data
        int calories;
        try {
            calories = Integer.parseInt(caloriesStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for calories and quantity", Toast.LENGTH_SHORT).show();
            return; //exit if there's an error parsing numbers
        }

        //get current user and date
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return; //exit if no user is logged in
        }
        String userId = currentUser.getUid();

        //use the selectedDate variable instead of creating a new date
        String currentDate = selectedDate;

        //data to be sent to Firebase
        Map<String, Object> foodData = new HashMap<>();
        foodData.put("foodName", foodName);
        foodData.put("calories", calories);

        //push data to Firebase
        databaseReference.child("users").child(userId).child("foodEntries").child(currentDate).push().setValue(foodData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Food added successfully", Toast.LENGTH_SHORT).show();
                    //clear the input fields
                    foodNameEditText.setText("");
                    caloriesEditText.setText("");
                })
                .addOnFailureListener(e -> {
                    //error adding data
                    Log.e("AddFoodScreen", "Error adding food to Firebase", e);
                    Toast.makeText(this, "Error adding food", Toast.LENGTH_SHORT).show();
                });
    }

    public void goHome(View view) {
        Intent intent = new Intent(this, FrameActivity.class);
        startActivity(intent);
        finish();
    }

    private  void fetchUSDADataBrandedFoods (String query, String brandOwner) {
        String url = "https://api.nal.usda.gov/fdc/v1/foods/search?query=%20" + query + "&dataType=Branded&pageSize=1&brandOwner=%20" + brandOwner + "&api_key=QBUeC7u2yP5nFrmu4wOQ7OcdpMLJ3p1yogk07bjY";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("USDA API","Request Failed", e );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    try {
                        String jsonData = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(jsonData);
                            JSONArray foods = jsonObject.getJSONArray("foods");
                            for (int start = 0; start < foods.length(); start++){
                                JSONObject food = foods.getJSONObject(start);
                                JSONArray foodNutrients = food.getJSONArray("foodNutrients");
                                for (int start1 = 0; start1 < foodNutrients.length(); start1++){
                                    JSONObject foodNutrient = foodNutrients.getJSONObject(start1);
                                    nutrientName = foodNutrient.getString("nutrientName");
                                    value = foodNutrient.getDouble("value");
                                    unit = foodNutrient.getString("unitName");
                                    Log.d("USDA Data","Nutrient's name: " + nutrientName + " Nutrient's value: " + value +  " Nutrient's unit: " + unit);
                                    fullNutritionDetails = fullNutritionDetails + nutrientName + " " + value + " " + unit + ", ";
                                }
                                ingredients = food.getString("ingredients");
                                Log.d("USDA Data","Food's ingredients: " + ingredients);
                            }

                            //update UI on the main thread
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ingredientText.setText(ingredients);
                                    listOfNutritionDetails.setText(fullNutritionDetails);
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    } catch (Exception e) {
                        Log.e("USDA API", "Parsing Error", e);
                    }
                } else {
                    Log.e("USDA API", "Response Failed" + response.code());
                }
            }
        });
    }
}