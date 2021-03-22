package com.lumination.leadme;

import android.util.Log;
import android.view.MotionEvent;

class MyGestureDetector extends android.view.GestureDetector.SimpleOnGestureListener {

    final String TAG = MyGestureDetector.class.getSimpleName();

    // for touch left or touch right events
    private static final int SWIPE_MIN_DISTANCE = 80;   //default is 120
    private static final int SWIPE_MAX_OFF_PATH = 400;
    private static final int SWIPE_THRESHOLD_VELOCITY = 70;
    LeadMeMain main;
    public MyGestureDetector(LeadMeMain main){
        this.main=main;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return super.onSingleTapConfirmed(e);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d(TAG, " on filing event, first velocityX :" + velocityX +
                " second velocityY" + velocityY);
        try {
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                return false;
            if(e1.getX() - e2.getX()
                    > SWIPE_MIN_DISTANCE && Math.abs(velocityX)
                    > SWIPE_THRESHOLD_VELOCITY) {
                toLeft();
//                onHorizonTouch(true);  // left
            }  else if (e2.getX() - e1.getX()
                    > SWIPE_MIN_DISTANCE && Math.abs(velocityX)
                    > SWIPE_THRESHOLD_VELOCITY) {
                toRight();
//                onHorizonTouch(false); // right
            }
        } catch (Exception e) {
            // nothing
        }
        return false;
    }
    public void toLeft(){
        Log.d(TAG, "toLeft: ");
        if(main.onBoardPage<4) {
            main.setOnboardCurrent(main.onBoardPage+1);
        }
    }
    public void toRight(){
        Log.d(TAG, "toRight: ");
        if(main.onBoardPage>0) {
            main.setOnboardCurrent(main.onBoardPage-1);
        }
    }
}