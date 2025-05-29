package com.example.coloriestrackerremastered;

import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;

public class FrameActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private SoundPool soundPool;
    private int soundId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frame);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MainScreen())
                    .commit();
        }

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5) //maximum concurrent sounds
                .setAudioAttributes(audioAttributes)
                .build();

        soundId = soundPool.load(this, R.raw.button, 1);

    }

    //navigation item selection listener
    private BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        Fragment selectedFragment = null;
        soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);

        // Track current and new position for directional animations
        int currentItem = bottomNavigationView.getSelectedItemId();
        int newItem = item.getItemId();
        boolean slideLeft;

        //gives order on navbar for choosing animations
        HashMap<Integer, Integer> fragmentMap = new HashMap<>();
        fragmentMap.put(R.id.navigation_home, 0);
        fragmentMap.put(R.id.navigation_search, 1);
        fragmentMap.put(R.id.navigation_history, 2);
        fragmentMap.put(R.id.navigation_profile, 3);

        if(fragmentMap.get(newItem) > fragmentMap.get(currentItem))
            slideLeft = true;
        else
            slideLeft = false;

        // Determine which fragment to display
        if(item.getItemId() == R.id.navigation_home)
            selectedFragment = new MainScreen();
        else if(item.getItemId() == R.id.navigation_search)
            selectedFragment = new SearchFoodScreenBrand();
        else if(item.getItemId() == R.id.navigation_history)
            selectedFragment = new HistoryScreen();
        else if(item.getItemId() == R.id.navigation_profile)
            selectedFragment = new ProfileScreen();

        // Replace with animation
        if (selectedFragment != null && newItem != currentItem) {
            // Choose animation direction based on navigation direction
            if (slideLeft) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(
                                R.anim.slide_in_right,
                                R.anim.slide_out_left
                        )
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            } else {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(
                                R.anim.slide_in_left,
                                R.anim.slide_out_right
                        )
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
        }

        return true;
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        soundPool.release();
        soundPool = null;
    }
}