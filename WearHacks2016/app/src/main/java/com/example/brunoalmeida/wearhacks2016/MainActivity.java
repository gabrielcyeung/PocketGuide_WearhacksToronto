package com.example.brunoalmeida.wearhacks2016;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.EstimoteSDK;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.estimote.sdk.BeaconManager;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * A full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "MainActivity";

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private static int PERMISSION_REQUEST_CODE_CAMERA = 1;

    /*
        private View mContentView;
    */
    private final Handler mHideHandler = new Handler();

    /*
        private View mControlsView;
    */
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
/*            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }*/
/*
            mControlsView.setVisibility(View.VISIBLE);
*/
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    private Camera mCamera = null;
    private FrameLayout m_camera_view = null;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            m_camera_view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LOW_PROFILE
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private CameraView mCameraView = null;
    private BeaconManager beaconManager;
    private List<Beacon> rangedBeacons = null;
    /*
    Define the hospital rooms here.
    The Room id should match the Room's zero-based index in the list.
     */
    private Room[] rooms = {
            new Room(0, 23105, 37595, "Emergency Room"),
            new Room(1, 22948, 14701, "Surgery Room"),
            new Room(2, 33491, 34365, "X-Ray Room"),
            new Room(3, 9652, 37519, "Maternity Ward"),
            new Room(4, 59932, 55122, "Intensive Care"),
            new Room(5, 24028, 20615, "Recovery Centre"),
            new Room(6, 64904, 53347, "Entertainment")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");

        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        m_camera_view = (FrameLayout) findViewById(R.id.camera_view);


        //  App ID & App Token can be taken from App section of Estimote Cloud.
        EstimoteSDK.initialize(this, getString(R.string.app_name), getString(R.string.app_name));
        // Optional, debug logging.
        EstimoteSDK.enableDebugLogging(true);

        onCreateBeacons();
        onCreateRooms();


        mVisible = true;

        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        m_camera_view.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);


/*        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);*/


        // Set up the user interaction to manually show or hide the system UI.
        m_camera_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
/*
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
*/


        /* Check for camera permission */

        // Permission is not already granted
        // Must request permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Request the write permission on Android 6.0+
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE_CAMERA);

            // Permission is already granted
        } else {
            cameraPermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.v(TAG, "In onRequestPermissionsResult()");

        if (requestCode == PERMISSION_REQUEST_CODE_CAMERA) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission granted
                cameraPermissionGranted();

            } else {
                // permission denied
            }
        }
    }

    private void cameraPermissionGranted() {
        Log.v(TAG, "Camera permission granted");

        try {
            Log.v(TAG, "number of cameras: " + Camera.getNumberOfCameras());
            mCamera = Camera.open();    // you can use open(int) to use different cameras
        } catch (Exception e) {
            Log.e(TAG, "Failed to get camera: " + e.getMessage());
        }

        if (mCamera != null) {
            mCameraView = new CameraView(this); // create a SurfaceView to show camera data
            FrameLayout camera_view = (FrameLayout) findViewById(R.id.camera_view);
            camera_view.addView(mCameraView);   // add the SurfaceView to the layout
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SystemRequirementsChecker.checkWithDefaultDialogs(this);

        if (mCameraView != null) {
            mCameraView.activityOnResume();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

/*
        mControlsView.setVisibility(View.GONE);
*/

        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        /*m_camera_view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        m_camera_view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        m_camera_view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);*/

        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.v(TAG, "onConfigurationChanged()");

        super.onConfigurationChanged(newConfig);

        mCameraView.activityOnConfigurationChanged();
    }

    /*
    Sets up the beacon manager and detection.
     */
    private void onCreateBeacons() {
        beaconManager = new BeaconManager(getApplicationContext());

        /*
        Use the RangingListener for more precise and frequent detection of specific beacons.
         */
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                Log.i(TAG, "onBeaconsDiscovered():\n" + region.toString() + "\n" + list.toString());

                rangedBeacons = list;
                updateView();
            }
        });

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                Log.i(TAG, "onServiceReady()");

                /*
                Search for all beacons with the default UUID.
                 */
                beaconManager.startRanging(new Region("General Beacon",
                        UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"),
                        null, null));
            }
        });
    }


    private enum Direction {
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    private class Room {
        int id;
        int major;
        int minor;
        String name;
        HashMap<Integer, Direction> nearbyRooms = new HashMap<>();

        Room(int id, int major, int minor, String name) {
            this.id = id;
            this.major = major;
            this.minor = minor;
            this.name = name;
        }
    }


    /*
    Define the hospital layout (directions to go between rooms).
    Each connection is in a single direction, so connections should be made in pairs (opposite connections).
     */
    private void onCreateRooms() {
        rooms[0].nearbyRooms.put(1, Direction.UP);
        rooms[1].nearbyRooms.put(0, Direction.DOWN);

        rooms[0].nearbyRooms.put(4, Direction.RIGHT);
        rooms[4].nearbyRooms.put(0, Direction.LEFT);

        rooms[1].nearbyRooms.put(5, Direction.RIGHT);
        rooms[5].nearbyRooms.put(1, Direction.LEFT);

        rooms[2].nearbyRooms.put(0, Direction.UP);
        rooms[0].nearbyRooms.put(2, Direction.DOWN);

        rooms[3].nearbyRooms.put(5, Direction.DOWN);
        rooms[5].nearbyRooms.put(3, Direction.UP);

        rooms[4].nearbyRooms.put(5, Direction.UP);
        rooms[5].nearbyRooms.put(4, Direction.DOWN);

        rooms[6].nearbyRooms.put(2, Direction.UP);
        rooms[2].nearbyRooms.put(6, Direction.DOWN);
    }

    public void updateView() {
        /* Display beacon list for debugging */

        String displayString = "";

        displayString += "General Beacons\n";
        for (Beacon beacon : rangedBeacons) {
            displayString += beacon.getMajor() + " " + beacon.getMinor() + "\n";
        }

        if (displayString.endsWith("\n")) {
            displayString = displayString.substring(0, displayString.lastIndexOf('\n'));
        }

        TextView debug_view = (TextView) findViewById(R.id.debug_view);
        debug_view.setText(displayString);




        /* Enable arrows and display text for connected rooms */
        ImageView left_arrow = (ImageView) findViewById(R.id.left_arrow);
        ImageView right_arrow = (ImageView) findViewById(R.id.right_arrow);
        ImageView up_arrow = (ImageView) findViewById(R.id.up_arrow);
        ImageView down_arrow = (ImageView) findViewById(R.id.down_arrow);

        TextView left_text = (TextView) findViewById(R.id.left_text);
        TextView right_text = (TextView) findViewById(R.id.right_text);
        TextView up_text = (TextView) findViewById(R.id.up_text);
        TextView down_text = (TextView) findViewById(R.id.down_text);

        left_arrow.setVisibility(View.INVISIBLE);
        right_arrow.setVisibility(View.INVISIBLE);
        up_arrow.setVisibility(View.INVISIBLE);
        down_arrow.setVisibility(View.INVISIBLE);

        left_text.setText("");
        right_text.setText("");
        up_text.setText("");
        down_text.setText("");


        TextView room_name = (TextView) findViewById(R.id.room_name);
        boolean isNameSet = false;

        /*
        Get the first room in the list of discovered rooms.
        (The `rangedBeacons` list is sorted by estimated proximity, or distance from the device).
         */
        if (rangedBeacons.size() > 0) {
            Beacon beacon = rangedBeacons.get(0);

            for (Room room : rooms) {
                if (beacon.getMajor() == room.major && beacon.getMinor() == room.minor) {
                    room_name.setText(room.name);
                    isNameSet = true;

                    for (int roomID : room.nearbyRooms.keySet()) {
                        switch (room.nearbyRooms.get(roomID)) {
                            case LEFT:
                                left_arrow.setVisibility(View.VISIBLE);
                                left_text.setText(rooms[roomID].name);
                                break;
                            case RIGHT:
                                right_arrow.setVisibility(View.VISIBLE);
                                right_text.setText(rooms[roomID].name);
                                break;
                            case UP:
                                up_arrow.setVisibility(View.VISIBLE);
                                up_text.setText(rooms[roomID].name);
                                break;
                            case DOWN:
                                down_arrow.setVisibility(View.VISIBLE);
                                down_text.setText(rooms[roomID].name);
                                break;
                        }
                    }

                    if (left_text.getWidth() > right_text.getWidth()) {
                        right_text.setWidth(left_text.getWidth());
                    } else if (right_text.getWidth() > left_text.getWidth()) {
                        left_text.setWidth(right_text.getWidth());
                    }

                    if (up_text.getWidth() > down_text.getWidth()) {
                        down_text.setWidth(up_text.getWidth());
                    } else if (down_text.getWidth() > up_text.getWidth()) {
                        up_text.setWidth(down_text.getWidth());
                    }

                    break;
                }
            }

            if (!isNameSet) {
                room_name.setText("Unknown Room");
                isNameSet = true;
            }
        }

        if (!isNameSet) {
            room_name.setText("SickKids Hospital");
            isNameSet = true;
        }
    }

}
