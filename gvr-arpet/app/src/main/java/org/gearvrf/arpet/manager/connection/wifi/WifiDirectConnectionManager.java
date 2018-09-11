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

package org.gearvrf.arpet.manager.connection.wifi;

import android.support.annotation.NonNull;

import org.gearvrf.arpet.connection.Device;
import org.gearvrf.arpet.connection.OnMessageListener;
import org.gearvrf.arpet.connection.socket.SocketConnectionThreadFactory;
import org.gearvrf.arpet.manager.connection.BaseSocketConnectionManager;

public class WifiDirectConnectionManager extends BaseSocketConnectionManager {

    @Override
    public void connectToDevices(@NonNull OnMessageListener messageListener, @NonNull Device... devices) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void startConnectionListener(@NonNull OnMessageListener messageListener) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void stopConnectionListener() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    protected SocketConnectionThreadFactory getSocketConnectionThreadFactory() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}