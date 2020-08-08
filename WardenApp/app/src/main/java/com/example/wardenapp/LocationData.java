package com.example.wardenapp;

public class LocationData {
    public int locationId;
    public String locationName;
    public String city;
    public int slots;
    public int bc;

    @Override
    public String toString() {
        return "LocationData{" +
                "locationId=" + locationId +
                ", locationName='" + locationName + '\'' +
                ", city='" + city + '\'' +
                ", slots=" + slots +
                ", bc=" + bc +
                '}';
    }
}
