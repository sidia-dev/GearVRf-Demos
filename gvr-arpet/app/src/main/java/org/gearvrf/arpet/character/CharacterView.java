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

package org.gearvrf.arpet.character;

import android.opengl.GLES30;
import android.support.annotation.NonNull;
import android.util.Log;

import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRBoxCollider;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;
import org.gearvrf.animation.GVRAnimation;
import org.gearvrf.arpet.AnchoredObject;
import org.gearvrf.arpet.PetContext;
import org.gearvrf.arpet.R;
import org.gearvrf.arpet.gesture.OnScaleListener;
import org.gearvrf.arpet.gesture.ScalableObject;
import org.gearvrf.arpet.gesture.impl.ScaleGestureDetector;
import org.gearvrf.arpet.mode.IPetView;
import org.gearvrf.arpet.util.LoadModelHelper;
import org.gearvrf.mixedreality.GVRPlane;
import org.gearvrf.mixedreality.IMRCommon;
import org.gearvrf.scene_objects.GVRCubeSceneObject;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class CharacterView extends AnchoredObject implements
        IPetView,
        ScalableObject {

    private final String TAG = getClass().getSimpleName();

    private IMRCommon mMixedReality;
    private List<OnScaleListener> mOnScaleListeners = new ArrayList<>();

    private GVRContext mContext;
    private GVRPlane mBoundaryPlane;
    private float[] mPlaneCenterPose = new float[16];
    private GVRSceneObject mCursor;
    private GVRSceneObject mShadow;
    public final static String PET_COLLIDER = "Pet collider";

    private GVRSceneObject m3DModel;

    CharacterView(@NonNull PetContext petContext) {
        super(petContext.getGVRContext(), petContext.getMixedReality(), null);

        mContext = petContext.getGVRContext();
        mMixedReality = petContext.getMixedReality();

        createShadow();

        // TODO: Load at thread
        petContext.runOnPetThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Loading pet 3D model...");
                load3DModel();
                Log.d(TAG, "Pet 3D model loaded!");
            }
        });
    }

    private void load3DModel() {
        m3DModel = LoadModelHelper.loadModelSceneObject(mContext, LoadModelHelper.PET_MODEL_PATH);
        m3DModel.getTransform().setScale(0.003f, 0.003f, 0.003f);

        addChildObject(m3DModel);

        final boolean showCollider = false;
        GVRSceneObject cube;

        // To debug the  collision
        if (!showCollider) {
            cube = new GVRSceneObject(mContext);
        }  else {
            GVRMaterial material = new GVRMaterial(mContext, GVRMaterial.GVRShaderType.Color.ID);
            material.setColor(1, 0, 0);
            cube = new GVRCubeSceneObject(mContext, true, material);
            cube.getRenderData().setDrawMode(GLES30.GL_LINE_LOOP);
        }

        GVRBoxCollider collider = new GVRBoxCollider(mContext);
        collider.setHalfExtents(0.5f, 0.5f, 0.5f);
        cube.attachCollider(collider);

        cube.getTransform().setPosition(0, 0.2f, 0);
        cube.getTransform().setScale(0.2f, 0.5f, 0.5f);

        cube.setName(PET_COLLIDER);
        addChildObject(cube);
    }

    public GVRAnimation getAnimation(int i) {
        return null;//m3DModel.getAnimations().get(i);
    }

    private void createShadow() {
        GVRTexture tex = mContext.getAssetLoader().loadTexture(new GVRAndroidResource(mContext, R.drawable.drag_shadow));
        GVRMaterial mat = new GVRMaterial(mContext);
        mat.setMainTexture(tex);
        mShadow = new GVRSceneObject(mContext, 0.3f, 0.6f);
        mShadow.getRenderData().setMaterial(mat);
        mShadow.getTransform().setRotationByAxis(-90f, 1f, 0f, 0f);
        mShadow.getTransform().setPosition(0f, 0.01f, -0.02f);
        mShadow.setName("shadow");
        mShadow.setEnable(false);
        addChildObject(mShadow);
    }

    @Override
    public boolean updatePose(float[] poseMatrix) {
        // Update y position to plane's y pos
        mBoundaryPlane.getCenterPose(mPlaneCenterPose);
        poseMatrix[13] = mPlaneCenterPose[13];

        if (mBoundaryPlane == null || mBoundaryPlane.isPoseInPolygon(poseMatrix)) {
            return super.updatePose(poseMatrix);
        }
        return false;
    }

    public void setBoundaryPlane(GVRPlane boundary) {
        boundary.getCenterPose(mPlaneCenterPose);
        mBoundaryPlane = boundary;
    }

    @Override
    public void scale(float factor) {
        getTransform().setScale(factor, factor, factor);
        notifyScale(factor);
    }

    public void rotate(float angle) {
        getTransform().rotateByAxis(angle, 0, 1, 0);
    }

    public void startDragging() {
        mShadow.setEnable(true);
        m3DModel.getTransform().setPositionY(0.2f);
    }

    public void stopDragging() {
        mShadow.setEnable(false);
        m3DModel.getTransform().setPositionY(0.0f);
    }

    public boolean isDragging() {
        return mShadow.isEnabled();
    }

    private synchronized void notifyScale(float factor) {
        for (OnScaleListener listener : mOnScaleListeners) {
            listener.onScale(factor);
        }
    }

    @Override
    public void addOnScaleListener(OnScaleListener listener) {
        if (!mOnScaleListeners.contains(listener)) {
            mOnScaleListeners.add(listener);
        }
    }

    @Override
    public void show(GVRScene mainScene) {
        mainScene.addSceneObject(getAnchor());
    }

    @Override
    public void hide(GVRScene mainScene) {
        mainScene.removeSceneObject(getAnchor());
    }

    /**
     * Sets the initial scale according to the distance between the pet and camera
     */
    public void setInitialScale() {
        final float MIN_DISTANCE = 1.0f;
        Vector3f vectorDistance = new Vector3f();
        float[] modelCam = mMixedReality.getCameraPoseMatrix();
        float[] modelCharacter = getAnchor().getPose();

        vectorDistance.set(modelCam[12], modelCam[13], modelCam[14]);
        // Calculates the distance in meters (coordinates from AR)
        float distance = vectorDistance.distance(modelCharacter[12], modelCharacter[13], modelCharacter[14]);

        if (distance < MIN_DISTANCE) {
            scale(ScaleGestureDetector.MIN_FACTOR);
        }
    }
}
