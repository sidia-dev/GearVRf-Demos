package org.gearvrf.arpet.mode;

import android.view.View;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.IViewEvents;
import org.gearvrf.arpet.R;
import org.gearvrf.scene_objects.GVRViewSceneObject;

public class PairingScene extends BasePetScene implements IViewEvents {
    private GVRContext mContext;
    private GVRSceneObject mPairing;

    public PairingScene(GVRContext gvrContext) {
        super(gvrContext);
        mContext = gvrContext;
        mPairing = new GVRViewSceneObject(mContext, R.layout.paring_layout, this);
    }

    @Override
    public void onInitView(GVRViewSceneObject gvrViewSceneObject, View view) {

    }

    @Override
    public void onStartRendering(GVRViewSceneObject pairingSceneObject, View view) {
        pairingSceneObject.getTransform().setScale(3.2f, 3.2f, 1.0f);
        pairingSceneObject.getTransform().setPosition(0.0f, 1.4f, -5.0f);
        addChildObject(mPairing);
    }
}