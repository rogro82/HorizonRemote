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

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ControllerFragment extends Fragment {

    public static final String ARG_CONTROLLER_ID = "controller_id";
    static final int TAG_KEY = 0;

    Handler mHandler;
    IControllerService mService;
    boolean mServiceBound;

    int mControllerId;
    int mControllerState = 0;
    SoftInputView softInput;

    public ControllerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        mControllerId = getArguments().getInt(ARG_CONTROLLER_ID);
        mHandler = new Handler();

        bindService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unbindService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_remote, container, false);

        /* bind keys */

        initializeButton(rootView, R.id.button_key_power, RemoteController.KEY_POWER);
        /* numeric keys */
        initializeButton(rootView, R.id.button_key_0, RemoteController.KEY_NUM_0);
        initializeButton(rootView, R.id.button_key_1, RemoteController.KEY_NUM_1);
        initializeButton(rootView, R.id.button_key_2, RemoteController.KEY_NUM_2);
        initializeButton(rootView, R.id.button_key_3, RemoteController.KEY_NUM_3);
        initializeButton(rootView, R.id.button_key_4, RemoteController.KEY_NUM_4);
        initializeButton(rootView, R.id.button_key_5, RemoteController.KEY_NUM_5);
        initializeButton(rootView, R.id.button_key_6, RemoteController.KEY_NUM_6);
        initializeButton(rootView, R.id.button_key_7, RemoteController.KEY_NUM_7);
        initializeButton(rootView, R.id.button_key_8, RemoteController.KEY_NUM_8);
        initializeButton(rootView, R.id.button_key_9, RemoteController.KEY_NUM_9);
        /* d-pad */
        initializeButton(rootView, R.id.button_key_up, RemoteController.KEY_DPAD_UP);
        initializeButton(rootView, R.id.button_key_down, RemoteController.KEY_DPAD_DOWN);
        initializeButton(rootView, R.id.button_key_left, RemoteController.KEY_DPAD_LEFT);
        initializeButton(rootView, R.id.button_key_right, RemoteController.KEY_DPAD_RIGHT);
        initializeButton(rootView, R.id.button_key_ok, RemoteController.KEY_OK);
        initializeButton(rootView, R.id.button_key_help, RemoteController.KEY_HELP);
        initializeButton(rootView, R.id.button_key_menu, RemoteController.KEY_MENU);
        initializeButton(rootView, R.id.button_key_info, RemoteController.KEY_INFO);
        initializeButton(rootView, R.id.button_key_back, RemoteController.KEY_BACK);

        initializeButton(rootView, R.id.button_key_demand, RemoteController.KEY_ONDEMAND);
        initializeButton(rootView, R.id.button_key_tv, RemoteController.KEY_TV);
        initializeButton(rootView, R.id.button_key_chan_up, RemoteController.KEY_CHAN_UP);
        initializeButton(rootView, R.id.button_key_dvr, RemoteController.KEY_DVR);
        initializeButton(rootView, R.id.button_key_guide, RemoteController.KEY_GUIDE);
        initializeButton(rootView, R.id.button_key_chan_dwn, RemoteController.KEY_CHAN_DWN);

        initializeButton(rootView, R.id.button_key_rwd, RemoteController.KEY_RWD);
        initializeButton(rootView, R.id.button_key_fwd, RemoteController.KEY_FWD);
        initializeButton(rootView, R.id.button_key_rec, RemoteController.KEY_RECORD);
        initializeButton(rootView, R.id.button_key_pause, RemoteController.KEY_PAUSE);
        initializeButton(rootView, R.id.button_key_stop, RemoteController.KEY_STOP);

        /* enable soft input */

        LinearLayout controllerLayout = (LinearLayout) rootView.findViewById(R.id.layout_controller);
        controllerLayout.addView((softInput = new SoftInputView(getActivity())));

        Button buttonSoftInput = (Button) rootView.findViewById(R.id.button_key_text);
        buttonSoftInput.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                softInput.requestFocus();
                softInput.toggleSoftInput();
            }
        });

        softInput.requestFocus();

        getActivity().setTitle(
                ControllerInfo.getName(getActivity(), mControllerId)
        );

        return rootView;
    }

    void initializeButton(View root, int viewid, int key) {
        /* TODO: Add long-press repeat for specific keys (d-pad and channel up/down) */

        View button = root.findViewById(viewid);
        button.setTag(key);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final Integer Key = (Integer) v.getTag();
                dispatchControllerEvent(ControllerService.EVENT_KEY_PRESS, Key);
            }
        });
    }

    OnTouchListener ButtonTouchListener = new OnTouchListener() {
        static final int DOWN_INTERVAL_MS = 500;
        long lastDown = 0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final Integer Key = (Integer) v.getTag();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    if (System.currentTimeMillis() - lastDown > DOWN_INTERVAL_MS) {
                        dispatchControllerEvent(ControllerService.EVENT_KEY_DOWN, Key);
                        lastDown = System.currentTimeMillis();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    dispatchControllerEvent(ControllerService.EVENT_KEY_PRESS, Key);
                    break;
            }

            return false;
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.controller, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connection:
                if (mService != null) {
                    try {
                        mService.dispatchEvent(mControllerId, ControllerService.EVENT_RECONNECT,
                                mControllerState == RemoteController.STATE_CONNECTING ? 1 : 0);
                    } catch (RemoteException e) {
                    }
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem connection = menu.findItem(R.id.action_connection);

        switch (mControllerState) {
            case RemoteController.STATE_CONNECTED:
                connection.setIcon(android.R.drawable.presence_online);
                break;
            case RemoteController.STATE_CONNECTING:
                connection.setIcon(android.R.drawable.presence_offline);
                break;
            case RemoteController.STATE_DISCONNECTED:
            case RemoteController.STATE_FAILURE:
                connection.setIcon(android.R.drawable.presence_busy);
                break;
        }

        super.onPrepareOptionsMenu(menu);
    }

    IControllerCallback.Stub callback = new IControllerCallback.Stub() {
        @Override
        public void OnControllerStateUpdate(int id, int state)
                throws RemoteException {

            if (mControllerId == id) {
                mHandler.post(new ControllerStateUpdate(state));
            }
        }
    };

    class ControllerStateUpdate implements Runnable {
        int state;

        public ControllerStateUpdate(int state) {
            this.state = state;
        }

        @Override
        public void run() {

            mControllerState = state;

            if (getActivity() != null) {

                /* reflect status change in ActionBar */

                getActivity().invalidateOptionsMenu();

                if (state == RemoteController.STATE_FAILURE)
                    Toast.makeText
                            (
                                    getActivity(),
                                    "Failed to connect",
                                    Toast.LENGTH_SHORT
                            )
                            .show();
            }
        }
    }

    private ServiceConnection connection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {

            mService = IControllerService.Stub.asInterface(service);

            try {
                if (mService != null)
                    mService.registerCallback(callback, mControllerId);

            } catch (RemoteException e) {
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    void dispatchControllerEvent(int action, int data) {
        if (mService != null) {
            try {
                mService.dispatchEvent(mControllerId, action, data);
            } catch (RemoteException e) {
            }
        }
    }

    void bindService() {
        getActivity().bindService(new Intent(this.getActivity(), ControllerService.class), connection,
                Context.BIND_AUTO_CREATE);

        mServiceBound = true;

    }

    void unbindService() {
        if (mServiceBound) {

            try {
                if (mService != null)
                    mService.unregisterCallback(callback);

            } catch (RemoteException e) {
            }

            getActivity().unbindService(connection);

            mServiceBound = false;
        }
    }

    /* dummy soft input view */

    public class SoftInputView extends View {
        public SoftInputView(Context context) {
            super(context);

            setFocusable(true);
            setFocusableInTouchMode(true);
        }

        public boolean toggleSoftInput() {
            Log.d("SoftInput", "Soft input toggled");

            InputMethodManager imm = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);

            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
            return true;
        }

        @Override
        public boolean onCheckIsTextEditor() {
            return true;
        }

        @Override
        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
            Log.d("SoftInput", "onKeyPreIme");
            if (event.isSystem()) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_VOLUME_DOWN:
                    case KeyEvent.KEYCODE_VOLUME_UP:
                        if (event.getAction() == KeyEvent.ACTION_UP) {
                            handleKeyCode(keyCode);
                        }
                        return true;
                    default:
                        return super.onKeyPreIme(keyCode, event);
                }
            }

            if (event.getAction() == KeyEvent.ACTION_UP)
                handleKeyCode(keyCode);

            return true;
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            outAttrs.inputType = EditorInfo.TYPE_NULL;
            return new BaseInputConnection(this, false) {

                @Override
                public boolean commitText(CharSequence text, int newCursorPosition) {
                    Log.d("SoftInput", "commitText");
                    for (int i = 0; i < text.length(); ++i)
                        handleKeyCode((int) text.charAt(i));

                    return true;
                }

                @Override
                public boolean deleteSurroundingText(int beforeLength,
                                                     int afterLength) {
                    Log.d("SoftInput", "deleteSurroundingText");
                    handleKeyCode(KeyEvent.KEYCODE_DEL);

                    return true;
                }

                @Override
                public boolean performEditorAction(int actionCode) {
                    Log.d("SoftInput", "performEditorAction " + actionCode);
                    if (actionCode == EditorInfo.IME_ACTION_UNSPECIFIED) {
                        dispatchControllerEvent(ControllerService.EVENT_KEY_PRESS, KeyEvent.KEYCODE_ENTER);
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean sendKeyEvent(KeyEvent event) {
                    Log.d("SoftInput", "sendKeyEvent " + event);
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        handleKeyCode(event.getKeyCode());
                    }
                    return true;
                }
            };
        }

        void handleKeyCode(int keyCode) {
            Log.d("SoftInput", "handleKeyCode " + keyCode);

            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    dispatchControllerEvent(ControllerService.EVENT_KEY_PRESS, RemoteController.KEY_CHAN_UP);
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    dispatchControllerEvent(ControllerService.EVENT_KEY_PRESS, RemoteController.KEY_CHAN_DWN);
                    break;
                case KeyEvent.KEYCODE_DEL:
                    dispatchControllerEvent(ControllerService.EVENT_KEY_PRESS, 0xFFFF);
                    break;
                default:
                    dispatchControllerEvent(ControllerService.EVENT_KEY_PRESS, keyCode);
                    break;
            }
        }


    }
}