package cs.umass.edu.myactivitiestoolkit.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.security.Provider;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.location.GPSLocation;
import cs.umass.edu.myactivitiestoolkit.location.LocationDAO;
import edu.umass.cs.MHLClient.sensors.GPSReading;



/**
 * The location service collects GPS data, stores the readings in a local database
 * and sends them to the server.
 */
public class LocationService extends SensorService implements LocationListener {

    /** Used during debugging to identify logs by class */
    private static final String TAG = LocationService.class.getName();

    /**
     * The minimum duration in milliseconds between sensor readings.
     */
    private static final int MIN_TIME = 5000;

    /**
     * Defines the minimum distance in meters between sequential sensor readings.
     */
    private static final float MIN_DISTANCE = 0f;

    /**
     * Manages the GPS sensor.
     */
    private LocationManager locationManager;

    @Override
    protected void onServiceStarted() {
        broadcastMessage(Constants.MESSAGE.LOCATION_SERVICE_STARTED);
    }

    @Override
    protected void onServiceStopped() {
        broadcastMessage(Constants.MESSAGE.LOCATION_SERVICE_STOPPED);
    }

    @Override
    protected void registerSensors() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //make sure we have permission to access location before requesting the sensor.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.d(TAG, "Starting location manager");
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME,
                MIN_DISTANCE,
                this,
                getMainLooper());

    }

    @Override
    protected void unregisterSensors() {
        //make sure we have permission to access location before requesting the sensor.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(this);
    }

    @Override
    protected int getNotificationID() {
        return Constants.NOTIFICATION_ID.LOCATION_SERVICE;
    }

    @Override
    protected String getNotificationContentText() {
        return getString(R.string.location_service_notification);
    }

    @Override
    protected int getNotificationIconResourceID() {
        return R.drawable.ic_location_on_white_48dp;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, location.toString());
        LocationDAO dao = new LocationDAO(getApplicationContext());
        dao.openWrite();
        dao.insert(new GPSLocation(location.getTime(),location.getLatitude(),location.getLongitude(), location.getAccuracy()));
        dao.close();
        mClient.sendSensorReading(new GPSReading(mUserID, "MOBILE", "", location.getTime(), location.getLatitude(), location.getLongitude()));

        //to determine if you're inside a building by calling helper method
        Log.d(TAG, "Location Time: " + location.getTime()+ ", Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());

    }


    protected boolean isBetterLocation(Location location, Location currentBestLocation){
        if(currentBestLocation == null){
            //a new location is always better than no location
            return true;
        }
        //check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        long TWO_MINUTES_IN_MILLI = 120000;
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES_IN_MILLI;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES_IN_MILLI;
        boolean isNewer = timeDelta > 0;

        //if it's been more than two minutes since the current location, use the new location because the user has most likely moved
        if(isSignificantlyNewer){
            return true;
            //if the new location has more than two minutes older, it must be worse
        }else if(isSignificantlyOlder){
            return false;
        }
        //check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        //check if the old and new location are from same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        //determine location quality using a combination of timelessness and accuracy
        if(isMoreAccurate){
            return true;
        }else if(isNewer && !isLessAccurate){ //if timedelta is more than 0
            return true;
        }else if(isNewer && !isSignificantlyLessAccurate && isFromSameProvider){
            return true;
        }
        return false;
    }

    protected boolean isSameProvider(String locationProvider, String currentBestLocationProvider){
        if(locationProvider== currentBestLocationProvider){
            return true;
        }
        return false;
    }
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
