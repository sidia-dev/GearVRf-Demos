/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf.arpet;

import android.view.MotionEvent;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRMain;
import org.gearvrf.GVRPicker;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.ITouchEvents;
import org.gearvrf.mixedreality.GVRAnchor;
import org.gearvrf.mixedreality.GVRMixedReality;
import org.gearvrf.mixedreality.GVRPlane;
import org.gearvrf.mixedreality.GVRTrackingState;
import org.gearvrf.mixedreality.IAnchorEventsListener;
import org.gearvrf.mixedreality.IPlaneEventsListener;
import org.gearvrf.physics.GVRWorld;

public class PetMain extends GVRMain {
    private static final String TAG = "GVR_ARPET";

    private GVRScene mScene;
    private GVRContext mContext;
    private PetActivity.PetContext mPetContext;
    private GVRMixedReality mMixedReality;

    private BallThrowHandler ballThrowHandler;

    public PetMain(PetActivity.PetContext petContext) {
        mPetContext = petContext;
    }

    @Override
    public void onInit(final GVRContext gvrContext) throws Throwable {
        super.onInit(gvrContext);

        mContext = gvrContext;
        mScene = gvrContext.getMainScene();

        mMixedReality = new GVRMixedReality(gvrContext);
        mMixedReality.resume();

        GVRWorld world = new GVRWorld(gvrContext);
        world.setGravity(0f, -10f, 0f);
        mScene.getRoot().attachComponent(world);

        ballThrowHandler = new BallThrowHandler(gvrContext);
        ballThrowHandler.enable();
    }

    private IPlaneEventsListener mPlaneEventsListener = new IPlaneEventsListener() {
        @Override
        public void onPlaneDetection(GVRPlane plane) {

        }

        @Override
        public void onPlaneStateChange(GVRPlane plane, GVRTrackingState trackingState) {

        }

        @Override
        public void onPlaneMerging(GVRPlane childPlane, GVRPlane parentPlane) {

        }
    };

    private IAnchorEventsListener mAnchorEventsListener = new IAnchorEventsListener() {
        @Override
        public void onAnchorStateChange(GVRAnchor gvrAnchor, GVRTrackingState gvrTrackingState) {

        }
    };


    public class TouchEvents implements ITouchEvents {
        @Override
        public void onEnter(GVRSceneObject sceneObject, GVRPicker.GVRPickedObject pickedObject) {

        }

        @Override
        public void onExit(GVRSceneObject sceneObject, GVRPicker.GVRPickedObject pickedObject) {

        }

        @Override
        public void onTouchStart(GVRSceneObject sceneObject, GVRPicker.GVRPickedObject pickedObject) {

        }

        @Override
        public void onTouchEnd(GVRSceneObject sceneObject, GVRPicker.GVRPickedObject pickedObject) {

        }

        @Override
        public void onInside(GVRSceneObject sceneObject, GVRPicker.GVRPickedObject pickedObject) {

        }

        @Override
        public void onMotionOutside(GVRPicker picker, MotionEvent motionEvent) {

        }
    }

}
