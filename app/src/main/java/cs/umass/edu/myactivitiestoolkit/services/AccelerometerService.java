package cs.umass.edu.myactivitiestoolkit.services;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.processing.Filter;
import cs.umass.edu.myactivitiestoolkit.steps.OnStepListener;
import cs.umass.edu.myactivitiestoolkit.steps.StepDetector;
import edu.umass.cs.MHLClient.client.MessageReceiver;
import edu.umass.cs.MHLClient.client.MobileIOClient;
import edu.umass.cs.MHLClient.sensors.AccelerometerReading;
import edu.umass.cs.MHLClient.sensors.SensorReading;

/**
 * This service is responsible for collecting the accelerometer data on
 * the phone. It is an ongoing foreground service that will run even when your
 * application is not running. Note, however, that a process of your application
 * will still be running! The sensor service will receive sensor events in the
 * {@link #onSensorChanged(SensorEvent)} method defined in the {@link SensorEventListener}
 * interface.
 * <br><br>
 * <b>ASSIGNMENT 0 (Data Collection & Visualization)</b> :
 *      In this assignment, you will display and visualize the accelerometer readings
 *      and send the data to the server. In {@link #onSensorChanged(SensorEvent)},
 *      you should send the data to the main UI using the method
 *      {@link #broadcastAccelerometerReading(long, float[])}. You should also
 *      use the {@link #mClient} object to send data to the server. You can
 *      confirm it works by checking that both the local and server-side plots
 *      are updating (make sure your html script is running on your machine!).
 * <br><br>
 *
 * <b>ASSIGNMENT 1 (Step Detection)</b> :
 *      In this assignment, you will detect steps using the accelerometer sensor. You
 *      will design both a local step detection algorithm and a server-side (Python)
 *      step detection algorithm. Your algorithm should look for peaks and account for
 *      the fact that humans generally take steps every 0.5 - 2.0 seconds. Your local
 *      and server-side algorithms may be functionally identical, or you may choose
 *      to take advantage of other Python tools/libraries to improve performance.
 *      Call your local step detection algorithm from {@link #onSensorChanged(SensorEvent)}.
 *      <br><br>
 *      To listen for messages from the server,
 *      register a {@link MessageReceiver} with the {@link #mClient} and override
 *      the {@link MessageReceiver#onMessageReceived(JSONObject)} method to handle
 *      the message appropriately. The data will be received as a {@link JSONObject},
 *      which you can parse to acquire the step count reading.
 *      <br><br>
 *      We have provided you with the reading computed by the Android built-in step
 *      detection algorithm as an example and a ground-truth reading that you may
 *      use for comparison. Note that although the built-in algorithm has empirically
 *      been shown to work well, it is not perfect and may be sensitive to the phone
 *      orientation. Also note that it does not update the step count immediately,
 *      so don't be surprised if the step count increases suddenly by a lot!
 *  <br><br>
 *
 * <b>ASSIGNMENT 2 (Activity Detection)</b> :
 *      In this assignment, you will classify the user's activity based on the
 *      accelerometer data. The only modification you should make to the mobile
 *      app is to register a listener which will parse the activity from the acquired
 *      {@link org.json.JSONObject} and update the UI. The real work, that is
 *      your activity detection algorithm, will be running in the Python script
 *      and acquiring data from the server.
 *
 * @author CS390MB
 *
 * @see android.app.Service
 * @see <a href="http://developer.android.com/guide/components/services.html#Foreground">
 * Foreground Service</a>
 * @see SensorEventListener#onSensorChanged(SensorEvent)
 * @see SensorEvent
 * @see MobileIOClient
 */
public class AccelerometerService extends SensorService implements SensorEventListener {
    /** Used during debugging to identify logs by class */
    private static final String TAG = AccelerometerService.class.getName();

    /** Sensor Manager object for registering and unregistering system sensors */
    private SensorManager mSensorManager;
    private Sensor mPressure;


    /** Manages the physical accelerometer sensor on the phone. */
    private Sensor mAccelerometerSensor;

    /** Android built-in step detection sensor **/
    private Sensor mStepSensor;
    private Sensor mLightSensor;

    /** Defines your step detection algorithm. **/
    private final StepDetector mStepDetector;

    /** The step count as predicted by the Android built-in step detection algorithm. */
    private int mAndroidStepCount = 0;

    private int mServerStepCount = 0;

    private Filter mFilter;

    private static final double CUTOFF_FREQUENCY=3.0;

    private OnStepListener mStepListener;

    //private LocalBroadcastManager mLocalBroadcastManager;

    public AccelerometerService(){
        //mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mFilter = new Filter(CUTOFF_FREQUENCY);
        mStepDetector = new StepDetector();
        mStepDetector.registerOnStepListener(new OnStepListener(){

            @Override
            public void onStepCountUpdated(int stepCount) {
                broadcastLocalStepCount(stepCount);
                //broadcastAndroidStepCount(stepCount);
            }

            @Override
            public void onStepDetected(long timestamp, float[] values) {
                broadcastStepDetected(timestamp, values);
            }
        });
    }



    @Override
    protected void onServiceStarted() {
        broadcastMessage(Constants.MESSAGE.ACCELEROMETER_SERVICE_STARTED);
    }

    @Override
    protected void onServiceStopped() {
        broadcastMessage(Constants.MESSAGE.ACCELEROMETER_SERVICE_STOPPED);
    }

    @Override
    public void onConnected() {
        super.onConnected();
        mClient.registerMessageReceiver(new MessageReceiver(Constants.MHLClientFilter.STEP_DETECTED) {
            @Override
            protected void onMessageReceived(JSONObject json) {
                Log.d(TAG, "Received step update from server.");
                try {
                    JSONObject data = json.getJSONObject("data");
                    long timestamp = data.getLong("timestamp");
                    broadcastServerStepDetected(mServerStepCount++);
                    Log.d(TAG, "Step occurred at " + timestamp + ".");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mClient.registerMessageReceiver(new MessageReceiver(Constants.MHLClientFilter.ACTIVITY_DETECTED) {
            @Override
            protected void onMessageReceived(JSONObject json) {
                String activity;
                try {
                    JSONObject data = json.getJSONObject("data");
                    activity = data.getString("activity");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
                // TODO : broadcast activity to UI
                //broadcastServerStepDetected(timestamp, values);
            }
        });
    }

    /**
     * Register accelerometer sensor listener
     */
    @Override
    protected void registerSensors(){

        //TODO : (Assignment 0) Register the accelerometer sensor from the sensor manager.
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        //mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mStepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, mAccelerometerSensor , SensorManager.SENSOR_DELAY_NORMAL);
        //TODO : (Assignment 1) Register your step detector. Register an OnStepListener to receive step events
        mSensorManager.registerListener(this, mStepSensor, SensorManager.SENSOR_DELAY_UI); //changed to ui?
        mSensorManager.registerListener(mStepDetector, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Unregister the sensor listener, this is essential for the battery life!
     */
    @Override
    protected void unregisterSensors() {
        //TODO : Unregister your sensors. Make sure mSensorManager is not null before calling its unregisterListener method.

        if(mAccelerometerSensor != null) {
            mSensorManager.unregisterListener(this, mAccelerometerSensor);
        }
        if(mStepSensor != null){
            mSensorManager.unregisterListener(this, mStepSensor);

        }if(mSensorManager!= null){
            mStepDetector.unregisterOnStepListeners();
        }
        if(mLightSensor != null){
            mSensorManager.unregisterListener(this, mLightSensor);
        }

    }

    @Override
    protected int getNotificationID() {
        return Constants.NOTIFICATION_ID.ACCELEROMETER_SERVICE;
    }

    @Override
    protected String getNotificationContentText() {
        return getString(R.string.activity_service_notification);
    }

    @Override
    protected int getNotificationIconResourceID() {
        return R.drawable.ic_running_white_24dp;
    }

    /**
     * This method is called when we receive a sensor reading. We will be interested in this method primarily.
     * <br><br>
     *
     * Assignment 0 : Your job is to send the accelerometer readings to the server as you receive
     * them. Use the {@link #mClient} from the base class {@link SensorService} to communicate with
     * the data collection server. Specifically look at {@link MobileIOClient#sendSensorReading(SensorReading)}.
     * <br><br>
     *
     * We will be sending {@link AccelerometerReading}s. When instantiating an {@link AccelerometerReading},
     * pass in your user ID, which is accessible from the base sensor service, your device type and
     * your device identifier, as well as the timestamp and values of the sensor event.
     * <br><br>
     *
     * Note you may leave the device identifier a blank string. For the device type, you can use "MOBILE".
     * <br><br>
     *
     * You also want to broadcast the accelerometer reading to the UI. You can do this by calling
     * {@link #broadcastAccelerometerReading(long, float[])}.
     *
     * @see AccelerometerReading
     * @see SensorReading
     * @see MobileIOClient
     * @see SensorEvent
     * @see #broadcastAccelerometerReading(long, float[])
     */
    Filter smoothingfilter = new Filter(10);
    @Override
    public void onSensorChanged(SensorEvent event) {

        /*float[] floatfilteredvalues= new float[4];
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            // convert the timestamp to milliseconds (note this is not in Unix time)
            long timestamp_in_milliseconds = (long) ((double) event.timestamp / Constants.TIMESTAMPS.NANOSECONDS_PER_MILLISECOND);

            //TODO: Send the accelerometer reading to the server
            AccelerometerReading accelerometerReading = new AccelerometerReading(mUserID, "MOBILE", "", timestamp_in_milliseconds, event.values);
            mClient.sendSensorReading(accelerometerReading);
            //TODO: broadcast the accelerometer reading to the UI
            double[] filteredvalues = smoothingfilter.getFilteredValues(event.values[0],event.values[1],event.values[2]);
            floatfilteredvalues[0] = (float)Math.pow(filteredvalues[0], 2);
            floatfilteredvalues[1] = (float)Math.pow(filteredvalues[1], 2);
            floatfilteredvalues[2] = (float)Math.pow(filteredvalues[2], 2);
            floatfilteredvalues[3] = (float) (Math.abs(filteredvalues[2]) + Math.abs(filteredvalues[1]) + Math.abs(filteredvalues[0]));
            broadcastAccelerometerReading(timestamp_in_milliseconds, floatfilteredvalues);
            //mStepDetector.onSensorChanged(event);

            Log.d(TAG, "X: " + floatfilteredvalues[0]+ ", Y: " + floatfilteredvalues[1] + ", Z: " + floatfilteredvalues[2] + ", TOTAL: " + floatfilteredvalues[3]);

        }
        else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            // we received a step event detected by the built-in Android step detector (assignment 1)
            broadcastAndroidStepCount(mAndroidStepCount++);

        } else {

            // cannot identify sensor type
            Log.w(TAG, Constants.ERROR_MESSAGES.WARNING_SENSOR_NOT_SUPPORTED);

        }*/
        if(event.sensor.getType() == Sensor.TYPE_LIGHT){
            long timestamp_in_milliseconds = (long) ((double) event.timestamp / Constants.TIMESTAMPS.NANOSECONDS_PER_MILLISECOND);
            // 600UI/day
            //10 to 15 minutes outside per day
            long counter = 0;
            long start = 0;
            float prev = 0;
            while ((event.values[0] < 90) || (event.values[0] > 70)){
                if(prev != event.values[0]){
                    start = timestamp_in_milliseconds;
                    prev = event.values[0];
                }
                prev = event.values[0];
            }
            counter = timestamp_in_milliseconds - start;
            long percent = counter/900000;
        }
        else {
            // cannot identify sensor type
            Log.w(TAG, Constants.ERROR_MESSAGES.WARNING_SENSOR_NOT_SUPPORTED);

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

    // ***************** Methods for broadcasting step counts (assignment 1) *****************

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

    /**
     * Broadcasts the step count computed by your step detection algorithm
     * to other application components, e.g. the main UI.
     */
    public void broadcastLocalStepCount(int stepCount) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.STEP_COUNT, stepCount);
        intent.setAction(Constants.ACTION.BROADCAST_LOCAL_STEP_COUNT);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }


    // TODO: (Assignment 1) Broadcast the step count as computed by your server-side algorithm.
    public void broadcastServerStepDetected(int serverStepCount) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.STEP_COUNT, serverStepCount);
        intent.setAction(Constants.ACTION.BROADCAST_ACTIVITY);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }
    /**
     * Broadcasts a step event to other application components, e.g. the main UI.
     * Use this if you would like to visualize the detected step on the accelerometer signal.
     */
    public void broadcastStepDetected(long timestamp, float[] values) {
        Intent intent = new Intent();
        intent.putExtra(Constants.KEY.ACCELEROMETER_PEAK_TIMESTAMP, timestamp);
        intent.putExtra(Constants.KEY.ACCELEROMETER_PEAK_VALUE, values);
        intent.setAction(Constants.ACTION.BROADCAST_ACCELEROMETER_PEAK);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);
    }
}
