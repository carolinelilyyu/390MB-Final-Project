package cs.umass.edu.myactivitiestoolkit.services;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;

/**
 * Created by Yu on 12/13/16.
 */

public class LightService extends SensorService implements SensorEventListener{
    private SensorManager mSensorManager;
    private Sensor mLightSensor;

    @Override
    protected void registerSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        broadcastMessage(Constants.MESSAGE.LIGHT_SERVICE_STARTED);
        Log.d(TAG, "Starting light manager");

    }

    @Override
    protected void unregisterSensors() {
        if(mLightSensor != null){
            mSensorManager.unregisterListener(this, mLightSensor);
            broadcastMessage(Constants.MESSAGE.LIGHT_SERVICE_STOPPED);
            Log.d(TAG, "Stopping light manager");
        }
    }

    @Override
    protected int getNotificationID() {
        return Constants.NOTIFICATION_ID.LIGHT_SERVICE;
    }

    @Override
    protected String getNotificationContentText() {
        return getString(R.string.light_service_notification);
    }

    @Override
    protected int getNotificationIconResourceID() {
        return R.drawable.ic_running_white_24dp;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_LIGHT) {
            //lux inside is ~168
            final float lux = event.values[0];
            final long time = SystemClock.uptimeMillis();
            Log.d(TAG, "lux: " + lux);
            Log.d(TAG, "time: " + time);
            broadcastLightReading(time, lux);

        }
    }

    public void broadcastLightReading(final long time, final float lux) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.TIMESTAMP, time);
        intent.putExtra(Constants.KEY.LIGHT_DATA, lux);
        intent.setAction(Constants.ACTION.BROADCAST_LIGHT_DATA);
        Log.d(TAG, "broadcasting! Intent: " + intent.getAction());
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
