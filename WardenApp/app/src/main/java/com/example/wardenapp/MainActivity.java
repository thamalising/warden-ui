package com.example.wardenapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.StringRequestListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    Button addButton, subButton, loginButton;
    EditText token;
    String myWardenId = "";
    TextView availableSlots, locationName;
    ImageButton prevButton, nextButton;

    ArrayList<LocationData> locationDataList = new ArrayList<LocationData>();
    int currentIndex = 0;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //just initialize both login and warden related objects
        initLoginView();
        initWardenView();
        //visible only login view
        toggleToLogin(true);

        AndroidNetworking.initialize(getApplicationContext());
    }

    public void initLoginView(){
        token = (EditText) findViewById(R.id.token);

        loginButton = (Button) findViewById(R.id.login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (token.length() == 0) {
                    String message = "Empty Token";
                    token.setText(message);
                    token.setSelection(0, message.length());
                    return;
                }
                useToken();
            }
        });
    }

    //get Warden ID from token
    public void useToken() {
        String host  = getString(R.string.host);
        System.out.println("useToken: " + host + "|" + token.getText());
        AndroidNetworking.get("http://" + host + "/token/" + token.getText())
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String wId) {
                        if (wId.isEmpty()) {
                            String invalidmsg = "Invalid Token";
                            token.setText(invalidmsg);
                            token.setSelection(0, invalidmsg.length());
                            // login failed
                            return;
                        }
                        // login success
                        myWardenId = wId;
                        toggleToLogin(false);
                        startTimer();
                    }
                    @Override
                    public void onError(ANError error) {
                        System.out.println("--------useToken: " + error);
                    }
                });
    }

    public void toggleToLogin(boolean loginView) {
        token.setVisibility(loginView ? View.VISIBLE : View.INVISIBLE);
        loginButton.setVisibility(loginView ? View.VISIBLE : View.INVISIBLE);

        prevButton.setVisibility(loginView ? View.INVISIBLE : View.VISIBLE);
        nextButton.setVisibility(loginView ? View.INVISIBLE : View.VISIBLE);
        locationName.setVisibility(loginView ? View.INVISIBLE : View.VISIBLE);
        addButton.setVisibility(loginView ? View.INVISIBLE : View.VISIBLE);
        subButton.setVisibility(loginView ? View.INVISIBLE : View.VISIBLE);
        availableSlots.setVisibility(loginView ? View.INVISIBLE : View.VISIBLE);
    }

    public void initWardenView(){
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
                updateAvailability("minus");
            }
        });
        subButton = (Button) findViewById(R.id.decrement);
        subButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateAvailability("plus");
            }
        });
        availableSlots = (TextView) findViewById(R.id.availableSlots);
    }

    public void updateAvailability(String op)
    {
        if (locationDataList.isEmpty()) return;

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

        //update backend
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

    //show locations and available slots on window
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
        // 10 sec timer to get location from web-service
        timer.scheduleAtFixedRate(timerTask, 0, 10000);
    }

    //get locations into location data list
    public void loadMyLocations()
    {
        String host  = getString(R.string.host);
        System.out.println("loadData: " + host + " by:" + myWardenId);
        AndroidNetworking.get("http://" + host + "/my-locations/" + myWardenId)
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
