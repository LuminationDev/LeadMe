package com.lumination.leadme;

import android.content.Context;
import android.content.res.Configuration;
import android.gesture.GestureOverlayView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class GestureDetectionView extends GestureOverlayView implements GestureOverlayView.OnGestureListener {

    private final String TAG = "GestureDetectView";

    /**
     * Performs all common initialisation behaviours required for this extended class
     */
    private void init() {
        addOnGestureListener(this);
        setGestureVisible(false); //default is not to see these, but will turn on for host later

        setGestureColor(getResources().getColor(R.color.luminationLightAccent, null));
        setUncertainGestureColor(getResources().getColor(R.color.luminationLightAccent, null));
    }

    /**
     * Standard constructor with added GestureListener
     *
     * @param context
     * @see GestureDetectionView
     */
    public GestureDetectionView(Context context) {
        super(context);
        init();
    }

    /**
     * Standard constructor with added GestureListener
     *
     * @param context
     * @see GestureDetectionView
     */
    public GestureDetectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Standard constructor with added GestureListener
     *
     * @param context
     * @see GestureDetectionView
     */
    public GestureDetectionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Standard constructor with added GestureListener. Only available API 21 (Lollipop) and above.
     *
     * @param context
     * @see GestureDetectionView
     */
    public GestureDetectionView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * Detects the end of a gesture on the capture view, converts it to a GestureDescription
     * and sends it to the accessibility service for dispatch
     *
     * @param overlay
     * @param event
     */
    @Override
    public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
        if (DraggableObject.isDragging) {
            return; //this means the nav bar is being moved
        }

        //if you're a student with the lock on AND you're connected, can't gesture
        if (main.nearbyManager.isConnectedAsFollower() && main.studentLockOn) {
            Log.d(TAG, "Sorry, you're a student in follow mode - you can't gesture.");
            return;
        }

        try {
            //deploy the captured gesture locally
            long duration = event.getEventTime() - event.getDownTime();
            remoteDispatcherService.addGesture(overlay.getGesturePath(), duration);

            //package and write the gesture for remotely connected peers
            if(!main.dialogShowing) {
                remoteDispatcherService.gestureToBytes(MainActivity.GESTURE_TAG, overlay.getGesture(), duration);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in " + this.getClass().getCanonicalName() + ": " + e.getLocalizedMessage());
        }
    }

    private MainActivity main;
    private RemoteDispatcherService remoteDispatcherService;

    public void attachMainAndService(MainActivity main, RemoteDispatcherService remoteDispatcherService) {
        this.main = main;
        this.remoteDispatcherService = remoteDispatcherService;
    }

    /**
     * Not implemented.
     *
     * @param overlay
     * @param event
     */
    @Override
    public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
        //not needed
    }

    /**
     * Not implemented.
     *
     * @param overlay
     * @param event
     */
    @Override
    public void onGesture(GestureOverlayView overlay, MotionEvent event) {
        //not needed
    }

    /**
     * Not implemented.
     *
     * @param overlay
     * @param event
     */
    @Override
    public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {
        //not needed
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        main.refreshOverlay();
    }

}
