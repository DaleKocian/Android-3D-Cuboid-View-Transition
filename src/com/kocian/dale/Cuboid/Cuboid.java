/**
 * Copyright (c) 2013 Dale Kocian dale.kocian@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  @author Dale C. Kocian
 *
 */
package com.kocian.dale.Cuboid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;

public class Cuboid extends RelativeLayout {
    private static final String TAG = "Cuboid";
    private static final float SCALE_DOWN_FACTOR = 0.15f;
    private static final int ITEM_VERTICAL_SPACE = 50;
    private static final int DEFAULT_SCREEN = 3;
    private static final int INVALID_SCREEN = -1;
    private static final int DEGREES_PER_SCREEN = 5;
    private static final int TOUCH_STATE_RESTING = 0;
    private static final int TOUCH_STATE_CLICK = 1;
    private static final int TOUCH_STATE_SCROLL = 2;
    private final int childWidth;
    private final int childHeight;
    private final int swipeMinDistance;
    private final int scaledMinimumFlingVelocity;
    private final int swipeMaxOffPath;
    private final int scaledMaximumFlingVelocity;
    private int mTouchState = TOUCH_STATE_RESTING;
    private ArrayList<RelativeLayout> cubeSides;
    private int currentScreen;
    private int nextScreen;
    private GestureDetector mDetector;
    private float mInitialX;
    private float mInitialY;
    private Camera mCamera;
    private Matrix mMatrix;
    private Paint mPaint;
    private int cubeRotation;
    private float currentChildRotation;

    {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        swipeMinDistance = viewConfiguration.getScaledPagingTouchSlop();
        scaledMinimumFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        scaledMaximumFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        swipeMaxOffPath = viewConfiguration.getScaledTouchSlop();
        childWidth = (int)getResources().getDimension(R.dimen.layout_width);
        childHeight = (int)getResources().getDimension(R.dimen.layout_height);
    }

    public Cuboid(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(final Context context) {
        currentScreen = DEFAULT_SCREEN;
        nextScreen = INVALID_SCREEN;
        currentChildRotation = 0f;
        inflateAndAddLayouts(context);
        mDetector = new GestureDetector(context, mListener);
    }

    private void inflateAndAddLayouts(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        cubeSides = new ArrayList<RelativeLayout>();
        Cuboid cuboid = (Cuboid) findViewById(R.id.my_cube);
        RelativeLayout cubeSides1 = (RelativeLayout) inflater.inflate(R.layout.cube_side1, cuboid, false);
        RelativeLayout cubeSides2 = (RelativeLayout) inflater.inflate(R.layout.cube_side2, cuboid, false);
        RelativeLayout cubeSides3 = (RelativeLayout) inflater.inflate(R.layout.cube_side3, cuboid, false);
        RelativeLayout cubeSides4 = (RelativeLayout) inflater.inflate(R.layout.cube_side4, cuboid, false);
        cubeSides.add(cubeSides1);
        cubeSides.add(cubeSides2);
        cubeSides.add(cubeSides3);
        cubeSides.add(cubeSides4);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getChildCount() == 0) {
            for (RelativeLayout cubeSide : cubeSides) {
                addAndMeasureChild(cubeSide);
            }
            positionItems();
        } else {
            positionItems();
            invalidate();
        }

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mInitialX = event.getX();
                mInitialY = event.getY();
                mTouchState = TOUCH_STATE_CLICK;
                // Feed the down event to the detector so it has context when/if dragging begins
                mDetector.onTouchEvent(event);
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mDetector.onTouchEvent(event);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        drawChild(canvas, getChildAt(currentScreen), getDrawingTime());
    }

    @Override
    protected boolean drawChild(final Canvas canvas, final View child, final long drawingTime) {
        // get the bitmap
        final Bitmap bitmap = child.getDrawingCache();
        if (bitmap == null) {
            return super.drawChild(canvas, child, drawingTime);
        }
        // get top left coordinates
        int left = child.getLeft();
        int top = child.getTop();

        // get centerX and centerY
        final int childWidth = child.getWidth();
        final int childHeight = child.getHeight();
        final int centerX = childWidth / 2;
        final int centerY = childHeight / 2;
        // get scale
        final float halfHeight = getHeight() / 2;
        final float distFromCenter = (top + centerY - halfHeight) / halfHeight;
        final float scale = (float) (1 - SCALE_DOWN_FACTOR * (1 - Math.cos(distFromCenter)));
        // get rotation
        currentChildRotation = cubeRotation;
        drawFace(canvas, bitmap, top, left, centerX, centerY, scale, currentChildRotation);
        if (nextScreen != INVALID_SCREEN) {
            drawFace(canvas, getChildAt(nextScreen).getDrawingCache(), top, left, centerX, centerY, scale, 0);
            currentScreen = nextScreen;
            nextScreen = INVALID_SCREEN;
        }
        return false;
    }

    private void drawFace(final Canvas canvas, final Bitmap view, final int top, final int left,
                          final int centerX, final int centerY, final float scale, final float rotation) {

        // create the camera if we haven't before
        if (mCamera == null) {
            mCamera = new Camera();
        }

        // save the camera state
        mCamera.save();

        // translate and then rotate the camera
        mCamera.translate(0, 0, centerX);
        mCamera.rotateY(rotation);
        mCamera.translate(0, 0, -centerX);

        // create the matrix if we haven't before
        if (mMatrix == null) {
            mMatrix = new Matrix();
        }

        // get the matrix from the camera and then restore the camera
        mCamera.getMatrix(mMatrix);
        mCamera.restore();

        // translate and scale the matrix
        mMatrix.preTranslate(-centerX, -centerY);
        mMatrix.postScale(scale, scale);
        mMatrix.postTranslate(left + centerX, top + centerY);

        // create and initialize the paint object
        if (mPaint == null) {
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setFilterBitmap(true);
        }

        // draw the bitmap
        canvas.drawBitmap(view, mMatrix, mPaint);
    }

    /**
     * Adds a view as a child view and takes care of measuring it
     *
     * @param child The view to add
     */

    private void addAndMeasureChild(final View child) {
        LayoutParams params = (LayoutParams) child.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }
        child.setDrawingCacheEnabled(true);
        addViewInLayout(child, 0, params, true);

        child.measure(MeasureSpec.EXACTLY | getWidth(), MeasureSpec.UNSPECIFIED);
    }

    /** Positions the children at the "correct" positions */
    private void positionItems() {
        for (int index = 0; index < cubeSides.size(); ++index) {
            final View child = getChildAt(index);
            final int width = childWidth;
            final int height = childHeight;
            final int left = (getWidth() - width) / 2;

            child.layout(left, ITEM_VERTICAL_SPACE, left + width, height);
        }
    }

    //Listener to handle all the touch events
    private GestureDetector.SimpleOnGestureListener mListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // Check movement along the Y-axis. If it exceeds SWIPE_MAX_OFF_PATH, then dismiss the swipe.
            /*if (Math.abs(e1.getY() - e2.getY()) > swipeMaxOffPath) {
                return false;
            } else */
            if (e1.getX() > e2.getX() && Math.abs(e1.getX() - e2.getX()) > swipeMinDistance
                    && isVelocityWithinRange(velocityX)) {
                Toast.makeText(getContext(), "Left", Toast.LENGTH_SHORT).show();
                nextScreen = getPrevScreen();
                rotateCube(-90);

            } else if (e1.getX() < e2.getX() && e2.getX() - e1.getX() > swipeMinDistance
                    && isVelocityWithinRange(velocityX)) {
                Toast.makeText(getContext(), "Right", Toast.LENGTH_SHORT).show();
                nextScreen = getNextScreen();
                rotateCube(90);
            }
            return true;
        }
    };

    private boolean isVelocityWithinRange(float velocityX) {
        return Math.abs(velocityX) > scaledMinimumFlingVelocity && Math.abs(velocityX) < scaledMaximumFlingVelocity;
    }

    private void rotateCube(final int scrolledDistance) {
        Log.d(TAG, "scrolledDistance" + String.valueOf(scrolledDistance));
        cubeRotation = scrolledDistance;
        requestLayout();
    }

    private int getNextScreen() {
        return (currentScreen + 1) % 4;
    }

    private int getPrevScreen() {
        return currentScreen - 1 < 0 ? 3 : currentScreen - 1;
    }
}
