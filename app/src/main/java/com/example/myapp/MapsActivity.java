package com.example.myapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.myapp.databinding.ActivityMapsBinding;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private MapView mMap;
    private IMapController controller;
    private GeoPoint myCurrentLocation;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private GeoPoint currentLocation;


    private boolean isUserInteracting = false;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE));

        mMap = binding.osmmap;
        mMap.setMultiTouchControls(true);

        controller = mMap.getController();

        MyLocationNewOverlay mMyLocationOverlay = new MyLocationNewOverlay(mMap);
        mMyLocationOverlay.enableMyLocation();
        mMyLocationOverlay.enableFollowLocation();
        mMyLocationOverlay.setDrawAccuracyEnabled(true);
        mMap.getOverlays().add(mMyLocationOverlay);

        controller.setZoom(10.0);
        controller.setCenter(new GeoPoint(0.0, 0.0)); // Set initial center


        Button btnRecaptureLocation = findViewById(R.id.btn_recapture_location);

        btnRecaptureLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        myCurrentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        controller.setCenter(myCurrentLocation);
                        controller.setZoom(18.0);

                        mMap.invalidate();
                        Toast.makeText(MapsActivity.this, "Location recaptured!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                    }
                }, null);
            } else {
                Toast.makeText(this, "Location permission required!", Toast.LENGTH_SHORT).show();
            }
        });

        SearchView searchView = binding.searchLocation;


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
//                searchLocation(query);
                fetchLocationSuggestions(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(!newText.isEmpty()){
                    fetchLocationSuggestions(newText);
                }
                return false;
            }
        });


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        mMap.setOnTouchListener((v, event) -> {
            isUserInteracting = true;
            return false;
        });


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
//            @Override
//            public void onLocationChanged(@NonNull Location location) {
//                if (location.getLatitude() != 0 && location.getLongitude() != 0) {
//                    myCurrentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
//                    Log.d("MapsActivity", "Current Location Updated: " + myCurrentLocation.getLatitude() + ", " + myCurrentLocation.getLongitude());
//
//                    if (!isUserInteracting && controller != null) {
//                        controller.setCenter(myCurrentLocation);
//                        controller.setZoom(20.0);
//                    }
//
//                    mMap.invalidate();
//                }
//            }

            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (location.getLatitude() != 0 && location.getLongitude() != 0) {
                    currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                    Log.d("MapsActivity", "Current Location Updated: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());

                    if (!isUserInteracting && controller != null) {
                        controller.setCenter(currentLocation);
                        controller.setZoom(20.0);
                    }


                    mMap.invalidate();
                }
            }


            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(@NonNull String provider) {}

            @Override
            public void onProviderDisabled(@NonNull String provider) {}
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("MapsActivity", "Location permission not granted!");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }


    private void fetchLocationSuggestions(String query) {
        ListView locationListView = findViewById(R.id.search_list);

        new Thread(() -> {
            try {
                String urlStr = "https://nominatim.openstreetmap.org/search?q=" +
                        URLEncoder.encode(query, "UTF-8") +
                        "&format=json&addressdetails=1&limit=5";
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JSONArray jsonArray = new JSONArray(response.toString());
                ArrayList<String> locations = new ArrayList<>();
                ArrayList<GeoPoint> geoPoints = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject place = jsonArray.getJSONObject(i);
                    String displayName = place.getString("display_name");
                    double lat = place.getDouble("lat");
                    double lon = place.getDouble("lon");

                    locations.add(displayName);
                    geoPoints.add(new GeoPoint(lat, lon));
                }

                runOnUiThread(() -> {
                    if (!locations.isEmpty()) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(MapsActivity.this, R.layout.list_item, locations);
                        locationListView.setAdapter(adapter);

                        locationListView.setVisibility(View.VISIBLE);

                        locationListView.setOnItemClickListener((parent, view, position, id) -> {
                            GeoPoint selectedPoint = geoPoints.get(position);
                            String selectedLocation = locations.get(position);

                            locationListView.setVisibility(View.GONE);

                            showLocationOnMap(selectedPoint, selectedLocation);
//                            drawRoute(currentLocation, selectedPoint);

                            if (currentLocation != null) {
                                // Fetch and display the route from current location to the selected destination
                                fetchRoute(currentLocation, selectedPoint);
                                drawRoute(currentLocation, selectedPoint);

                            } else {
                                Log.e("MapsActivity", "Current location is not available.");
                            }
                        });
                    } else {
                        // If no results, hide the ListView
                        locationListView.setVisibility(View.GONE);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void showLocationOnMap(GeoPoint point, String location) {
        controller.setCenter(point);
        controller.setZoom(18.0);

        Marker marker = new Marker(mMap);
        marker.setPosition(point);
        marker.setTitle(location);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mMap.getOverlays().add(marker);
        mMap.invalidate();
    }

    private void drawRoute(GeoPoint start, GeoPoint end) {
        new Thread(() -> {
            try {
                String urlStr = "https://router.project-osrm.org/route/v1/driving/" +
                        start.getLongitude() + "," + start.getLatitude() + ";" +
                        end.getLongitude() + "," + end.getLatitude() +
                        "?overview=full&geometries=geojson";
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JSONObject jsonObject = new JSONObject(response.toString());
                JSONArray routes = jsonObject.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject geometry = route.getJSONObject("geometry");
                    JSONArray coordinates = geometry.getJSONArray("coordinates");

                    List<GeoPoint> routePoints = new ArrayList<>();
                    for (int i = 0; i < coordinates.length(); i++) {
                        JSONArray coord = coordinates.getJSONArray(i);
                        double lon = coord.getDouble(0);
                        double lat = coord.getDouble(1);
                        routePoints.add(new GeoPoint(lat, lon));
                    }
                    runOnUiThread(() -> {
                        Polyline lineOverlay = new Polyline();
                        lineOverlay.setPoints(routePoints);
                        lineOverlay.setColor(Color.BLUE);
                        lineOverlay.setWidth(8);
                        mMap.getOverlays().add(lineOverlay);
                        mMap.invalidate();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation(); // Re-attempt location fetch if permission is granted
            } else {
                Log.e("MapsActivity", "Location permission denied.");
            }
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                myCurrentLocation = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                Log.d("MapsActivity", "Using Last Known Location: " + myCurrentLocation.getLatitude() + ", " + myCurrentLocation.getLongitude());
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0, locationListener);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private void fetchRoute(GeoPoint start, GeoPoint end) {
        String url = "https://router.project-osrm.org/route/v1/driving/"
                + start.getLongitude() + "," + start.getLatitude() + ";"
                + end.getLongitude() + "," + end.getLatitude()
                + "?overview=full&geometries=polyline"; // Change to "polyline" (not polyline6)

        Log.d("MapsActivity", "Fetching route from: " + start.getLatitude() + "," + start.getLongitude()
                + " to " + end.getLatitude() + "," + end.getLongitude());

        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                Log.d("MapsActivity", "API Response: " + response.toString());

                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.has("routes") && jsonResponse.getJSONArray("routes").length() > 0) {
                    JSONObject route = jsonResponse.getJSONArray("routes").getJSONObject(0);
                    if (route.has("geometry")) {
                        String encodedPolyline = route.getString("geometry");
                        List<GeoPoint> pathPoints = decodePolyline(encodedPolyline);

                        runOnUiThread(() -> {
                            if (!pathPoints.isEmpty()) {
                                drawRouteOnMap(pathPoints);
                            } else {
                                Log.e("MapsActivity", "No path points available to draw.");
                            }
                        });
                    } else {
                        Log.e("OSRM", "Geometry not found in the response.");
                    }
                } else {
                    Log.e("OSRM", "No route found in the response.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("OSRM", "Error fetching route: " + e.getMessage());
            }
        }).start();
    }

    private List<GeoPoint> decodePolyline(String encoded) {
        List<GeoPoint> polyline = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1F) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1F) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            GeoPoint point = new GeoPoint(lat / 1E5, lng / 1E5);
            polyline.add(point);
        }
        return polyline;
    }


    private void drawRouteOnMap(List<GeoPoint> pathPoints) {
        if (pathPoints != null && !pathPoints.isEmpty()) {
            Polyline polyline = new Polyline();
            for (GeoPoint point : pathPoints) {
                polyline.addPoint(point);
                Log.d("MapsActivity", "Adding point: " + point.getLatitude() + ", " + point.getLongitude());
            }
            polyline.setColor(0xFF0000FF); // Blue color
            polyline.setWidth(5f);
            mMap.getOverlays().add(polyline);
            mMap.invalidate();
        } else {
            Log.e("MapsActivity", "No valid path points to draw.");
        }
    }
}