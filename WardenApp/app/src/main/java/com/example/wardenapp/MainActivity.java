package com.example.wardenapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.text.BoringLayout;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.ParsedRequestListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    Button addButton, subButton;
    TextView availableSlots, locationName;
    ImageButton prevButton, nextButton;

    ArrayList<LocationData> locationDataList = new ArrayList<LocationData>();
    int currentIndex = 0;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // location controlling
        prevButton = (ImageButton) findViewById(R.id.prev);
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                --currentIndex;
                updateWindow();
            }
        });
        nextButton = (ImageButton) findViewById(R.id.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ++currentIndex;
                updateWindow();
            }
        });
        locationName = (TextView) findViewById(R.id.locationName);

        // slots controlling
        addButton = (Button) findViewById(R.id.increment);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationDataList.isEmpty()) return;
                updateAvailability("minus");
            }
        });
        subButton = (Button) findViewById(R.id.decrement);
        subButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationDataList.isEmpty()) return;
                updateAvailability("plus");
            }
        });
        availableSlots = (TextView) findViewById(R.id.availableSlots);

        AndroidNetworking.initialize(getApplicationContext());
        startTimer();
    }

    public void updateAvailability(String op)
    {
        if (locationDataList.size() <= currentIndex) {
            currentIndex = locationDataList.size() - 1;
        }

        if (currentIndex < 0) {
            currentIndex = 0;
        }

        if (op.equalsIgnoreCase("minus")) {
            LocationData l = locationDataList.get(currentIndex);
            --l.bc;
            if (l.bc < 0)
                l.bc = 0;
        }
        if (op.equalsIgnoreCase("plus")) {
            LocationData l = locationDataList.get(currentIndex);
            ++l.bc;
            if ( l.bc > l.slots)
                l.bc = l.slots;
        }

        // how updated slots in window
        updateWindow();

        if (locationDataList.isEmpty()) return;

        String host  = getString(R.string.host);
        int locationId = locationDataList.get(currentIndex).locationId;
        System.out.println("updateAvailability: " + host + " op:" + op);
        AndroidNetworking.put("http://" + host + "/availability/" + op + "?location-id=" + locationId)
        .build()
        .getAsJSONObject(new JSONObjectRequestListener() {
            @Override
            public void onResponse(JSONObject response) {
                System.out.println("------ updateAvailability: " + response);
            }
            @Override
            public void onError(ANError error) {
                System.out.println("--------error: updateAvailability: " + error);
            }
        });
    }

    public void updateWindow()
    {
        if (locationDataList.size() <= currentIndex) {
            currentIndex = locationDataList.size() - 1;
        }

        if (currentIndex < 0) {
            currentIndex = 0;
        }

        if (locationDataList.size() > currentIndex) {
            LocationData l = locationDataList.get(currentIndex);
            locationName.setText(l.locationName + ", "+ l.city);
            Integer freeSlots = l.slots  - l.bc;
            availableSlots.setText("Free " + freeSlots);
            availableSlots.setTextColor(getColor(R.color.colorAccent));
            if (freeSlots < 2) {
                availableSlots.setTextColor(getColor(R.color.red));
            }
        }

        if (locationDataList.isEmpty()) {
            locationName.setText("");
            availableSlots.setText("");
            availableSlots.setTextColor(getColor(R.color.colorAccent));
        }
    }

    public void startTimer() {
        timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("get locations...");
                        loadMyLocations();
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 10000);
    }

    public void loadMyLocations()
    {
        String host  = getString(R.string.host);
        String myId = getString(R.string.myid);
        System.out.println("loadData: " + host + " by:" + myId);
        AndroidNetworking.get("http://" + host + "/my-locations/" + myId)
        .build()
        .getAsJSONArray(new JSONArrayRequestListener() {
            @Override
            public void onResponse(JSONArray response) {
                System.out.println("------ my loactions: " + response.length());
                // clear exsisting data before add new
                locationDataList.clear();
                for(int i = 0; i < response.length(); ++i) {
                    try {
                        JSONObject location = response.getJSONObject(i);

                        LocationData l = new LocationData();
                        l.locationId = location.getInt("id");
                        l.slots = location.getInt("slots");
                        l.bc = location.getInt("bc");
                        l.city = location.getString("city");
                        l.locationName = location.getString("lname");

                        locationDataList.add(l);
                        System.out.println("------ mylocations: " + l.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                updateWindow();
            }
            @Override
            public void onError(ANError error) {
                System.out.println("--------error: get ownloations: " + error);
            }
        });
    }
}
