package com.example.myapp;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
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
import org.json.JSONException;
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

public class MapsActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int PERMISSION_REQUEST_CODE = 1;
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MapsActivity", "Location permission not granted! By user");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }

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
                        Log.d("MapsActivity", "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            }else {

                logLocation();
            }
        }else{
            logLocation();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }else {

            logLocation();
        }
        }else{
            logLocation();
        }

        mMap.setOnTouchListener((v, event) -> {
            isUserInteracting = true;
            return false;
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
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


    }

    private void logLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                Log.d("MapsActivity", "Latitude: " + lastKnownLocation.getLatitude() + ", Longitude: " + lastKnownLocation.getLongitude());
            } else {
                Log.e("MapsActivity", "Location not available");
            }
        }

    }


    private void fetchLocationSuggestions(String query) {
        ListView locationListView = findViewById(R.id.search_list);

        new Thread(() -> {
            try {
                String urlStr = "https://nominatim.openstreetmap.org/search.php?q=" +
                        URLEncoder.encode(query, "UTF-8") +
                        "&format=json&addressdetails=1&limit=5";



                Log.d("MapsActivity", "Fetching from URL: " + urlStr);

                JSONArray jsonArray = getJsonArray(urlStr);
                ArrayList<String> locations = new ArrayList<>();
                ArrayList<GeoPoint> geoPoints = new ArrayList<>();

                if (jsonArray.length() == 0) {
                    Log.e("MapsActivity", "No results found for query: " + query);
                    runOnUiThread(() -> Toast.makeText(MapsActivity.this, "No locations found", Toast.LENGTH_SHORT).show());
                    return; // Early exit if no results
                }

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject place = jsonArray.getJSONObject(i);
                    String displayName = place.optString("display_name", "Unknown Location");
                    double lat = place.optDouble("lat", 0.0);
                    double lon = place.optDouble("lon", 0.0);

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

                            // Check if current location is available
                            if (currentLocation == null) {
                                Log.e("MapsActivity", "Current location is null. Cannot draw route.");
                                return;
                            }

                            drawRoute(currentLocation, selectedPoint);

                        });
                    } else {
                        locationListView.setVisibility(View.GONE);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("MapsActivity", "Error fetching location suggestions", e);
            }
        }).start();
    }


    private static @NonNull JSONArray getJsonArray(String urlStr) throws IOException, JSONException {
        Log.d("MapsActivity", "Fetching from URL: " + urlStr);

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

        if (response.toString().isEmpty()) {
            Log.e("MapsActivity", "Empty response from API.");
            return new JSONArray(); // or handle the error accordingly
        }

        return new JSONArray(response.toString());
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
        if (currentLocation == null || end == null) {
            Log.e("MapsActivity", "Cannot draw route with null locations.");
            return;
        }
        new Thread(() -> {
            try {
                JSONArray routes = getJsonArray(start, end);
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
                        mMap.getOverlays().clear();

                        Polyline lineOverlay = new Polyline();
                        lineOverlay.setPoints(routePoints);
                        lineOverlay.setColor(Color.BLUE);
                        lineOverlay.setWidth(8);
                        mMap.getOverlays().add(lineOverlay);
                        mMap.invalidate();
                    });
                }else{
                    Toast.makeText(this, "No routes found.", Toast.LENGTH_SHORT).show();
                    Log.e("MapsActivity", "No routes found.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static @NonNull JSONArray getJsonArray(GeoPoint start, GeoPoint end) throws IOException, JSONException {
        String urlStr = "https://router.project-osrm.org/route/v1/driving/" +
                start.getLongitude() + "," + start.getLatitude() + ";" +
                end.getLongitude() + "," + end.getLatitude() +
                "?overview=full&geometries=geojson";
        URL url = new URL(urlStr);
        JSONObject jsonObject = getObject(url);

        return jsonObject.getJSONArray("routes");
    }

    private static @NonNull JSONObject getObject(URL url) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String line;
        Log.d("MapsActivity", "OSRM API Response: " + response);

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }


        return new JSONObject(response.toString());
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with location fetching
                getCurrentLocation();
            } else {
                Log.e("MapsActivity", "Location permission denied.");
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void getCurrentLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    myCurrentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                    controller.setCenter(myCurrentLocation);
                    controller.setZoom(18.0);
                    mMap.invalidate();
                    Toast.makeText(MapsActivity.this, "Location updated!", Toast.LENGTH_SHORT).show();
                    Log.d("MapsActivity", "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(@NonNull String provider) {}

                @Override
                public void onProviderDisabled(@NonNull String provider) {}
            }, null);
        } else {
            Toast.makeText(this, "Location permission required!", Toast.LENGTH_SHORT).show();
        }
    }




    @Override
    protected void onResume() {
        super.onResume();
        // Register location listener when the activity is in the foreground
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister location listener to stop updates when the activity is paused
        locationManager.removeUpdates(locationListener);
    }

}