package cs.umass.edu.myactivitiestoolkit.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import cs.umass.edu.myactivitiestoolkit.constants.Constants;

/**
 * Created by Yu on 12/13/16.
 */

public class LightService extends SensorService implements LocationListener {
    private LocationManager locationManager;
    private static final int MIN_TIME = 5000;
    private static final float MIN_DISTANCE = 0f;


    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    protected void registerSensors() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(this);
    }

    @Override
    protected int getNotificationID() {
        return Constants.NOTIFICATION_ID.LIGHT_SERVICE;
    }

    @Override
    protected String getNotificationContentText() {
        return null;
    }

    @Override
    protected int getNotificationIconResourceID() {
        return 0;
    }
}
