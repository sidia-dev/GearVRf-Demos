/*
 * Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.gearvrf.arpet.gesture;

import android.view.MotionEvent;

public class RotationGestureDetector {

    private Touch mTouch1 = new Touch(), mTouch2 = new Touch();
    private float mAngle;
    private boolean mEnabled;

    public interface OnRotationGestureListener {
        void onRotate(RotationGestureDetector detector);
    }

    private static class Pointer {

        private static final int INVALID_ID = -1;

        int id = INVALID_ID;
        float x, y; // Position

        void reset() {
            id = INVALID_ID;
        }

        boolean isValid() {
            return id != INVALID_ID;
        }

        void setPosition(MotionEvent event) {
            x = event.getX(event.findPointerIndex(id));
            y = event.getY(event.findPointerIndex(id));
        }
    }

    private class Touch {

        Pointer pointer1 = new Pointer(), pointer2 = new Pointer();

        float angle(Touch other) {

            float angle1 = (float) Math.atan2((pointer1.y - pointer2.y), (pointer1.x - pointer2.x));
            float angle2 = (float) Math.atan2((other.pointer1.y - other.pointer2.y), (other.pointer1.x - other.pointer2.x));

            float angle = ((float) Math.toDegrees(angle1 - angle2)) % 360;
            if (angle < -180.f) angle += 360.0f;
            if (angle > 180.f) angle -= 360.0f;

            return angle;
        }

        void setPositions(MotionEvent event) {
            pointer1.setPosition(event);
            pointer2.setPosition(event);
        }

        boolean isValid() {
            return pointer1.isValid() && pointer2.isValid();
        }
    }

    private RotationGestureDetector.OnRotationGestureListener mListener;

    public float getAngle() {
        return mAngle;
    }

    public RotationGestureDetector(RotationGestureDetector.OnRotationGestureListener listener) {
        mListener = listener;
    }

    public void onTouchEvent(MotionEvent event) {

        switch (event.getActionMasked()) {
            // Detect first touch
            case MotionEvent.ACTION_DOWN:
                mTouch1.pointer1.id = event.getPointerId(event.getActionIndex());
                mTouch2.pointer1.id = mTouch1.pointer1.id;
                break;
            // Detect second touch
            case MotionEvent.ACTION_POINTER_DOWN:
                mTouch1.pointer2.id = event.getPointerId(event.getActionIndex());
                mTouch2.pointer2.id = mTouch1.pointer2.id;
                mTouch1.setPositions(event);
                break;
            // Detect rotation
            case MotionEvent.ACTION_MOVE:
                if (mTouch1.isValid()) {
                    mTouch2.setPositions(event);
                    mAngle = mTouch1.angle(mTouch2);
                    if (mListener != null) {
                        mListener.onRotate(this);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                mTouch1.pointer1.reset();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                mTouch1.pointer2.reset();
                break;

            case MotionEvent.ACTION_CANCEL:
                mTouch1.pointer1.reset();
                mTouch1.pointer2.reset();
                break;
        }
    }

    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

    public boolean isEnabled() {
        return mEnabled;
    }
}
