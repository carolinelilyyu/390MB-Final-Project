package cs.umass.edu.myactivitiestoolkit.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.location.GPSLocation;
import cs.umass.edu.myactivitiestoolkit.location.LocationDAO;
import edu.umass.cs.MHLClient.sensors.GPSReading;

/**
 * Created by Yu on 12/13/16.
 */

public class LightService extends SensorService implements SensorEventListener, LocationListener{
    private SensorManager mSensorManager;
    private Sensor mStepSensor;
    private Sensor mLightSensor;

    @Override
    protected void registerSensors() {
        mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void unregisterSensors() {
        if(mLightSensor != null){
            mSensorManager.unregisterListener(this, mLightSensor);
        }
    }

    @Override
    protected int getNotificationID() {
        return 0;
    }

    @Override
    protected String getNotificationContentText() {
        return null;
    }

    @Override
    protected int getNotificationIconResourceID() {
        return 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //need to lower values probably
        float prev = 0;
        long percentage = 0;
        if(event.sensor.getType() == Sensor.TYPE_LIGHT) {
            long timestamp_in_milliseconds = (long) ((double) event.timestamp / Constants.TIMESTAMPS.NANOSECONDS_PER_MILLISECOND);
            // 600UI/day
            //10 to 15 minutes outside per day
            List<Long> indirectLight = new ArrayList<Long>();
            List<Long> directLight = new ArrayList<Long>();
            float current = event.values[0];
            if (current >= 9500 && current <= 25500) {
                //indirect light measure -- scale of 1
                indirectLight.add(timestamp_in_milliseconds);
                prev = current;
            } else if (current >= 31500 && current <= 100000) {
                //direct light measure -- scale 1.5
                directLight.add(timestamp_in_milliseconds);
                prev = current;
            } else {
                long tempTimeIndirc = indirectLight.get(indirectLight.size() - 1) - indirectLight.get(0);
                long tempTimeDirc = directLight.get(directLight.size() - 1) - directLight.get(0);
                percentage = (long) (tempTimeDirc * 1.5 + tempTimeIndirc) / (long) 90000;

                //clearing all arrays for new set of readings
                indirectLight.clear();
                directLight.clear();
            }
        }
        else {
            // cannot identify sensor type
            Log.w(TAG, Constants.ERROR_MESSAGES.WARNING_SENSOR_NOT_SUPPORTED);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

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
}
