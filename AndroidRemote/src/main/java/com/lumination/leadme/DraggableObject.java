package com.lumination.leadme;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class DraggableObject implements View.OnTouchListener {

    // variable that gesture dispatcher checks
    // to check if gesture is this navigation dragging
    // or something that should be replicated on student devices
    public static volatile boolean isDragging = false;

    // local variables
    private float dX, dY;
    private MainActivity main;
    private int prevMoves = 0;

    public DraggableObject(MainActivity main) {
        this.main = main;
    }

    /**
     * Determine which button on the draggable nav bar was pressed
     *
     * @param parent Navigation bar main container
     * @param x      x location of touch
     * @param y      y location of touch
     */
    private void findTouchedSubview(View parent, int x, int y) {
        //final ImageView returnBtn = parent.findViewById(R.id.returnBtn);
        final ImageView exitBtn = parent.findViewById(R.id.exitBtn);
        final ImageView logoView = parent.findViewById(R.id.logoView);
        final ImageView lockView = parent.findViewById(R.id.lockBtn);
        //final ImageView visibilityView = parent.findViewById(R.id.visibilityBtn);

        Rect testRec = new Rect();
        logoView.getHitRect(testRec);
        if (testRec.contains(x, y)) {
            main.returnToAppAndSendAction();
        }

        exitBtn.getHitRect(testRec);
        if (testRec.contains(x, y)) {
            main.exitApp();
        }

        lockView.getHitRect(testRec);
        if (testRec.contains(x, y)) {
            main.toggleStudentLock();
        }

//        visibilityView.getHitRect(testRec);
//        if (testRec.contains(x, y)) {
//            main.toggleStudentMenu();
//        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (prevMoves == 0) {
                    findTouchedSubview(view, (int) event.getX(), (int) event.getY());
                }
                isDragging = false;
                prevMoves = 0;
                break;

            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                dX = view.getX() - event.getRawX();
                dY = view.getY() - event.getRawY();
                break;

            case MotionEvent.ACTION_MOVE:
                prevMoves++;
                view.animate()
                        .x(event.getRawX() + dX)
                        .y(event.getRawY() + dY)
                        .setDuration(0)
                        .start();
                break;
            default:
        }

        return true;
    }
}
