package com.hoanguyenhs.invercargillbuses;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.hoanguyenhs.utility.GeocodingProcessor;
import com.hoanguyenhs.utility.PlacesAutocompleteAdapter;

import java.util.List;

public class DirectionActivity extends Activity implements
        OnMapReadyCallback, GoogleMap.OnMapClickListener {
    private static final LatLng INVERCARGILL = new LatLng(-46.4131, 168.3475);
    private static final double MIN_X = -46.468226;
    private static final double MAX_X = -46.352353;
    private static final double MAX_Y = 168.423727;
    private static final double MIN_Y = 168.313006;
    private GoogleMap googleMap;
    private String isManual = "yes";
    private Marker startingMarker = null;
    private Marker destinationMarker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direction);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        MapFragment mMapFragment = MapFragment.newInstance();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.my_container, mMapFragment);
        fragmentTransaction.commit();
        mMapFragment.getMapAsync(DirectionActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.setOnMapClickListener(this);
        map.setMyLocationEnabled(true);
        this.googleMap = map;
        // Close keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isAcceptingText()) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }

        final BusesDetail busesDetail = new BusesDetail(getAssets());
        final BusStopsDetail busStopsDetail = new BusStopsDetail(getAssets());

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(INVERCARGILL, 6));
        map.animateCamera(CameraUpdateFactory.zoomIn());
        map.animateCamera(CameraUpdateFactory.zoomTo(12), 2000, null);

        Intent intent = getIntent();
        isManual = (String) intent.getSerializableExtra("isManual");
        if (isManual.equals("no")) {
            String startingText = (String) intent.getSerializableExtra("Starting");
            String destinationText = (String) intent.getSerializableExtra("Destination");
            displayDirection(startingText, destinationText, map);
        }

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                return true;
            }
        });
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                Context context = getApplicationContext();
                LinearLayout info = new LinearLayout(context);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(context);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());
                info.addView(title);

                if (marker.getSnippet() != null) {
                    String[] snippets = marker.getSnippet().split(";");
                    TextView textView0 = new TextView(context);
                    textView0.setTextColor(Color.BLACK);
                    textView0.setText("Stopcode: " + snippets[1]);
                    info.addView(textView0);
                    TextView textView1 = new TextView(context);
                    textView1.setTextColor(Color.BLACK);
                    textView1.setText("The next scheduled services are:");
                    info.addView(textView1);
                    String[] buses = snippets[0].split(",");
                    String[] times = snippets[2].split(",");
                    for (Integer i = 0; i < buses.length; i++) {
                        TextView textView = new TextView(context);
                        textView.setTextColor(Color.GRAY);
                        textView.setText(busesDetail.getBusRouteName(buses[i]) + " - " + times[i]);
                        info.addView(textView);
                    }
                    TextView textView2 = new TextView(context);
                    textView2.setTextColor(Color.BLACK);
                    textView2.setText("While at bus stop...");
                    info.addView(textView2);
                    TextView textView3 = new TextView(context);
                    textView3.setTextColor(Color.GRAY);
                    textView3.setText("Send INV " + snippets[1] + " as a text to 400");
                    info.addView(textView3);
                }
                return info;
            }
        });
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (isManual.equals("yes")) {
            if (startingMarker == null && destinationMarker == null) {
                startingMarker = googleMap.addMarker(new MarkerOptions().position(latLng));
                startingMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.starting32));
                startingMarker.setTitle(latLng.latitude + ", " + latLng.longitude);
            } else if (startingMarker != null && destinationMarker == null) {
                destinationMarker = googleMap.addMarker(new MarkerOptions().position(latLng));
                displayDirectionByMarker(startingMarker, destinationMarker, googleMap);
                if (destinationMarker != null) {
                    destinationMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.destination32));
                    destinationMarker.setTitle(latLng.latitude + ", " + latLng.longitude);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_direction, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if (id == R.id.action_direction_activity) {
            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getText(R.string.action_direction_activity));
            builder.setIcon(R.drawable.pointobject32);
            LayoutInflater inflater = this.getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_direction, null);
            // Setting for start point
            final AutoCompleteTextView startingPoint = (AutoCompleteTextView) view.findViewById(R.id.starting_point);
            startingPoint.setAdapter(new PlacesAutocompleteAdapter(this, R.layout.place_list));
            // Setting for destination point
            final AutoCompleteTextView destinationPoint = (AutoCompleteTextView) view.findViewById(R.id.destination_point);
            destinationPoint.setAdapter(new PlacesAutocompleteAdapter(this, R.layout.place_list));
            // Setting for button my location
            view.findViewById(R.id.starting_point_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startingPoint.setText("My location");
                }
            });
            view.findViewById(R.id.destination_point_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    destinationPoint.setText("My location");
                }
            });
            // Display the dialog
            builder.setView(view);
            builder.setPositiveButton("Search", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    isManual = "no";
                    googleMap.clear();
                    startingMarker = null;
                    destinationMarker = null;
                    // Close keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm.isAcceptingText()) {
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }
                    // Call method to search direction
                    String startingText = startingPoint.getText().toString();
                    String destinationText = destinationPoint.getText().toString();
                    displayDirection(startingText, destinationText, googleMap);
                }
            });
            builder.setNegativeButton("Search Manually", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    isManual = "yes";
                    googleMap.clear();
                    startingMarker = null;
                    destinationMarker = null;
                    // Close keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm.isAcceptingText()) {
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }
                }
            });
            dialog = builder.create();
            dialog.show();
            return true;
        } else if (id == R.id.action_clear) {
            isManual = "yes";
            googleMap.clear();
            startingMarker = null;
            destinationMarker = null;
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayDirectionByMarker(Marker marker1, Marker marker2, GoogleMap map) {
        final BusesDetail busesDetail = new BusesDetail(getAssets());
        final BusStopsDetail busStopsDetail = new BusStopsDetail(getAssets());

        double sX = marker1.getPosition().latitude;
        double sY = marker1.getPosition().longitude;
        double dX = marker2.getPosition().latitude;
        double dY = marker2.getPosition().longitude;
        boolean isOk = true;
        if ((sX < MIN_X || sX > MAX_X) || (sY < MIN_Y || sY > MAX_Y) ||
                (dX < MIN_X || dX > MAX_X) || (dY < MIN_Y || dY > MAX_Y)) {
            isOk = false;
        }
        if (isOk) {
            try {
                // Display nearest bus stops for starting point and destination point
                Marker nearStartingPoint = map.addMarker(busStopsDetail.nearestBusStop(marker1.getPosition()));
                Marker nearDestinationPoint = map.addMarker(busStopsDetail.nearestBusStop(marker2.getPosition()));

                Object[] directions = busesDetail.getDirection(nearStartingPoint, nearDestinationPoint);
                for (PolylineOptions direction : (List<PolylineOptions>) directions[0]) {
                    map.addPolyline(direction);
                }
                for (MarkerOptions transferStop : (List<MarkerOptions>) directions[1]) {
                    map.addMarker(transferStop);
                }
            } catch (Exception e) {
                isManual = "yes";
                googleMap.clear();
                startingMarker = null;
                destinationMarker = null;
                AlertDialog alertDialog = new AlertDialog.Builder(DirectionActivity.this).create();
                alertDialog.setTitle("Alert");
                alertDialog.setMessage("Can not find any posible direction." +
                        " Please select manually by click on the map or try to find it again");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        } else {
            isManual = "yes";
            googleMap.clear();
            startingMarker = null;
            destinationMarker = null;
            AlertDialog alertDialog = new AlertDialog.Builder(DirectionActivity.this).create();
            alertDialog.setTitle("Alert");
            alertDialog.setMessage("Can not find 'Starting Point' and 'Destination Point'." +
                    " Please select manually by click on the map or try to find it again");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
    }

    private void displayDirection(String startingText, String destinationText, GoogleMap map) {
        final BusesDetail busesDetail = new BusesDetail(getAssets());
        final BusStopsDetail busStopsDetail = new BusStopsDetail(getAssets());

        String[] starting = null;
        String[] destination = null;
        try {
            if (startingText.equals("My location")) {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Location myLcation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                starting = new String[2];
                starting[0] = myLcation.getLatitude() + "";
                starting[1] = myLcation.getLongitude() + "";
            } else {
                starting = GeocodingProcessor.getLatLngFromAddress(startingText).split(",");
            }

            if (destinationText.equals("My location")) {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Location myLcation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                destination = new String[2];
                destination[0] = myLcation.getLatitude() + "";
                destination[1] = myLcation.getLongitude() + "";
            } else {
                destination = GeocodingProcessor.getLatLngFromAddress(destinationText).split(",");
            }

            try {
                double sX = Double.parseDouble(starting[0]);
                double sY = Double.parseDouble(starting[1]);
                double dX = Double.parseDouble(destination[0]);
                double dY = Double.parseDouble(destination[1]);
                boolean isOk = true;
                if ((sX < MIN_X || sX > MAX_X) || (sY < MIN_Y || sY > MAX_Y) ||
                        (dX < MIN_X || dX > MAX_X) || (dY < MIN_Y || dY > MAX_Y)) {
                    isOk = false;
                }
                if (isOk) {
                    // Display staring point
                    LatLng startingLatLng = new LatLng(sX, sY);
                    Marker startingPoint = map.addMarker(new MarkerOptions().position(startingLatLng));
                    startingPoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.starting32));
                    startingPoint.setTitle(startingText);
                    // Display destination point
                    LatLng destinationLatLng = new LatLng(dX, dY);
                    Marker destinationPoint = map.addMarker(new MarkerOptions().position(destinationLatLng));
                    destinationPoint.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.destination32));
                    destinationPoint.setTitle(destinationText);
                    // Display nearest bus stops for starting point and destination point
                    Marker nearStartingPoint = map.addMarker(busStopsDetail.nearestBusStop(startingLatLng));
                    Marker nearDestinationPoint = map.addMarker(busStopsDetail.nearestBusStop(destinationLatLng));

                    Object[] directions = busesDetail.getDirection(nearStartingPoint, nearDestinationPoint);
                    for (PolylineOptions direction : (List<PolylineOptions>) directions[0]) {
                        map.addPolyline(direction);
                    }
                    for (MarkerOptions transferStop : (List<MarkerOptions>) directions[1]) {
                        map.addMarker(transferStop);
                    }
                } else {
                    isManual = "yes";
                    googleMap.clear();
                    startingMarker = null;
                    destinationMarker = null;
                    AlertDialog alertDialog = new AlertDialog.Builder(DirectionActivity.this).create();
                    alertDialog.setTitle("Alert");
                    alertDialog.setMessage("Can not find 'Starting Point' and 'Destination Point'." +
                            " Please select manually by click on the map or try to find it again");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                isManual = "yes";
                googleMap.clear();
                startingMarker = null;
                destinationMarker = null;
                AlertDialog alertDialog = new AlertDialog.Builder(DirectionActivity.this).create();
                alertDialog.setTitle("Alert");
                alertDialog.setMessage("Can not find any posible direction." +
                        " Please select manually by click on the map or try to find it again.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            isManual = "yes";
            googleMap.clear();
            startingMarker = null;
            destinationMarker = null;
            AlertDialog alertDialog = new AlertDialog.Builder(DirectionActivity.this).create();
            alertDialog.setTitle("Alert");
            alertDialog.setMessage("Please enable your GPS service.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
    }
}
