package com.example.yu.sunlightapp.services;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.app.Activity;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import cs.umass.edu.myactivitiestoolkit.processing.Filter;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;



/**
 * Created by Yu on 12/3/16.
 */
public class AccelerometerService implements SensorEventListener{
    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;
    private Filter mFilter;
    private static final String TAG = AccelerometerService.class.getName();


    int[] Vals = new int[3];

    public int[] init(Context context){
        registerSensors(context);

        return Vals;
    }

    public void registerSensors(Context context){
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }


    @Override
    Filter smoothingfilter = new Filter(10);
    public void onSensorChanged(SensorEvent event){
        float[] floatfilteredvalues= new float[4];

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            Vals[0] = (int) event.values[0];
            Vals[1] = (int) event.values[1];
            Vals[2] = (int) event.values[2];
            //just in case we need to filter our values, will put this here.
            floatfilteredvalues[0] = (int) event.values[0];
            floatfilteredvalues[1] = (int) event.values[1];
            floatfilteredvalues[2] = (int) event.values[2];

            long timestamp_in_milliseconds = (long) ((double) event.timestamp / Constants.TIMESTAMPS.NANOSECONDS_PER_MILLISECOND);

            broadcastAccelerometerReading(timestamp_in_milliseconds, floatfilteredvalues);
        }
    }

    //unregister the listener
    @Override
    protected void unregisterSensors(){
        if(mAccelerometerSensor != null){
            mSensorManager.unregisterListener(this, mAccelerometerSensor);
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "Accuracy changed: " + accuracy);
    }

    /**
     * Broadcasts the accelerometer reading to other application components, e.g. the main UI.
     * @param accelerometerReadings the x, y, and z accelerometer readings
     */
    public void broadcastAccelerometerReading(final long timestamp, final float[] accelerometerReadings) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.TIMESTAMP, timestamp);
        intent.putExtra(Constants.KEY.ACCELEROMETER_DATA, accelerometerReadings);
        intent.setAction(Constants.ACTION.BROADCAST_ACCELEROMETER_DATA);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }


    /**
     * Broadcasts the step count computed by the Android built-in step detection algorithm
     * to other application components, e.g. the main UI.
     */
    public void broadcastAndroidStepCount(int stepCount) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.STEP_COUNT, stepCount);
        intent.setAction(Constants.ACTION.BROADCAST_ANDROID_STEP_COUNT);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }
}
