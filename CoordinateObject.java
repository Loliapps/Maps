package com.lilach.mapsandareas;

import com.google.android.gms.maps.model.LatLng;

public class CoordinateObject {

    private double longitude;
    private double latitude;
    private LatLng latLng;

    public CoordinateObject(double longitude, double latitude){
        this.longitude = longitude;
        this.latitude = latitude;
        latLng = new LatLng(latitude,latitude);
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public LatLng getLatLng() {
        return latLng;
    }
}
