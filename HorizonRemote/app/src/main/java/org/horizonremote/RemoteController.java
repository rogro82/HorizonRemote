/*
 * Copyright 2013 Rob Groenendijk
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

package org.horizonremote;

import org.horizonremote.jni.JNIReference;

public class RemoteController extends JNIReference {

    public static final int STATE_FAILURE = -2;
    public static final int STATE_DISCONNECTED = -1;
    public static final int STATE_CONNECTING = 0;
    public static final int STATE_CONNECTED = 1;


    public static final int KEY_POWER = 0xe000;
    public static final int KEY_OK = 0xe001;
    public static final int KEY_BACK = 0xe002;
    public static final int KEY_CHAN_UP = 0xe006;
    public static final int KEY_CHAN_DWN = 0xe007;
    public static final int KEY_HELP = 0xe009;
    public static final int KEY_MENU = 0xe00a;
    public static final int KEY_GUIDE = 0xe00b;
    public static final int KEY_INFO = 0xe00e;
    public static final int KEY_TEXT = 0xe00f;
    public static final int KEY_MENU1 = 0xe011;
    public static final int KEY_MENU2 = 0xe015;
    public static final int KEY_DPAD_UP = 0xe100;
    public static final int KEY_DPAD_DOWN = 0xe101;
    public static final int KEY_DPAD_LEFT = 0xe102;
    public static final int KEY_DPAD_RIGHT = 0xe103;
    public static final int KEY_NUM_0 = 0xe300;
    public static final int KEY_NUM_1 = 0xe301;
    public static final int KEY_NUM_2 = 0xe302;
    public static final int KEY_NUM_3 = 0xe303;
    public static final int KEY_NUM_4 = 0xe304;
    public static final int KEY_NUM_5 = 0xe305;
    public static final int KEY_NUM_6 = 0xe306;
    public static final int KEY_NUM_7 = 0xe307;
    public static final int KEY_NUM_8 = 0xe308;
    public static final int KEY_NUM_9 = 0xe309;
    public static final int KEY_PAUSE = 0xe400;
    public static final int KEY_STOP = 0xe402;
    public static final int KEY_RECORD = 0xe403;
    public static final int KEY_FWD = 0xe405;
    public static final int KEY_RWD = 0xe407;
    public static final int KEY_MENU3 = 0xef00;
    public static final int KEY_UNKNOWN_0 = 0xef06;    // TIMESHIFT INFO
    public static final int KEY_UNKNOWN_1 = 0xef15;    // POWER
    public static final int KEY_UNKNOWN_2 = 0xef16;    // NR
    public static final int KEY_UNKNOWN_3 = 0xef17;    // RC PAIRING
    public static final int KEY_UNKNOWN_4 = 0xef19;    // TIMING
    public static final int KEY_ONDEMAND = 0xef28;
    public static final int KEY_DVR = 0xef29;
    public static final int KEY_TV = 0xef2a;

    public RemoteController(String address) throws JNIReferenceException {
        super(nativeCreate(address));
    }

    public void connect() {
        nativeConnect(getReferencePointer());
    }

    public void disconnect() {
        nativeDisconnect(getReferencePointer());
    }

    public int getState() {
        return nativeState(getReferencePointer());
    }

    public void sendKey(int keyCode, boolean keyDown) {
        nativeSendKey(getReferencePointer(), keyCode, keyDown);
    }

    public void toggleKey(int keyCode) {
        nativeToggleKey(getReferencePointer(), keyCode);
    }

    public boolean poll() {
        return nativePoll(getReferencePointer());
    }

    /* native methods */

    private static native long nativeCreate(String address);

    private static native void nativeConnect(long pointer);

    private static native void nativeDisconnect(long pointer);

    private static native int nativeState(long pointer);

    private static native void nativeSendKey(long pointer, int keycode, boolean down);

    private static native void nativeToggleKey(long pointer, int keycode);

    private static native boolean nativePoll(long pointer);
}
