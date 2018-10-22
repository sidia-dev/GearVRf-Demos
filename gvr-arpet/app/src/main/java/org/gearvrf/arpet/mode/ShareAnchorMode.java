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

package org.gearvrf.arpet.mode;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import org.gearvrf.GVRCameraRig;
import org.gearvrf.arpet.PetContext;
import org.gearvrf.arpet.common.Task;
import org.gearvrf.arpet.common.TaskException;
import org.gearvrf.arpet.connection.socket.ConnectionMode;
import org.gearvrf.arpet.constant.ArPetObjectType;
import org.gearvrf.arpet.constant.PetConstants;
import org.gearvrf.arpet.manager.cloud.anchor.CloudAnchor;
import org.gearvrf.arpet.manager.cloud.anchor.CloudAnchorException;
import org.gearvrf.arpet.manager.cloud.anchor.CloudAnchorManager;
import org.gearvrf.arpet.manager.cloud.anchor.OnCloudAnchorManagerListener;
import org.gearvrf.arpet.manager.cloud.anchor.ResolvedCloudAnchor;
import org.gearvrf.arpet.manager.connection.IPetConnectionManager;
import org.gearvrf.arpet.manager.connection.PetConnectionEvent;
import org.gearvrf.arpet.manager.connection.PetConnectionEventHandler;
import org.gearvrf.arpet.manager.connection.PetConnectionEventType;
import org.gearvrf.arpet.manager.connection.PetConnectionManager;
import org.gearvrf.arpet.service.IMessageService;
import org.gearvrf.arpet.service.MessageCallback;
import org.gearvrf.arpet.service.MessageException;
import org.gearvrf.arpet.service.MessageService;
import org.gearvrf.arpet.service.SimpleMessageReceiver;
import org.gearvrf.arpet.service.data.ViewCommand;
import org.gearvrf.arpet.service.share.SharedMixedReality;
import org.gearvrf.mixedreality.GVRAnchor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.gearvrf.arpet.mode.ShareAnchorView.UserType.GUEST;

public class ShareAnchorMode extends BasePetMode {
    private final String TAG = getClass().getSimpleName();

    private static final int DEFAULT_SERVER_LISTENING_TIMEOUT = PetConstants.HOST_VISIBILITY_DURATION * 1000; // time in ms
    private static final int DEFAULT_GUEST_TIMEOUT = 10000;  // 10s for waiting to connect to the host
    private final int DEFAULT_SCREEN_TIMEOUT = 5000;  // 5s to change between screens

    private IPetConnectionManager mConnectionManager;
    private final Handler mHandler;
    private ShareAnchorView mShareAnchorView;
    private final GVRAnchor mWorldCenterAnchor;
    private CloudAnchorManager mCloudAnchorManager;
    private IMessageService mMessageService;
    private OnBackToHudModeListener mBackToHudModeListener;
    private SharedMixedReality mSharedMixedReality;

    public ShareAnchorMode(PetContext petContext, @NonNull GVRAnchor anchor, OnBackToHudModeListener listener) {
        super(petContext, new ShareAnchorView(petContext));
        mConnectionManager = PetConnectionManager.getInstance();
        mHandler = new Handler(petContext.getActivity().getMainLooper());
        mConnectionManager.addEventHandler(new AppConnectionMessageHandler());
        mShareAnchorView = (ShareAnchorView) mModeScene;
        mShareAnchorView.setListenerShareAnchorMode(new HandlerActionsShareAnchorMode());
        mWorldCenterAnchor = anchor;
        mCloudAnchorManager = new CloudAnchorManager(petContext, new CloudAnchorManagerReadyListener());
        mBackToHudModeListener = listener;
        mMessageService = MessageService.getInstance();
        mMessageService.addMessageReceiver(new MessageReceiver());
        mSharedMixedReality = (SharedMixedReality) petContext.getMixedReality();
    }

    @Override
    protected void onEnter() {

    }

    @Override
    protected void onExit() {

    }

    @Override
    protected void onHandleOrientation(GVRCameraRig cameraRig) {

    }

    public class HandlerActionsShareAnchorMode implements ShareAnchorListener {

        @Override
        public void OnHost() {
            if (!mCloudAnchorManager.hasInternetConnection()) {
                mShareAnchorView.showNoInternetMessage();
                return;
            }
            mConnectionManager.startInvitation();
        }

        @Override
        public void OnGuest() {
            if (!mCloudAnchorManager.hasInternetConnection()) {
                mShareAnchorView.showNoInternetMessage();
                return;
            }

            // Disable the planes detection
            mPetContext.unregisterPlaneListener();

            // Start to accept invitation from the host
            mConnectionManager.findInvitationThenConnect();

            // Change the views
            mShareAnchorView.modeGuest();
        }

        @Override
        public void OnBackShareAnchor() {
            mPetContext.getGVRContext().runOnGlThread(() -> mBackToHudModeListener.OnBackToHud());
        }

        @Override
        public void OnCancel() {
            mPetContext.getGVRContext().runOnGlThread(() -> mBackToHudModeListener.OnBackToHud());
        }

        @Override
        public void OnTry() {
            if (mShareAnchorView.getUserType() == GUEST) {
                OnGuest();
            } else {
                OnHost();
            }
        }

        @Override
        public void OnDisconnectScreen() {
            if (mShareAnchorView.getUserType() == GUEST) {
                mShareAnchorView.disconnectScreenGuest();
            } else {
                mShareAnchorView.disconnectScreenHost();
            }
            onSharingOff();
        }

        @Override
        public void OnConnectedScreen() {
            mShareAnchorView.modeView();
        }

        @Override
        public void OnTryPairingError() {
            if (!mCloudAnchorManager.hasInternetConnection()) {
                mShareAnchorView.showNoInternetMessage();
                return;
            }
            if (mShareAnchorView.getUserType() == GUEST) {
                mShareAnchorView.lookingSidebySide();
                doResolveGuest();
            } else {
                mShareAnchorView.toPairView();
                mCloudAnchorManager.clearAnchors();
                doHostAnchor();
            }
        }

        @Override
        public void OnContinue() {
            mConnectionManager.stopInvitation();
        }

        @Override
        public void OnCancelConnection() {
            if (mShareAnchorView.getUserType() == GUEST){
                mConnectionManager.cancelFindInvitation();
            }else {
                mConnectionManager.stopInvitationAndDisconnect();
                OnCancel();
            }
        }
    }

    private void showWaitingForScreen() {
        mShareAnchorView.modeHost();
        mHandler.postDelayed(this::OnWaitingForConnection, DEFAULT_SERVER_LISTENING_TIMEOUT);
    }

    private void showInviteAcceptedScreen() {
        if (mConnectionManager.getConnectionMode() == ConnectionMode.SERVER) {
            Log.d(TAG, "host");
            mShareAnchorView.inviteAcceptedHost(mConnectionManager.getTotalConnected());
            mHandler.postDelayed(() -> {
                mShareAnchorView.centerPetView();
                mHandler.postDelayed(() -> showMoveAround(), DEFAULT_SCREEN_TIMEOUT);
            }, DEFAULT_SCREEN_TIMEOUT);

            doHostAnchor();
        } else {
            Log.d(TAG, "guest");
            mShareAnchorView.inviteAcceptedGuest();
            mHandler.postDelayed(() -> showWaitingScreen(), DEFAULT_SCREEN_TIMEOUT);
        }
    }

    private void doHostAnchor() {
        mCloudAnchorManager.hostAnchor(mWorldCenterAnchor);
    }

    private void showSharedHost() {
        mPetContext.getActivity().runOnUiThread(() -> mShareAnchorView.sharedHost());
    }

    private void OnWaitingForConnection() {
        Log.d(TAG, "OnWaitingForConnection: time up");
        mConnectionManager.stopInvitation();
        if (mConnectionManager.getTotalConnected() > 0) {
            Log.d(TAG, "Total " + mConnectionManager.getTotalConnected());
            mShareAnchorView.inviteAcceptedGuest();
        }
    }

    private void showNotFound() {
        final String[] type = {""};
        mPetContext.getActivity().runOnUiThread(() -> {
            if (mShareAnchorView.getUserType() == GUEST) {
                type[0] = "Host";
                mShareAnchorView.notFound(type[0]);
            } else {
                type[0] = "Guest";
                mShareAnchorView.notFound(type[0]);
            }
        });
    }

    private void showPairingError() {
        mPetContext.getActivity().runOnUiThread(() -> mShareAnchorView.pairingError());
    }

    @SuppressLint("HandlerLeak")
    private class AppConnectionMessageHandler implements PetConnectionEventHandler {

        @SuppressLint("SwitchIntDef")
        @Override
        public void handleEvent(PetConnectionEvent message) {
            mPetContext.getActivity().runOnUiThread(() -> {
                @PetConnectionEventType
                int messageType = message.getType();
                switch (messageType) {
                    case PetConnectionEventType.CONN_CONNECTION_ESTABLISHED:
                        handleConnectionEstablished();
                        break;
                    case PetConnectionEventType.CONN_NO_CONNECTION_FOUND:
                        showNotFound();
                        break;
                    case PetConnectionEventType.CONN_ALL_CONNECTIONS_LOST:
                        onSharingOff();
                        mPetContext.getGVRContext().runOnGlThread(() -> mBackToHudModeListener.OnBackToHud());
                        break;
                    case PetConnectionEventType.CONN_ON_LISTENING_TO_GUESTS:
                        showWaitingForScreen();
                        break;
                    case PetConnectionEventType.CONN_GUEST_CONNECTION_ESTABLISHED:
                        mShareAnchorView.updateQuantityGuest(mConnectionManager.getTotalConnected());
                        showWaitingForScreen();
                        break;
                    case PetConnectionEventType.ERR_ENABLE_BLUETOOTH_DENIED:
                        showBluetoothDisabledMessage();
                        break;
                    case PetConnectionEventType.ERR_HOST_VISIBILITY_DENIED:
                        showDeviceNotVisibleMessage();
                        break;
                    default:
                        break;
                }
            });
        }
    }

    private void onSharingOff() {
        mSharedMixedReality.stopSharing();
    }

    private void handleConnectionEstablished() {
        switch (mConnectionManager.getConnectionMode()) {
            case ConnectionMode.CLIENT: {
                showInviteAcceptedScreen();
                break;
            }
            case ConnectionMode.SERVER: {
                showInviteAcceptedScreen();
                break;
            }
            case ConnectionMode.NONE:
            default:
                break;
        }
    }

    private void showBluetoothDisabledMessage() {
        mPetContext.getActivity().runOnUiThread(() -> mShareAnchorView.showBluetoothDisabledMessage());
    }

    private void showDeviceNotVisibleMessage() {
        mPetContext.getActivity().runOnUiThread(() -> mShareAnchorView.showDeviceNotVisibleMessage());
    }

    private void showWaitingScreen() {
        mPetContext.getActivity().runOnUiThread(() -> mShareAnchorView.waitingView());
    }

    private void showStayInPositionToPair() {
        mPetContext.getActivity().runOnUiThread(() -> mShareAnchorView.toPairView());
    }

    private void showCenterPet() {
        mPetContext.getActivity().runOnUiThread(() -> mShareAnchorView.centerPetView());
    }

    private void showMoveAround() {
        mPetContext.getActivity().runOnUiThread(() -> mShareAnchorView.moveAroundView());
    }

    private void showModeShareAnchorView() {
        mPetContext.getActivity().runOnUiThread(() -> mHandler.postDelayed(() -> showStatusModeView(), DEFAULT_SCREEN_TIMEOUT));
    }

    private void showStatusModeView() {
        mPetContext.getActivity().runOnUiThread(() -> mShareAnchorView.modeView());
    }

    private void showLookingSidebySide() {
        mPetContext.getActivity().runOnUiThread(() -> mShareAnchorView.lookingSidebySide());
    }

    private void showMainView() {
        mPetContext.getActivity().runOnUiThread(() -> mShareAnchorView.mainView());
    }

    private void sendCommandToShowModeShareAnchorView() {
        ViewCommand command = new ViewCommand(ViewCommand.SHOW_MODE_SHARE_ANCHOR_VIEW);
        mMessageService.sendViewCommand(command, new MessageCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                showModeShareAnchorView();
            }

            @Override
            public void onFailure(Exception error) {
                Log.e(TAG, "command to show 'paired view' has failed");
            }
        });
    }

    private void sendCommandPairingErrorView() {
        ViewCommand command = new ViewCommand(ViewCommand.PAIRING_ERROR_VIEW);
        mMessageService.sendViewCommand(command, new MessageCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "command to show 'pairing error  view' was performed successfully");
            }

            @Override
            public void onFailure(Exception error) {
                Log.e(TAG, "command to show 'pairing error view' has failed");
            }
        });
    }

    private void sendCommandSharedHost() {
        ViewCommand command = new ViewCommand(ViewCommand.SHARED_HOST);
        mMessageService.sendViewCommand(command, new MessageCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "command to show 'shared host view' was performed successfully");
            }

            @Override
            public void onFailure(Exception error) {
                Log.e(TAG, "command to show 'shared host view' has failed");
            }
        });
    }

    private void sendCommandLookingSideBySide() {
        ViewCommand command = new ViewCommand(ViewCommand.LOOKING_SIDE_BY_SIDE);
        mMessageService.sendViewCommand(command, new MessageCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "command to show 'looking side by  view' was performed successfully");
            }

            @Override
            public void onFailure(Exception error) {
                Log.e(TAG, "command to show 'looking side by  view' has failed");
            }
        });
    }

    private void shareScene(CloudAnchor[] anchors) {
        mMessageService.shareCloudAnchors(anchors, new MessageCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Inform the guests to change their view
                sendCommandToShowModeShareAnchorView();

                mSharedMixedReality.startSharing(mWorldCenterAnchor, SharedMixedReality.HOST);

                mBackToHudModeListener.OnBackToHud();
            }

            @Override
            public void onFailure(Exception error) {
                showPairingError();
                mBackToHudModeListener.OnBackToHud();
            }
        });
    }

    private class CloudAnchorManagerReadyListener implements OnCloudAnchorManagerListener {
        @Override
        public void onHostReady() {
            // Just inform the guests to change their view
            sendCommandSharedHost();
            sendCommandLookingSideBySide();
            // Change the host view
            showStayInPositionToPair();

            Log.d(TAG, "sending a list of CloudAnchor objects to the guests");
            doResolveGuest();
        }

        @Override
        public void onHostFailure() {
            // TODO: handle this error on UI
            Log.d(TAG, "host failure");
            showPairingError();
        }
    }

    private void doResolveGuest() {
        ArrayList<CloudAnchor> cloudAnchors = mCloudAnchorManager.getCloudAnchors();
        int listSize = cloudAnchors.size();
        shareScene(cloudAnchors.toArray(new CloudAnchor[listSize]));
    }

    private class MessageReceiver extends SimpleMessageReceiver {

        @Override
        public void onReceiveSharedCloudAnchors(CloudAnchor[] cloudAnchors) throws MessageException {
            Log.d(TAG, "Sharing received: " + Arrays.toString(cloudAnchors));
            CloudAnchorResolverTask task = new CloudAnchorResolverTask(cloudAnchors);
            // This method gets locked and will return after loader finish
            Log.d(TAG, "Resolving anchors...");
            task.start();
            Log.d(TAG, "Task ended");
            // Lock released, now checks thread result
            if (task.getError() != null) {
                sendCommandPairingErrorView();
                throw new MessageException(task.getError());
            } else {
                // Start sharing using resolved pet pose as guest's world center
                ResolvedCloudAnchor cloudAnchor = task.getResolvedCloudAnchorByType(ArPetObjectType.PET);
                if (cloudAnchor != null) {
                    mSharedMixedReality.startSharing(cloudAnchor.getAnchor(), SharedMixedReality.GUEST);
                    mBackToHudModeListener.OnBackToHud();
                }
            }
        }

        @Override
        public void onReceiveViewCommand(ViewCommand command) throws MessageException {
            try {
                Log.d(TAG, "View command received: " + command);
                switch (command.getType()) {
                    case ViewCommand.SHOW_MODE_SHARE_ANCHOR_VIEW:
                        showModeShareAnchorView();
                        break;
                    case ViewCommand.LOOKING_SIDE_BY_SIDE:
                        mHandler.postDelayed(() -> showLookingSidebySide(), DEFAULT_SCREEN_TIMEOUT);
                        break;
                    case ViewCommand.SHARED_HOST:
                        showSharedHost();
                        break;
                    case ViewCommand.PAIRING_ERROR_VIEW:
                        showPairingError();
                        break;
                    default:
                        Log.d(TAG, "Unknown view command: " + command.getType());
                        break;
                }
            } catch (Throwable t) {
                throw new MessageException("Error processing view command", t);
            }
        }
    }

    private class CloudAnchorResolverTask extends Task {

        CloudAnchor[] mCloudAnchors;
        List<ResolvedCloudAnchor> mResolvedCloudAnchors;

        CloudAnchorResolverTask(CloudAnchor[] cloudAnchors) {
            this.mCloudAnchors = cloudAnchors;
        }

        @Override
        public void execute() {

            mCloudAnchorManager.resolveAnchors(mCloudAnchors, new CloudAnchorManager.OnResolveCallback() {
                @Override
                public void onAllResolved(List<ResolvedCloudAnchor> resolvedCloudAnchors) {
                    mResolvedCloudAnchors = new ArrayList<>(resolvedCloudAnchors);
                    notifyExecuted();
                }

                @Override
                public void onError(CloudAnchorException e) {
                    setError(new TaskException(e));
                    notifyExecuted();
                }
            });
        }

        ResolvedCloudAnchor getResolvedCloudAnchorByType(@ArPetObjectType String type) {
            return mResolvedCloudAnchors.stream()
                    .filter(a -> a.getObjectType().equals(type))
                    .findFirst()
                    .orElse(null);
        }
    }
}
