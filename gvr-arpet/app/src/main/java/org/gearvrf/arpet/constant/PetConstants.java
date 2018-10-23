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

package org.gearvrf.arpet.constant;

import org.gearvrf.arpet.BuildConfig;

public interface PetConstants {
    String GOOGLE_CLOUD_ANCHOR_KEY_NAME = "com.google.android.ar.API_KEY";
    int HOST_VISIBILITY_DURATION = BuildConfig.DEBUG ? 5 * 4 : 5 * 60; // in seconds
    int TEXTURE_BUFFER_SIZE = 2048;
}