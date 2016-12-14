package cs.umass.edu.myactivitiestoolkit.view.fragments;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.clustering.Cluster;
import cs.umass.edu.myactivitiestoolkit.clustering.ClusteringRequest;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.location.FastConvexHull;
import cs.umass.edu.myactivitiestoolkit.location.GPSLocation;
import cs.umass.edu.myactivitiestoolkit.location.LocationDAO;
import cs.umass.edu.myactivitiestoolkit.services.AccelerometerService;
import cs.umass.edu.myactivitiestoolkit.services.LocationService;
import cs.umass.edu.myactivitiestoolkit.services.ServiceManager;
import cs.umass.edu.myactivitiestoolkit.util.PermissionsUtil;
import edu.umass.cs.MHLClient.client.MessageReceiver;
import edu.umass.cs.MHLClient.client.MobileIOClient;

/**
 * Created by sallyli on 12/13/16.
 */

public class SunlightFragment extends Fragment{

    private static final String TAG = LocationsFragment.class.getName();

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 4;
    MapView mapView;
    private GoogleMap map;
    private final List<Marker> locationMarkers;
    private boolean hideMarkers = false;
    private View btnToggleLocationService;
    private ServiceManager serviceManager;
    protected MobileIOClient client;
    protected String userID;

    public SunlightFragment(){
        locationMarkers = new ArrayList<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceManager = ServiceManager.getInstance(getActivity());
        userID = getString(R.string.mobile_health_client_user_id);
        client = MobileIOClient.getInstance(userID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_location, container, false);


        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        mapView = (MapView) rootView.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                SunlightFragment.this.map = mMap;
            }
        });

        View btnUpdate = rootView.findViewById(R.id.btnUpdateMap);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GPSLocation[] locations = getSavedLocations();
                if (locations.length == 0){
                    Toast.makeText(getActivity(), "No locations to cluster.", Toast.LENGTH_LONG).show();
                    return;
                }
                //Place a marker at each point and also adds it to the global list of markers
                map.clear();
                locationMarkers.clear();
                if (!hideMarkers) {
                    for (GPSLocation loc : locations) {
                        Marker marker = map.addMarker(new MarkerOptions()
                                .position(new LatLng(loc.latitude, loc.longitude)) //sets the latitude & longitude
                                .title("At " + LocationDAO.getISOTimeString(loc.timestamp))); //display the time it occurred when clicked
                        locationMarkers.add(marker);
                    }
                }
            }
        });

        View btnSettings = rootView.findViewById(R.id.btnMapsSettings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(getActivity(), view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_maps, popup.getMenu());
                popup.show();
                popup.getMenu().getItem(0).setTitle(hideMarkers ? "Show Markers" : "Hide Markers");
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.action_hide_markers) {
                            hideMarkers = !hideMarkers;
                            for (Marker marker : locationMarkers){
                                marker.setVisible(!hideMarkers);
                            }
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
            }
        });

        btnToggleLocationService = rootView.findViewById(R.id.btnToggleLocation);
        if (serviceManager.isServiceRunning(LocationService.class)) {
            btnToggleLocationService.setBackgroundResource(R.drawable.ic_location_on_black_48dp);
        } else {
            btnToggleLocationService.setBackgroundResource(R.drawable.ic_location_off_black_48dp);
        }
        btnToggleLocationService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!serviceManager.isServiceRunning(LocationService.class)) {
                    requestPermissions();
                }else{
                    serviceManager.stopSensorService(LocationService.class);
                }
            }
        });

        return rootView;
    }

    /**
     * When the fragment starts, register a {@link #receiver} to receive messages from the
     * {@link LocationService}. The intent filter defines messages
     * we are interested in receiving. We would like to receive notifications for when the
     * service has started and stopped in order to update the toggle icon appropriately.
     */
    @Override
    public void onStart() {
        super.onStart();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION.BROADCAST_MESSAGE);
        broadcastManager.registerReceiver(receiver, filter);
    }

    /**
     * When the fragment stops, e.g. the user closes the application or opens a new activity,
     * then we should unregister the {@link #receiver}.
     */
    @Override
    public void onStop() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        try {
            broadcastManager.unregisterReceiver(receiver);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
        super.onStop();
    }

    /**
     * Retrieves all locations saved in the local database.
     * @return a list of {@link GPSLocation}s.
     */
    private GPSLocation[] getSavedLocations(){
        LocationDAO dao = new LocationDAO(getActivity());
        try {
            dao.openRead();
            return dao.getAllLocations();
        } finally {
            dao.close();
        }
    }

    /**
     * The receiver listens for messages from the {@link AccelerometerService}, e.g. was the
     * service started/stopped, and updates the status views accordingly. It also
     * listens for sensor data and displays the sensor readings to the user.
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION.BROADCAST_MESSAGE)){
                    int message = intent.getIntExtra(Constants.KEY.MESSAGE, -1);
                    if (message == Constants.MESSAGE.LOCATION_SERVICE_STARTED){
                        btnToggleLocationService.setBackgroundResource(R.drawable.ic_location_on_black_48dp);
                    } else if (message == Constants.MESSAGE.LOCATION_SERVICE_STOPPED){
                        btnToggleLocationService.setBackgroundResource(R.drawable.ic_location_off_black_48dp);
                    }
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        System.gc();
        mapView.onLowMemory();
    }

    /**
     * Request permissions required for video recording. These include
     * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE},
     * and {@link android.Manifest.permission#CAMERA CAMERA}. If audio is enabled, then
     * the {@link android.Manifest.permission#RECORD_AUDIO RECORD_AUDIO} permission is
     * additionally required.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void requestPermissions(){
        String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

        if (!PermissionsUtil.hasPermissionsGranted(getActivity(), permissions)) {
            requestPermissions(permissions, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        onLocationPermissionGranted();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                //If the request is cancelled, the result array is empty.
                if (grantResults.length == 0) return;

                for (int i = 0; i < permissions.length; i++){
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                        switch (permissions[i]) {
                            case Manifest.permission.ACCESS_COARSE_LOCATION:
                                //TODO: Show status
                                return;
                            case Manifest.permission.ACCESS_FINE_LOCATION:
                                //TODO: Show status
                                return;
                            default:
                                return;
                        }
                    }
                }
                onLocationPermissionGranted();
            }
        }
    }

    /**
     * Called when location permissions have been granted by the user.
     */
    public void onLocationPermissionGranted(){
        serviceManager.startSensorService(LocationService.class);
    }
}
