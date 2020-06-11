package com.summertaker.jnews2

import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

abstract class SwipeActivity : AppCompatActivity() {
    private var gestureDetector: GestureDetector? = null

    inner class SwipeDetector : SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {

            // Check movement along the Y-axis. If it exceeds SWIPE_MAX_OFF_PATH,
            // then dismiss the swipe.
            if (abs(e1.y - e2.y) > SWIPE_MAX_OFF_PATH) {
                return false
            }

            //toast( "start = "+String.valueOf( e1.getX() )+" | end = "+String.valueOf( e2.getX() )  );
            //from left to right
            if (e2.x > e1.x) {
                if (e2.x - e1.x > SWIPE_MIN_DISTANCE && abs(
                        velocityX
                    ) > SWIPE_THRESHOLD_VELOCITY
                ) {
                    //Log.e(">>", "onSwipeRight()")
                    onSwipeRight()
                    return true
                }
            }
            if (e1.x > e2.x) {
                if (e1.x - e2.x > SWIPE_MIN_DISTANCE && abs(
                        velocityX
                    ) > SWIPE_THRESHOLD_VELOCITY
                ) {
                    //Log.e(">>", "onSwipeLeft()")
                    onSwipeLeft()
                    return true
                }
            }
            return false
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // TouchEvent dispatcher.
        if (gestureDetector != null) {
            if (gestureDetector!!.onTouchEvent(ev)) // If the gestureDetector handles the event, a swipe has been
            // executed and no more needs to be done.
                return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector?.onTouchEvent(event) ?: true
    }

    protected open fun initGesture() {
        gestureDetector = GestureDetector(this, SwipeDetector())
    }

    protected abstract fun onSwipeRight()
    protected abstract fun onSwipeLeft()

    companion object {
        private const val SWIPE_MIN_DISTANCE = 120
        private const val SWIPE_MAX_OFF_PATH = 250
        private const val SWIPE_THRESHOLD_VELOCITY = 200
    }
}