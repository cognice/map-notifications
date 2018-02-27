package us.cognice.secrets;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import us.cognice.secrets.background.LocationService;
import us.cognice.secrets.data.Data;
import us.cognice.secrets.data.Location;
import us.cognice.secrets.fragments.ManageFragment;
import us.cognice.secrets.fragments.MapScreen;
import us.cognice.secrets.fragments.WelcomeDialog;
import us.cognice.secrets.utils.Utils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, WelcomeDialog.WelcomeDialogListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerDragListener {

    private GoogleMap map;
    private Gson gson = new Gson();
    private final ManageFragment manageFragment = new ManageFragment();
    private final MapScreen mapFragment = new MapScreen();
    private boolean settings = false;
    private Animation animation;
    private Data data;
    private MapLayout layout;
    private ViewGroup infoWindow;
    private TextView infoTitle;
    private TextView infoSnippet;
    private ImageButton iwEdit, iwDelete;
    private FloatingActionButton fab;
    private OnMapWindowInfoClickListener iwEditListener, iwDeleteListener;
    private GeofencingClient geofencingClient;
    private PendingIntent geofenceIntent;
    private GeofencingRequest geofencingRequest;

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        animation = AnimationUtils.loadAnimation(this, R.anim.fab_rotate);
        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragmentContainer) != null) {
            fab = findViewById(R.id.fab);
            toggleButton();
            fab.setOnClickListener(new View.OnClickListener() {
                public void onClick(View button) {
                    toggleFragments();
                }
            });
            mapFragment.getMapAsync(this);
            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            mapFragment.setArguments(getIntent().getExtras());
            // If we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState == null) {
                // Add the fragment to the 'fragment_container' FrameLayout
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragmentContainer, mapFragment).commit();
            }
            layout = findViewById(R.id.activity_main);
            infoWindow = (ViewGroup) getLayoutInflater().inflate(R.layout.map_info_window, null);
            infoTitle = infoWindow.findViewById(R.id.iwTitle);
            infoSnippet = infoWindow.findViewById(R.id.iwSnippet);
            iwEdit = infoWindow.findViewById(R.id.iwEdit);
            iwDelete = infoWindow.findViewById(R.id.iwDelete);
            iwEditListener = new OnMapWindowInfoClickListener(iwEdit) {
                @Override
                protected void onClickConfirmed(View v, Marker marker) {
                    final Location location = (Location) marker.getTag();
                    if (location != null) {
                        Intent i = new Intent(MainActivity.this, LocationActivity.class);
                        i.putExtra(LocationActivity.NEW_LOCATION, false);
                        i.putExtra(LocationActivity.LOCATION_OBJECT, gson.toJson(location));
                        startActivityForResult(i, LocationActivity.REQUEST_CODE);
                    }
                }
            };
            iwDeleteListener = new OnMapWindowInfoClickListener(iwDelete) {
                @Override
                protected void onClickConfirmed(View v, Marker marker) {
                    final Location location = (Location) marker.getTag();
                    if (location != null) {
                        AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
                        adb.setTitle("Confirm action");
                        adb.setMessage("Are you sure you want to delete location '" + location.getName() + "'?");
                        adb.setNegativeButton("Cancel", null);
                        adb.setPositiveButton("Yes", new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeGeofence(location.getId());
                                data.getPlaces().remove(location);
                                saveData();
                                drawMarkers();
                            }
                        });
                        adb.show();
                    }
                }
            };
            iwEdit.setOnTouchListener(iwEditListener);
            iwDelete.setOnTouchListener(iwDeleteListener);
            File json = new File(getFilesDir(), "data.json");
            try {
                if (json.createNewFile()) {
                    toggleFragments();
                } else {
                    BufferedReader br = new BufferedReader(new FileReader(json));
                    data = gson.fromJson(br, Data.class);
                    br.close();
                }
            } catch (IOException e) {
                Toast.makeText(this, "Failed to read app data: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
            if (data != null) {
                if (data.isLocationServiceOn() && !data.isLocationServiceRunning()) {
                    startLocationService();
                } else if (!data.isLocationServiceOn() && data.isLocationServiceRunning()) {
                    stopLocationService();
                }
            } else {
                data = new Data();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == LocationActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                boolean saved = intent.getBooleanExtra(LocationActivity.RESULT_SAVED, false);
                if (saved) {
                    Location upd = gson.fromJson(intent.getExtras().getString(LocationActivity.LOCATION_OBJECT), Location.class);
                    if (upd.isActive()) {
                        addGeofence(upd);
                    } else {
                        removeGeofence(upd.getId());
                    }
                    for (Location l: data.getPlaces()) {
                        if (l.getId().equals(upd.getId())) {
                            Utils.copyLocation(upd, l);
                            break;
                        }
                    }
                    saveData();
                    drawMarkers();
                }
            } else {
                Toast.makeText(this, "Error while editing location", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) instanceof MapScreen) {
            settings = false;
            drawMarkers();
        } else {
            settings = true;
        }
        toggleButton();
    }

    public void startLocationService() {
        List<Geofence> geofences = new ArrayList<>();
        for (Location location : data.getPlaces()) {
            geofences.add(createGeofence(location));
        }
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofences);
        geofencingRequest = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{ACCESS_FINE_LOCATION}, 1);
        } else {
            onLocationServiceStarted(performAddGeofenceRequest(geofencingRequest));
        }
    }

    private void onLocationServiceStarted(Task<Void> task) {
        if (task != null) {
            task.addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        data.setLocationServiceRunning(true);
                        saveData();
                    } else {
                        String msg = task.getException() != null ? task.getException().getLocalizedMessage() : "";
                        Toast.makeText(MainActivity.this, "Failed to start locations service. " + msg, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    public void stopLocationService() {
        if (geofencingClient == null) {
            geofencingClient = LocationServices.getGeofencingClient(this);
        }
        // This is the same pending intent that was used in startLocationService()
        geofencingClient.removeGeofences(getGeofencePendingIntent()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    data.setLocationServiceRunning(false);
                    saveData();
                } else {
                    String msg = task.getException() != null ? task.getException().getLocalizedMessage() : "";
                    Toast.makeText(MainActivity.this, "Failed to stop locations service. " + msg, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private Task<Void> performAddGeofenceRequest(GeofencingRequest request) {
        try {
            if (geofencingClient == null) {
                geofencingClient = LocationServices.getGeofencingClient(this);
            }
            return geofencingClient.addGeofences(request, getGeofencePendingIntent());
        } catch (SecurityException e) {
            Toast.makeText(MainActivity.this, "No permissions were granted to add geofences", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    public void addGeofence(Location location) {
        if (data.isLocationServiceOn()) {
            GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
            builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
            builder.addGeofence(createGeofence(location));
            geofencingRequest = builder.build();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{ACCESS_FINE_LOCATION}, 2);
            } else {
                performAddGeofenceRequest(geofencingRequest);
            }
        }
    }

    public void removeGeofence(String id) {
        if (geofencingClient == null) {
            geofencingClient = LocationServices.getGeofencingClient(this);
        }
        geofencingClient.removeGeofences(Collections.singletonList(id));
    }

    private Geofence createGeofence(Location location) {
        return new Geofence.Builder()
            .setRequestId(location.getId())
            .setCircularRegion(
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getRadius()
            )
            .setExpirationDuration(NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
            .build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofenceIntent != null) {
            return geofenceIntent;
        }
        Intent intent = new Intent(this, LocationService.class);
        intent.putExtra(LocationService.LOCATIONS_UPDATE, gson.toJson(data.getPlaces()));
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofenceIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geofenceIntent;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //starting location service
        if (requestCode == 1) {
            onLocationServiceStarted(performAddGeofenceRequest(geofencingRequest));
        //adding single geofence
        } else if (requestCode == 2) {
            performAddGeofenceRequest(geofencingRequest);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            toggleFragments();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public void setData(Data upd) {
        data = upd;
        saveData();
    }

    public Data getData() {
        return data;
    }

    private void saveData() {
        try (Writer writer = new BufferedWriter(new FileWriter(new File(getFilesDir(), "data.json")))) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to save changes: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void toggleFragments() {
        settings = !settings;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragmentContainer, settings ? manageFragment : mapFragment);
        transaction.addToBackStack(null);
        // Commit the transaction
        transaction.commit();
        if (!settings) drawMarkers();
        toggleButton();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!settings) drawMarkers();
    }

    private void toggleButton() {
        fab.setImageDrawable(ContextCompat.getDrawable(fab.getContext(), settings ? R.drawable.ic_done_white_24dp : R.drawable.ic_menu_white_24dp));
        fab.startAnimation(animation);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        initMap();
    }

    //TODO sometimes markers are not shown
    private void initMap() {
        layout.init(map, Utils.dipToPixels(this, 59f));
        map.setOnMarkerClickListener(this);
        map.setOnMapLongClickListener(this);
        map.setOnMarkerDragListener(this);
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }
            @Override
            public View getInfoContents(Marker marker) {
                // Setting up the infoWindow with current's marker info
                infoTitle.setText(marker.getTitle());
                infoSnippet.setText(marker.getSnippet());
                iwEditListener.setMarker(marker);
                iwDeleteListener.setMarker(marker);
                // We must call this to set the current marker and infoWindow references
                // to the MapWrapperLayout
                layout.setMarkerWithInfoWindow(marker, infoWindow);
                return infoWindow;
            }
        });
        final View rootView = findViewById(android.R.id.content);
        if (rootView != null && rootView.getViewTreeObserver().isAlive()) {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            drawMarkers();
                            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    });
        }

    }

    private void drawMarkers() {
        map.clear();
        if (data == null) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Location l: data.getPlaces()) {
            if (l.isActive()) {
                LatLng position = new LatLng(l.getLatitude(), l.getLongitude());
                Marker marker = map.addMarker(new MarkerOptions()
                        .position(position)
                        .draggable(false)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker))
                        .snippet("Radius: " + Utils.formatMeters(l.getRadius()) + ". \nMessage: " + l.getMessage())
                        .title(l.getName()));
                marker.setTag(l);
                builder.include(position);
            }
        }
        if (data.getPlaces().size() > 0) {
            if (data.getPlaces().size() > 1) {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
            } else {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(data.getPlaces().get(0).getLatitude(), data.getPlaces().get(0).getLongitude()), 7));
            }
        }
    }

    public ManageFragment getManageFragment() {
        return manageFragment;
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        marker.showInfoWindow();
        map.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
        return true;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        //TODO context menu
    }

    @Override
    public void afterWelcome() {
        manageFragment.afterWelcome();
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        //TODO
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        //TODO
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        //TODO
    }

}
