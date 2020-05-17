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

public class MainActivity extends AppCompatActivity {

    Button addButton, subButton;
    TextView availableSlots, locationName;
    ImageButton prevButton, nextButton;

    ArrayList<LocationData> locationDataList = new ArrayList<LocationData>();
    int currentIndex = 0;

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
                if (currentIndex < 0) {
                    currentIndex = locationDataList.size()  -1;
                }
                execute();
            }
        });
        nextButton = (ImageButton) findViewById(R.id.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ++currentIndex;
                if (locationDataList.size() <= currentIndex) {
                    currentIndex = 0;
                    loadMyLocations();
                } else {
                    execute();
                }
            }
        });
        locationName = (TextView) findViewById(R.id.locationName);

        // slots controlling
        addButton = (Button) findViewById(R.id.increment);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationDataList.isEmpty()) return;

                LocationData l = locationDataList.get(currentIndex);
                --l.bc;
                if (l.bc < 0) {
                    l.bc = 0;
                }
                execute();
                updateAvailability("minus");
            }
        });
        subButton = (Button) findViewById(R.id.decrement);
        subButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationDataList.isEmpty()) return;

                LocationData l = locationDataList.get(currentIndex);
                ++l.bc;
                if ( l.bc > l.slots){
                    l.bc = l.slots;
                }
                execute();
                updateAvailability("plus");
            }
        });
        availableSlots = (TextView) findViewById(R.id.availableSlots);

        AndroidNetworking.initialize(getApplicationContext());
        loadMyLocations();
    }

    public void execute()
    {
        String host  = getString(R.string.host);
        String myId = getString(R.string.myid);
        AndroidNetworking.get("http://" + host + "/ami/" + myId)
        .build()
        .getAsJSONObject(new JSONObjectRequestListener() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (0 == response.getInt("locations")) {
                        locationDataList.clear();
                    }
                    showCurrent();
                } catch (JSONException e) {
                    System.out.println("------ error: isValid: " + response);
                }
            }
            @Override
            public void onError(ANError error) {
                System.out.println("--------error: isValid: " + error);
            }
        });
    }

    public void updateAvailability(String op)
    {
        if (locationDataList.isEmpty()) return;

        String host  = getString(R.string.host);
        int locationId = locationDataList.get(currentIndex).locationId;
        System.out.println("updateAvailability: " + host + " op:" + op);
        AndroidNetworking.put("http://" + host + "/availability/" + op + "?location-id=" + locationId)
        .build()
        .getAsJSONArray(new JSONArrayRequestListener() {
            @Override
            public void onResponse(JSONArray response) {
                System.out.println("------ updateAvailability: " + response);
            }
            @Override
            public void onError(ANError error) {
                System.out.println("--------error: updateAvailability: " + error);
            }
        });
    }

    public void showCurrent()
    {
        if (locationDataList.size() > 0)
        {
            LocationData l = locationDataList.get(currentIndex);
            locationName.setText(l.locationName + ", "+ l.city);
            Integer freeSlots = l.slots  - l.bc;
            availableSlots.setText("Free " + freeSlots);
            availableSlots.setTextColor(getColor(R.color.colorAccent));
            if (freeSlots < 2) {
                availableSlots.setTextColor(getColor(R.color.red));
            }
        }

        if (locationDataList.isEmpty())
        {
            locationName.setText("");
            availableSlots.setText("");
            availableSlots.setTextColor(getColor(R.color.colorAccent));
        }
    }

    public void loadMyLocations()
    {
        locationDataList.clear();
        String host  = getString(R.string.host);
        String myId = getString(R.string.myid);
        System.out.println("loadData: " + host + " by:" + myId);
        AndroidNetworking.post("http://" + host + "/my-locations/" + myId)
            .build()
            .getAsJSONArray(new JSONArrayRequestListener() {
                @Override
                public void onResponse(JSONArray response) {
                    System.out.println("------ my loactions: " + response.length());
                    for(int i = 0; i < response.length(); ++i) {

                        try {
                            JSONObject location = response.getJSONObject(i);
//                                System.out.println("------ my loactions: " + location);
                            Integer locationId = location.getInt("id");
                            Integer slots = location.getInt("slots");
                            Integer bc = location.getInt("bc");
                            String city = location.getString("city");
                            String name = location.getString("lname");

                            LocationData l = new LocationData();
                            l.locationId = locationId;
                            l.slots = slots;
                            l.bc = bc;
                            l.city = city;
                            l.locationName = name;

                            locationDataList.add(l);
                            System.out.println("------ mylocations: "
                                    + i + "[id:" +locationId +" bc:"+bc +"/" + slots +"]");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    if (locationDataList.size() > 0) {
                        currentIndex = 0;
                        showCurrent();
                    }
                }
                @Override
                public void onError(ANError error) {
                    System.out.println("--------error: get ownloations: " + error);
                }
            });

    }
}
