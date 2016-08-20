package com.hoanguyenhs.invercargillbuses;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.google.android.gms.maps.model.Polyline;
import com.hoanguyenhs.utility.LatLngInterpolator;
import com.hoanguyenhs.utility.MarkerAnimation;
import com.hoanguyenhs.utility.PlacesAutocompleteAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity implements
        OnMapReadyCallback {

    private static final LatLng INVERCARGILL = new LatLng(-46.4131, 168.3475);
    private Polyline[] busRoutes;
    private Marker[] buses;
    private boolean[] busRoutesIsCheckd;
    private HashMap<Integer, List<Marker>> busStopsHashMap;
    private boolean[] busStopsIsCheckd;

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        MapFragment mMapFragment = MapFragment.newInstance();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.my_container, mMapFragment);
        fragmentTransaction.commit();
        mMapFragment.getMapAsync(MainActivity.this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        final BusesDetail busDetail = new BusesDetail(getAssets());
        final Integer numOfBuses = busDetail.countBuses();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_update_buses) {
            for (Integer i = 0; i < numOfBuses; i++) {
                LatLng busPos = busDetail.simulateBusLocation(i.toString());
                if (busPos.latitude == 0) {
                    buses[i].setPosition(busPos);
                    buses[i].setVisible(false);
                } else {
                    LatLngInterpolator latlonInter = new LatLngInterpolator.LinearFixed();
                    latlonInter.interpolate(20, buses[i].getPosition(), busPos);

                    MarkerAnimation.animateMarkerToGB(buses[i], busPos, latlonInter);
                    if (busRoutes[i].isVisible()) {
                        buses[i].setVisible(true);
                    } else {
                        buses[i].setVisible(false);
                    }
                }
            }
            return true;
        }
        if (id == R.id.action_show_hide_bus_routes) {
            final CharSequence[] busNames = new CharSequence[numOfBuses];
            for (Integer i = 0; i < numOfBuses; i++) {
                busNames[i] = busDetail.getBusRouteName(i.toString());
            }
            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getText(R.string.action_show_hide_bus_routes));
            builder.setMultiChoiceItems(busNames, busRoutesIsCheckd,
                    new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                            if (isChecked) {
                                busRoutesIsCheckd[indexSelected] = true;
                            } else {
                                busRoutesIsCheckd[indexSelected] = false;
                            }
                        }
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            for (Integer i = 0; i < numOfBuses; i++) {
                                if (busRoutesIsCheckd[i]) {
                                    busRoutes[i].setVisible(true);
                                    buses[i].setVisible(true);
                                } else {
                                    busRoutes[i].setVisible(false);
                                    buses[i].setVisible(false);
                                }
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            dialog = builder.create();
            dialog.show();
            return true;
        }
        if (id == R.id.action_show_hide_bus_stops) {
            final CharSequence[] busNames = new CharSequence[numOfBuses];
            for (Integer i = 0; i < numOfBuses; i++) {
                busNames[i] = busDetail.getBusRouteName(i.toString());
            }
            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getText(R.string.action_show_hide_bus_stops));
            builder.setMultiChoiceItems(busNames, busStopsIsCheckd,
                    new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                            if (isChecked) {
                                busStopsIsCheckd[indexSelected] = true;
                            } else {
                                busStopsIsCheckd[indexSelected] = false;
                            }
                        }
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            for (Integer i = 0; i < numOfBuses; i++) {
                                if (busStopsIsCheckd[i]) {
                                    for (Marker marker : (List<Marker>) busStopsHashMap.get(i)) {
                                        marker.setVisible(true);
                                    }
                                } else {
                                    for (Marker marker : (List<Marker>) busStopsHashMap.get(i)) {
                                        marker.setVisible(false);
                                    }
                                }
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            dialog = builder.create();
            dialog.show();
            return true;
        }
        if (id == R.id.action_direction_activity) {
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
            builder.setView(view);
            builder.setPositiveButton("Search", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // Set variable to next activity
                    Intent intent = new Intent(MainActivity.this, DirectionActivity.class);
                    intent.putExtra("Starting", startingPoint.getText().toString());
                    intent.putExtra("Destination", destinationPoint.getText().toString());
                    intent.putExtra("isManual", "no");
                    startActivity(intent);
                }
            });
            builder.setNegativeButton("Search Manually", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // Set variable to next activity
                    Intent intent = new Intent(MainActivity.this, DirectionActivity.class);
                    intent.putExtra("isManual", "yes");
                    startActivity(intent);
                }
            });
            dialog = builder.create();
            dialog.show();
            return true;
        }
        if (id == R.id.action_about_me) {
            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getText(R.string.title_about_me));
            builder.setIcon(R.drawable.aboutus32);
            LayoutInflater inflater = this.getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_aboutme, null);
            builder.setView(view);
            dialog = builder.create();
            dialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(final GoogleMap map) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(INVERCARGILL, 6));
        map.animateCamera(CameraUpdateFactory.zoomIn());
        map.animateCamera(CameraUpdateFactory.zoomTo(12), 2000, null);

        final BusesDetail busesDetail = new BusesDetail(getAssets());
        final BusStopsDetail busStopsDetail = new BusStopsDetail(getAssets());
        Integer numOfBuses = busesDetail.countBuses();
        busRoutes = new Polyline[numOfBuses];
        busRoutesIsCheckd = new boolean[numOfBuses];
        buses = new Marker[numOfBuses];
        busStopsHashMap = new HashMap<Integer, List<Marker>>();
        busStopsIsCheckd = new boolean[numOfBuses];
        for (Integer i = 0; i < numOfBuses; i++) {
            // Create bus routes
            busRoutes[i] = map.addPolyline(busesDetail.getBusRoute(i.toString()));
            busRoutesIsCheckd[i] = true;
            // Create bus Stops
            List<Marker> busStops = new ArrayList<Marker>();
            for (MarkerOptions busStopDetail : busStopsDetail.getBusStopsDetail(i.toString())) {
                Marker marker = map.addMarker(busStopDetail);
                marker.setVisible(false);
                busStops.add(marker);
            }
            busStopsHashMap.put(i, busStops);
            busStopsIsCheckd[i] = false;
            // Create buses
            buses[i] = map.addMarker(new MarkerOptions().position(busesDetail.simulateBusLocation(i.toString()))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.shuttle24))
                    .title(busesDetail.getBusRouteName(i.toString())));
            if (buses[i].getPosition().latitude == 0) {
                buses[i].setVisible(false);
            } else {
                buses[i].setVisible(true);
            }
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
}