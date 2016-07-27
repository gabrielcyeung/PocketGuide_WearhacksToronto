package com.example.brunoalmeida.wearhacks2016;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * A view displaying a live feed from the camera.
 *
 * Partial code for displaying the continuous camera feed taken from:
 * https://github.com/aron-bordin/Android-Tutorials/tree/master/SimpleCamera
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "CameraView";

    private Activity mActivity;

    private SurfaceHolder mHolder;
    private Camera mCamera = null;


    public CameraView(Activity activity) {
        super(activity);

        mActivity = activity;

        setCamera();

        // get the holder and set this class as the callback, so we can get camera data here
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
    }

    private void setCamera() {
        try {
            Log.v(TAG, "number of cameras: " + Camera.getNumberOfCameras());
            mCamera = Camera.open();    // you can use open(int) to use different cameras
        } catch (Exception e) {
            Log.e(TAG, "Failed to get camera: " + e.getMessage() + ", " + e.toString());
        }


        // mCamera.setDisplayOrientation(90);

        Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(0, info);
        int rotation = mActivity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        Log.v(TAG, "setCamera(): degrees = " + degrees);

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            // when the surface is created, we can set the camera to draw images in this SurfaceHolder
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "Camera error on surfaceCreated " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        Log.v(TAG, "surfaceChanged()");
        // before changing the application orientation, you need to stop the preview, rotate and then start it again
        if (mHolder.getSurface() == null)    // check if the surface is ready to receive camera data
            return;

        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            Log.i(TAG, "surfaceChanged(): stopping camera error - " + e);
            // this will happen when you are trying the camera if it's not running
        }

        setCamera();

        // now, recreate the camera preview
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "Camera error on surfaceChanged " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.v(TAG, "In surfaceDestroyed()");
        // our app has only one screen, so we'll destroy the camera in the surface
        // if you are using with more screens, move this code to the activity
        mCamera.stopPreview();
        mCamera.release();
    }

    public void activityOnResume() {
        Log.v(TAG, "In activityOnResume()");
        setCamera();
    }

    public void activityOnConfigurationChanged() {
        Log.v(TAG, "activityOnConfigurationChanged()");
        surfaceChanged(mHolder, 0, 0, 0);
    }

}
