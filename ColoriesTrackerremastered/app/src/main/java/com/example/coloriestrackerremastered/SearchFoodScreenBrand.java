package com.example.coloriestrackerremastered;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchFoodScreenBrand extends Fragment {
    private SearchView searchBar;
    private ListView listView;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;

    private ArrayList<FoodItem> foodItems;
    private Map<String, FoodItem> foodDictionary;
    private ArrayAdapter<String> arrayAdapter;
    private Handler mainHandler;

    // Custom class to hold food data
    private static class FoodItem {
        final int fdcId;
        final String description;
        final String brand;

        FoodItem(int fdcId, String description, String brand) {
            this.fdcId = fdcId;
            this.description = description;
            this.brand = brand;
        }

        @Override
        public String toString() {
            return description + " - " + brand;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.search_food_brand, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
        searchBar = view.findViewById(R.id.searchBar);
        listView = view.findViewById(R.id.listView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);

        // Initialize data structures
        foodItems = new ArrayList<>();
        foodDictionary = new HashMap<>();
        arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(arrayAdapter);

        // Handler for UI updates on main thread
        mainHandler = new Handler(Looper.getMainLooper());

        // Set up search bar functionality
        setupSearchBar();

        // Set up list item click listener
        setupListItemClickListener();

        // Initial UI state
        updateUIState(UIState.INITIAL);
    }

    private void setupSearchBar() {
        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.isEmpty()) {
                    updateUIState(UIState.LOADING);
                    foodItems.clear();
                    foodDictionary.clear();
                    arrayAdapter.clear();
                    fetchUSDADataBranded(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // We could implement real-time filtering here if needed
                return false;
            }
        });
    }

    private void setupListItemClickListener() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String itemText = (String) parent.getItemAtPosition(position);
                FoodItem selectedFood = foodDictionary.get(itemText);

                if (selectedFood != null) {
                    Intent intent = new Intent(getActivity(), AddFoodScreen.class);
                    intent.putExtra("fdcId", selectedFood.fdcId);
                    intent.putExtra("description", selectedFood.description);
                    intent.putExtra("brand", selectedFood.brand);
                    intent.putExtra("Food's name", itemText);
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "Error retrieving food details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchUSDADataBranded(String query) {
        // URL encode the query parameter
        String encodedQuery = query.replace(" ", "%20");
        String url = "https://api.nal.usda.gov/fdc/v1/foods/search?query=" + encodedQuery +
                "&dataType=Branded&pageSize=50&api_key=QBUeC7u2yP5nFrmu4wOQ7OcdpMLJ3p1yogk07bjY";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> {
                    updateUIState(UIState.ERROR);
                    Toast.makeText(getContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonData);
                        JSONArray foods = jsonObject.getJSONArray("foods");

                        ArrayList<String> displayItems = new ArrayList<>();

                        for (int i = 0; i < foods.length(); i++) {
                            JSONObject food = foods.getJSONObject(i);
                            int fdcId = food.getInt("fdcId");
                            String description = food.getString("description");
                            String brand = food.optString("brandOwner", "Unknown Brand");

                            FoodItem foodItem = new FoodItem(fdcId, description, brand);
                            String displayText = foodItem.toString();

                            foodDictionary.put(displayText, foodItem);
                            displayItems.add(displayText);
                        }

                        mainHandler.post(() -> {
                            arrayAdapter.clear();
                            arrayAdapter.addAll(displayItems);
                            arrayAdapter.notifyDataSetChanged();

                            if (displayItems.isEmpty()) {
                                updateUIState(UIState.EMPTY);
                            } else {
                                updateUIState(UIState.RESULTS);
                            }
                        });

                    } catch (JSONException e) {
                        mainHandler.post(() -> {
                            updateUIState(UIState.ERROR);
                            Toast.makeText(getContext(), "Error parsing data", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    mainHandler.post(() -> {
                        updateUIState(UIState.ERROR);
                        Toast.makeText(getContext(), "API error: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    // Enum for UI states
    private enum UIState {
        INITIAL, LOADING, RESULTS, EMPTY, ERROR
    }

    // Update UI components based on state
    private void updateUIState(UIState state) {
        switch (state) {
            case INITIAL:
                progressBar.setVisibility(View.GONE);
                listView.setVisibility(View.GONE);
                emptyStateTextView.setVisibility(View.VISIBLE);
                emptyStateTextView.setText("Search for food items");
                break;
            case LOADING:
                progressBar.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
                emptyStateTextView.setVisibility(View.GONE);
                break;
            case RESULTS:
                progressBar.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
                emptyStateTextView.setVisibility(View.GONE);
                break;
            case EMPTY:
                progressBar.setVisibility(View.GONE);
                listView.setVisibility(View.GONE);
                emptyStateTextView.setVisibility(View.VISIBLE);
                emptyStateTextView.setText("No results found");
                break;
            case ERROR:
                progressBar.setVisibility(View.GONE);
                emptyStateTextView.setVisibility(View.VISIBLE);
                emptyStateTextView.setText("Something went wrong. Please try again.");
                break;
        }
    }
}