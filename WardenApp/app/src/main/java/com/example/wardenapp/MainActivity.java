package com.example.wardenapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    Button addButton, subButton;
    TextView availableSlots, locationName;
    ImageButton prevButton, nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // location controlling
        prevButton = (ImageButton) findViewById(R.id.prev);
        nextButton = (ImageButton) findViewById(R.id.next);
        locationName = (TextView) findViewById(R.id.locationName);

        // slots controlling
        addButton = (Button) findViewById(R.id.increment);
        subButton = (Button) findViewById(R.id.decrement);
        availableSlots = (TextView) findViewById(R.id.availableSlots);
    }
}
