package us.cognice.secrets.background;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.gson.Gson;
import us.cognice.secrets.MainActivity;
import us.cognice.secrets.R;
import us.cognice.secrets.data.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Kirill Simonov on 22.10.2017.
 */
public class LocationService extends IntentService {

    public static final String REQUEST_FROM_ACTIVITY = "requestFromActivity";
    public static final String LOCATIONS_UPDATE = "locationsUpdate";
    public static final String STARTED_BY_NOTIFICATION = "startedByNotification";
    public static final String NOTIFICATION_CHANNEL_ID = "secretsNotifications";
    public static final String NOTIFICATION_CHANNEL_NAME = "Notifications for Secrets App";
    private static final String SERVICE_NAME = "SecretsBackgroundService";

    private final Gson gson = new Gson();
    private Map<String, Location> locations;
    private AtomicInteger notificationId = new AtomicInteger(1);

    public LocationService() {
        super(SERVICE_NAME);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getBooleanExtra(REQUEST_FROM_ACTIVITY, false)) {
            updateLocations(gson.fromJson(intent.getStringExtra(LOCATIONS_UPDATE), Location[].class));
        } else {
            //check if it is the first call
            if (locations == null) {
                createNotificationChannel();
                updateLocations(gson.fromJson(intent.getStringExtra(LOCATIONS_UPDATE), Location[].class));
            }
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            if (geofencingEvent.hasError()) {
                //TODO
                /*String errorMessage = GeofenceErrorMessages.getErrorString(this,
                        geofencingEvent.getErrorCode());
                Log.e(TAG, errorMessage);*/
                return;
            }
            // Get the transition type.
            int geofenceTransition = geofencingEvent.getGeofenceTransition();
            // Test that the reported transition was of interest.
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                // Get the geofences that were triggered. A single event can trigger
                // multiple geofences.
                for (Geofence geofence: geofencingEvent.getTriggeringGeofences()) {
                    showNotification(locations.get(geofence.getRequestId()));
                }
            } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                //TODO
            } else {
                //TODO
                // Log the error.
                //Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
            }
        }
    }

    private void updateLocations(Location[] update) {
        Map<String, Location> m = new HashMap<>();
        for (Location l: update) {
            m.put(l.getId(), l);
        }
        locations = m;
    }

    private void showNotification(Location location) {
        //TODO remove on click
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.app_icon)
                        .setContentTitle("'" + location.getName() + "' location notification")
                        .setContentText(location.getMessage());
        //TODO open special window with notification text
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra(STARTED_BY_NOTIFICATION, true);
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(notificationId.getAndIncrement(), mBuilder.build());
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            //TODO customize
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.GREEN);
            notificationChannel.enableVibration(false);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

}
