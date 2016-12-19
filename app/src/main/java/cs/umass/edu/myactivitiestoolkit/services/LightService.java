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

        Log.d(TAG, "Starting light manager");

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
        intent.putExtra("time", time);
        intent.putExtra("counter", lux);
        sendBroadcast(intent);

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
