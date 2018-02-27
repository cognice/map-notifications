package us.cognice.secrets;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.*;
import com.google.gson.Gson;
import us.cognice.secrets.data.Location;
import us.cognice.secrets.fragments.MapScreen;
import us.cognice.secrets.fragments.MessageDialog;
import us.cognice.secrets.fragments.RadiusPicker;
import us.cognice.secrets.utils.UnderscoreRemover;
import us.cognice.secrets.utils.Utils;

import java.util.UUID;

public class LocationActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerDragListener, RadiusPicker.RadiusPickerListener, MessageDialog.LocationMessageListener {

    public static final int REQUEST_CODE = 201;
    public static final String RESULT_SAVED = "locationSaved";
    public static final String NEW_LOCATION = "locationIsNew";
    public static final String LOCATION_OBJECT = "locationObject";
    public static final String LOCATION_ACTIVE = "locationActive";
    private final MapScreen mapFragment = new MapScreen();
    private final Gson gson = new Gson();
    private GoogleMap map;
    private FloatingActionButton save, cancel;
    private EditText locationName;
    private TextView coordinates, locationMessage, radius;
    private ImageView radiusEdit, messageEdit;
    private Switch showOnMap;
    private Marker marker;
    private Circle circle;
    private boolean create;
    private Location location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        save = findViewById(R.id.saveLocation);
        cancel = findViewById(R.id.cancelLocation);
        locationName = findViewById(R.id.locationName);
        locationMessage = findViewById(R.id.locationMessage);
        showOnMap = findViewById(R.id.showOnMap);
        coordinates = findViewById(R.id.coordinates);
        radius = findViewById(R.id.radius);
        radiusEdit = findViewById(R.id.radiusEditButton);
        messageEdit = findViewById(R.id.messageEditButton);
        create = getIntent().getExtras().getBoolean(NEW_LOCATION);
        if (create) {
            coordinates.setText(getString(R.string.location_hint_create));
            location = new Location(UUID.randomUUID().toString());
        } else {
            location = gson.fromJson(getIntent().getExtras().getString(LOCATION_OBJECT), Location.class);
            locationName.setText(location.getName());
            locationMessage.setText(location.getMessage());
            radius.setText(Utils.formatMeters(location.getRadius()));
            coordinates.setText("Latitude: " + Utils.formatCoordinate(location.getLatitude()) + ". Longitude: " + Utils.formatCoordinate(location.getLongitude()));
            showOnMap.setChecked(location.isActive());
        }
        showOnMap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                location.setActive(isChecked);
            }
        });
        if (findViewById(R.id.locationMap) != null) {
            mapFragment.getMapAsync(this);
            //TODO need it?
            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            mapFragment.setArguments(getIntent().getExtras());
            // If we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            //TODO back button
            if (savedInstanceState == null) {
                // Add the fragment to the 'fragment_container' FrameLayout
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.locationMap, mapFragment).commit();
            }
        }
        initEditText(locationName);
        save.setClickable(true);
        cancel.setClickable(true);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeLocationActivity(true);
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeLocationActivity(false);
            }
        });
        radiusEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadiusPicker.newInstance(location.getRadius()).show(getSupportFragmentManager(), "RadiusPicker");
            }
        });
        messageEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MessageDialog.newInstance(location.getMessage()).show(getSupportFragmentManager(), "MessageDialog");
            }
        });
    }

    private void closeLocationActivity(boolean saved) {
        Intent result = new Intent();
        result.putExtra(RESULT_SAVED, saved);
        result.putExtra(NEW_LOCATION, create);
        result.putExtra(LOCATION_OBJECT, gson.toJson(location));
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    private void initEditText(final EditText edit) {
        edit.getBackground().setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_IN);
        edit.setOnFocusChangeListener(new UnderscoreRemover(save, cancel));
        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    edit.clearFocus();
                    Utils.hideKeyboard(edit);
                    location.setName(edit.getText().toString());
                    new Handler().postDelayed(
                            new Runnable() {
                                public void run() {
                                    save.show();
                                    cancel.show();
                                }
                            }, 300);
                }
                return false;
            }
        });
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

    private void initMap() {
        map.setOnMarkerClickListener(this);
        map.setOnMapLongClickListener(this);
        map.setOnMarkerDragListener(this);
        if (!create) {
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            createMarkerWithCircle(position);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 10));
        }
    }

    private void createMarkerWithCircle(LatLng position) {
        marker = map.addMarker(new MarkerOptions()
                .position(position)
                .draggable(true)
                .anchor(0.5f, 0.5f)
                .alpha(1)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_location_circle)));
        circle = map.addCircle(new CircleOptions()
                .center(position)
                .radius(location.getRadius())
                .strokeWidth(3)
                .strokeColor(Color.argb(255, 76, 175, 80))
                .fillColor(Color.argb(64, 76, 175, 100)));
    }

    @Override
    public void onMapLongClick(LatLng position) {
        if (marker == null) {
            createMarkerWithCircle(position);
        } else {
            marker.setPosition(position);
            circle.setCenter(marker.getPosition());
        }
        updateCoordinates(position);
    }

    @Override
    public void radiusChanged(int value) {
        radius.setText(Utils.formatMeters(value));
        location.setRadius(value);
        if (circle != null) circle.setRadius(value);
    }

    @Override
    public void messageChanged(String message) {
        locationMessage.setText(message);
        location.setMessage(message);
    }

    private void updateCoordinates(LatLng position) {
        coordinates.setText("Latitude: " + Utils.formatCoordinate(position.latitude) + ". Longitude: " + Utils.formatCoordinate(position.longitude));
        location.setLatitude(position.latitude);
        location.setLongitude(position.longitude);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Toast.makeText(this, "YOU CLICKED ON " + marker.getTitle(), Toast.LENGTH_LONG).show();
        return true;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        updateCoordinates(marker.getPosition());
        circle.setCenter(marker.getPosition());
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        updateCoordinates(marker.getPosition());
        circle.setCenter(marker.getPosition());

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        updateCoordinates(marker.getPosition());
        circle.setCenter(marker.getPosition());
    }
}
