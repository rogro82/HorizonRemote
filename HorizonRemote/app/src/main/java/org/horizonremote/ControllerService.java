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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;

public class ControllerService extends Service {

    static final String LOG_TAG = "ControllerService";

    public static final String ACTION_EVENT = "horizonremote.intent.action.EVENT";
    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_EVENT_ACTION = "event_action";
    public static final String EXTRA_EVENT_DATA = "event_data";

    public static final int EVENT_KEY_PRESS = 0;
    public static final int EVENT_KEY_DOWN = 1;
    public static final int EVENT_KEY_UP = 2;
    public static final int EVENT_RECONNECT = 3;

    boolean started;
    final ArrayList<ControllerInstance> instances =
            new ArrayList<ControllerInstance>();

    @Override
    public void onCreate() {
        if (!started) {
            this.startService(new Intent(this, ControllerService.class));
        }

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!started) {
            started = true;
        }

        /* see if this command has an event we should handle or that its just a trigger to start
         * this service so that we will not loose it immediately after all bindings are gone */

        if (intent != null && ACTION_EVENT.equals(intent.getAction())) {

            int id = intent.getIntExtra(EXTRA_EVENT_ID, 0);
            int action = intent.getIntExtra(EXTRA_EVENT_ACTION, EVENT_KEY_PRESS);
            int data = intent.getIntExtra(EXTRA_EVENT_DATA, 0);

            getControllerInstance(id).dispatchEvent(action, data);
        }

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    IControllerService.Stub binder = new IControllerService.Stub() {
        @Override
        public void unregisterCallback(IControllerCallback cb)
                throws RemoteException {

            synchronized (instances) {
                for (ControllerInstance instance : instances) {
                    instance.unregisterCallback(cb);
                }
            }
        }

        @Override
        public void registerCallback(IControllerCallback cb, int id)
                throws RemoteException {

            getControllerInstance(id)
                    .registerCallback(cb);
        }

        @Override
        public void dispatchEvent(int id, int action, int data)
                throws RemoteException {

            getControllerInstance(id)
                    .dispatchEvent(action, data);
        }
    };

    ControllerInstance getControllerInstance(int id) {
        synchronized (instances) {

            /* see if we already have a ControllerInstance with this id */

            for (ControllerInstance instance : instances) {
                if (instance.id == id)
                    return instance;
            }

            /* start a new ControllerInstance for id */

            Log.d(LOG_TAG, "creating new controller for id: " + id);

            ControllerInstance instance = new ControllerInstance(id);
            instances.add(instance);

            return instance;
        }
    }

    class ControllerInstance {

        int id;
        ControllerThread thread;
        volatile int state;

        final ArrayList<IControllerCallback> callbacks =
                new ArrayList<IControllerCallback>();

        final ArrayList<ControllerEvent> events =
                new ArrayList<ControllerEvent>();

        public ControllerInstance(int id) {
            this.id = id;
        }

        void startController(boolean forceRestart) {
            if (forceRestart && thread != null) {
                try {

                    /* force stop possibly active thread */
                    thread.cancel();
                    thread.join(500);

                } catch (InterruptedException e) {
                }

                thread = null;
            }

            /* start thread if its not already running */

            if (thread == null || !thread.isAlive()) {
                thread = new ControllerThread();
                thread.start();
            }
        }

        public void registerCallback(IControllerCallback cb) {
            synchronized (callbacks) {
                try {
                    callbacks.add(cb);
                    cb.OnControllerStateUpdate(id, state);
                    Log.d(LOG_TAG, "registered callback @ controller " + id);

                } catch (Exception ex) {
                    /* remove dead callback */
                    Log.d(LOG_TAG, "removed dead callback @ controller " + id);
                    callbacks.remove(cb);
                }
            }

            /* start controller if its not running */
            startController(false);
        }

        public void unregisterCallback(IControllerCallback cb) {
            synchronized (callbacks) {
                if (callbacks.remove(cb)) {
                    Log.d(LOG_TAG, "unregistered callback @ controller " + id);
                }
            }
        }

        public void dispatchEvent(int action, int data) {
            switch (action) {
                case EVENT_RECONNECT:
                    /* handle reconnect event locally */
                    startController(data > 0);
                    break;
                default:
                    /* start controller if its not running */
                    startController(false);

                    /* schedule event */
                    synchronized (events) {
                        events.add(new ControllerEvent(action, data));
                    }
                    break;
            }
        }

        void updateState(int state) {
            this.state = state;

            synchronized (callbacks) {
                for (IControllerCallback cb : callbacks) {
                    try {
                        cb.OnControllerStateUpdate(id, state);
                    } catch (RemoteException e) {
                        /* remove dead callback */
                        Log.d(LOG_TAG, "removed dead callback @ controller " + id);
                        callbacks.remove(cb);
                    }
                }
            }

            /* clear any unhandled events */

            if (state == RemoteController.STATE_DISCONNECTED) {
                synchronized (events) {
                    events.clear();
                }
            }
        }

        class ControllerThread extends Thread {

            static final int EVENT_INTERVAL_MILLIS = 100;
            static final int CONNECTION_RETRY_COUNT = 3;
            static final int CONNECTION_RETRY_MILLIS = 500;
            static final int CONNECTION_POLL_MILLIS = 5000;
            static final int CONNECTION_INACTIVITY_MILLIS = 30000;

            boolean shouldstop;
            long lastpoll;
            long lastevent;

            RemoteController controller;

            final ArrayList<ControllerEvent> batch =
                    new ArrayList<ControllerEvent>();

            public void cancel() {
                this.shouldstop = true;
            }

            @Override
            public void run() {

                updateState(RemoteController.STATE_CONNECTING);

                /* get controller properties */

                ControllerInfo info = ControllerInfo.get(ControllerService.this, id);
                if (info != null) {

                    Log.d(LOG_TAG, "starting controller (name: " + info.name + ", addr: " + info.address + ")");

                    try {

                        controller = new RemoteController(info.address);
                        controller.connect();

                        Log.d(LOG_TAG, "controller connected");

                        int retrycnt = 0;

                        while (controller.getState() != RemoteController.STATE_CONNECTED
                                && retrycnt < CONNECTION_RETRY_COUNT) {
                            try {
                                /* sleep before retrying */
                                Thread.sleep(CONNECTION_RETRY_MILLIS);
                            } catch (InterruptedException e) {
                            }

                            controller.connect();
                            retrycnt++;
                        }

                        if (controller.getState() != RemoteController.STATE_CONNECTED) {
                            Log.d(LOG_TAG, "failed to start controller");

                            updateState(RemoteController.STATE_FAILURE);

                        } else {
                            Log.d(LOG_TAG, "controller is entering running state");

                            updateState(RemoteController.STATE_CONNECTED);

                            lastevent = System.currentTimeMillis();
                            lastpoll = lastevent;

                            while (!shouldstop) {

                                final long currenttime = System.currentTimeMillis();

                                /* copy events to thread local events so that we do not block any new incoming
                                 * events while processing them */

                                int batchsize = 0;

                                synchronized (events) {
                                    batchsize = events.size();

                                    if (batchsize > 0) {
                                        /* move events to batch */
                                        batch.addAll(events);
                                        events.clear();
                                    }
                                }

                                if (batchsize > 0) {

                                    /* start processing events in batch */

                                    for (ControllerEvent ev : batch) {
                                        switch (ev.action) {
                                            case EVENT_KEY_PRESS:
                                                controller.toggleKey(ev.data);
                                                break;
                                            case EVENT_KEY_DOWN:
                                                controller.sendKey(ev.data, true);
                                                break;
                                            case EVENT_KEY_UP:
                                                controller.sendKey(ev.data, false);
                                                break;
                                        }
                                    }

                                    lastevent = currenttime;

                                    /* remove processed events */

                                    batch.clear();
                                }

                                /* check if we should disconnect based on inactivity ( when we have no registered callbacks ) */

                                final long inactivity = currenttime - lastevent;
                                if ((inactivity > CONNECTION_INACTIVITY_MILLIS) && callbacks.isEmpty()) {
                                    Log.d(LOG_TAG, "stopping controller (id:" + id + ") because of " + inactivity + "ms inactivity");
                                    break;
                                }

                                /* check if the RemoteController is still connected */

                                if (currenttime - lastpoll > CONNECTION_POLL_MILLIS) {
                                    lastpoll = currenttime;

                                    if (controller.poll()) {
                                        /* dispatch heartbeat to callbacks so that we can filter out
                                         * any possibly dead callbacks who failed to unregister */
                                        updateState(RemoteController.STATE_CONNECTED);
                                    } else {
                                        Log.d(LOG_TAG, "stopping controller (id:" + id + ") because we are no longer connected");
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                    }

                    /* disconnect in case we are stopping because of inactivity */

                    controller.disconnect();

                }

                updateState(RemoteController.STATE_DISCONNECTED);
            }
        }

        class ControllerEvent {
            int action;
            int data;

            public ControllerEvent(int action, int data) {
                this.action = action;
                this.data = data;
            }
        }
    }
}