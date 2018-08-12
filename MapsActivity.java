package com.lilach.mapsandareas;

import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;



public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    private ArrayList<CoordinateObject> coordinates = new ArrayList<>();
    private double maxY,
                   minY,
                   maxX,
                   minX;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // get coordinates from file
        getCoordinates();

    }

    private void getCoordinates() {

        try {
            InputStream in = getAssets().open("allowed_area.kml");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String all = "";
            String coordinateArray = "";
            String line = null;

            while ((line = reader.readLine()) != null){
               all += line.trim();
            }

            all = all.substring(all.indexOf("<coordinates>")+13,all.indexOf("</coordinates>"));
            String[] coor = all.split(",0 ");
            for(int i = 0; i < coor.length-1; i++){
                String[] longLat = coor[i].split(",");
                coordinates.add(new CoordinateObject(Double.parseDouble(longLat[0]),Double.parseDouble(longLat[1])));
            }

            // find edge coordinates
            maxY = coordinates.get(0).getLatitude();
            minY = coordinates.get(0).getLatitude();
            maxX = coordinates.get(0).getLongitude();
            minX = coordinates.get(0).getLongitude();


            for (CoordinateObject cObject : coordinates){
                if(cObject.getLatitude() > maxY){
                    maxY = cObject.getLatitude();
                }

                if(cObject.getLatitude() < minY){
                    minY = cObject.getLatitude();
                }

                if(cObject.getLongitude() > maxX){
                    maxX = cObject.getLongitude();
                }

                if(cObject.getLongitude() < minX){
                    minX = cObject.getLongitude();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // create polygon for drawing on map
        PolygonOptions pOptions = new PolygonOptions();
        for (CoordinateObject obj : coordinates){
            pOptions.add(new LatLng(obj.getLatitude(),obj.getLongitude()));
        }

        pOptions.strokeWidth(2);
        pOptions.strokeColor(Color.BLUE);
        pOptions.fillColor(Color.GREEN);


        // set map options

        mMap.addPolygon(pOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(coordinates.get(0).getLatitude(),coordinates.get(0).getLongitude()),12));
        mMap.setMinZoomPreference(7);
        mMap.setMaxZoomPreference(20);

        mMap.setOnMapClickListener(this);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        checkTouchPoint(latLng);
    }

    private void checkTouchPoint(LatLng latLng) {
        Log.d("meir",latLng.latitude + " " + latLng.longitude);
        String title = "";

        if(latLng.latitude < minY){
            title = checkDistanceFromPolygon(latLng, "bottom");
        } else if(latLng.latitude > maxY){
            title = checkDistanceFromPolygon(latLng,"top");
        }else if (latLng.longitude < minX){
            title = checkDistanceFromPolygon(latLng,"left");
        }else if(latLng.longitude > maxX){
            title = checkDistanceFromPolygon(latLng,"right");
        }else{  // in bounds

            ArrayList<CoordinateObject> inRange = new ArrayList<>();
            ArrayList<Double> closest = new ArrayList<>();
            int next;

            for(int c = 0; c < coordinates.size(); c++){

                if(c == coordinates.size()-1){
                    next = 0;
                }else{
                    next = c+1;
                }

                if(coordinates.get(c).getLatLng() == latLng){
                    title = "in bounds";
                }else{
                    double x1 = coordinates.get(c).getLongitude();
                    double x2 = coordinates.get(next).getLongitude();
                    double y1 = coordinates.get(c).getLatitude();
                    double y2 = coordinates.get(next).getLatitude();
                    double x = latLng.longitude;
                    double y = latLng.latitude;


                    if(x2 > x1) {
                        if (x > x1 && x < x2) {
                            // latLngX within range create f(x) = ax + b and pose x to get y range
                            // enter x1 in x1*a + b = y1 to find b
                            double a = (y2 - y1) / (x2 - x1);
                            double b = y1 - (x1 * a);

                            // newY = ax+b pose latLngX to get y
                            double newY = a * x + b;
                            if (newY == y) {
                                title = "in bounds";
                            } else {
                                inRange.add(new CoordinateObject(x, newY));
                            }
                        }
                    }

                    if(x2 < x1){
                        if (x < x1 && x > x2) {
                            // latLngX within range create f(x) = ax + b and pose x to get y range
                            // enter x1 in x1*a + b = y1 to find b
                            double a = (y2 - y1) / (x2 - x1);
                            double b = y1 - (x1 * a);

                            // newY = ax+b pose latLngX to get y
                            double newY = a * x + b;
                            if (newY == y) {
                                title = "in bounds";
                            } else {
                                inRange.add(new CoordinateObject(x, newY));
                            }
                        }
                    }
                }
            }

            if(inRange.size() > 0){

                int greaterThanLat = 0, lessThanLat = 0;
                for(CoordinateObject obj : inRange){
                    if(obj.getLatitude() > latLng.latitude){
                        greaterThanLat ++;
                    }else{
                        lessThanLat ++;
                    }
                }

                if((greaterThanLat % 2) == 0 && (lessThanLat % 2) == 0){
                    title = checkDistanceFromPolygon(latLng,"");
                }else{
                    title = "in bounds";
                }

                inRange.clear();
            }

        }
        mMap.addMarker(new MarkerOptions().position(latLng).title(title)).showInfoWindow();

    }

    private String checkDistanceFromPolygon(LatLng latLng, String direction) {

            String title = "";
            CoordinateObject closestY = null;
            CoordinateObject closestX = null;
            String cxPosition = "";
            String cyPosition = "";
            int next;

            double firstX = coordinates.get(0).getLongitude();
            double firstY = coordinates.get(0).getLatitude();

            for(int c = 0; c < coordinates.size(); c++) {


                double x1 = coordinates.get(c).getLongitude();
                double y1 = coordinates.get(c).getLatitude();
                double x = latLng.longitude;
                double y = latLng.latitude;


                if(direction.equals("top")) {
                    if((y-y1) < firstY){
                        closestY = new CoordinateObject(x1,y1);
                        firstY = y-y1;
                        cyPosition = String.valueOf(c);
                    }
                }

                if(direction.equals("bottom")){
                    if((y1-y)< firstY){
                        closestY = new CoordinateObject(x1,y1);
                        firstY = y1-y;
                        cyPosition = String.valueOf(c);
                    }
                }

                if(direction.equals("right")){
                    if ((x-x1) < firstX){
                        closestX = new CoordinateObject(x1,y1);
                        firstX = x-x1;
                        cxPosition = String.valueOf(c);
                    }
                }

                if(direction.equals("left")){
                    if ((x1-x) < firstX){
                        closestX = new CoordinateObject(x1,y1);
                        firstX = x1-x;
                        cxPosition = String.valueOf(c);
                    }
                }

                if(direction.equals("")){
                    if ((x1-x) < firstX){
                        closestX = new CoordinateObject(x1,y1);
                        firstX = x1-x;
                        cxPosition = String.valueOf(c);
                    }
                    if ((x-x1) < firstX){
                        closestX = new CoordinateObject(x1,y1);
                        firstX = x-x1;
                        cxPosition = String.valueOf(c);
                    }
                    if((y1-y)< firstY){
                        closestY = new CoordinateObject(x1,y1);
                        firstY = y1-y;
                        cyPosition = String.valueOf(c);
                    }
                    if((y-y1) < firstY){
                        closestY = new CoordinateObject(x1,y1);
                        firstY = y-y1;
                        cyPosition = String.valueOf(c);
                    }
                }
            }


            if(closestX != null && cxPosition != ""){

                int position = Integer.parseInt(cxPosition);

                if (position == coordinates.size() - 1) {
                    next = 0;
                }else{
                    next = position + 1;
                }

                double x1 = coordinates.get(position).getLongitude();
                double x2 = coordinates.get(next).getLongitude();
                double y1 = coordinates.get(position).getLatitude();
                double y2 = coordinates.get(next).getLatitude();
                double x = latLng.longitude;
                double y = latLng.latitude;

                double midX = (x1+x2)/2;
                double midY = (y1+y2)/2;
                float[] dis = new float[1];
                Location.distanceBetween(y,x,midY,midX,dis);
                title = String.valueOf(dis[0]/1000 + " km");

                closestX = null;
                cxPosition = "";
            }

            if(closestY != null && cyPosition != ""){

                int position = Integer.parseInt(cyPosition);

                if (position == coordinates.size() - 1) {
                    next = 0;
                }else{
                    next = position + 1;
                }

                double x1 = coordinates.get(position).getLongitude();
                double x2 = coordinates.get(next).getLongitude();
                double y1 = coordinates.get(position).getLatitude();
                double y2 = coordinates.get(next).getLatitude();
                double x = latLng.longitude;
                double y = latLng.latitude;

                double midX = (x1+x2)/2;
                double midY = (y1+y2)/2;
                float[] dis = new float[1];
                Location.distanceBetween(y,x,midY,midX,dis);
                title = String.valueOf(dis[0]/1000 + " km");

                closestY = null;
                cyPosition = "";
            }

        return title;
    }
}


/*
 // find vertical line to f(x) = ax + b  =  -1/a * x + b
// vertical line is the shortest distance

                                double b2 = y - x*(-1/a);
                                //  z(-1/a) + b2 = z*a + b => (b2-b) = (z*a) - (z*(-1/a))
                                double zx = (b2-b)*a / (a*a + 1);
                                double zy = zx*a + b;
                                Log.d("meir","distance = " + "("+ zx +"," + zy + ")");
                                float[] dis = new float[1];
                                Location.distanceBetween(y,x,zy,zx,dis);
 */