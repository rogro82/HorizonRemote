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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class ControllerInfo {

    static final String CONTROLLER_PREFIX = "controller-";
    static final String PREFERENCES_NAME = "org.horizonremote_preferences";

    static final String DEFAULT_CONTROLLER_NAME = "Controller";
    static final String DEFAULT_CONTROLLER_ADDRESS = "192.168.0.0";

    public static final String PREF_NAME = "_name";
    public static final String PREF_ADDRESS = "_address";

    int id;
    String name;
    String address;

    public static ArrayList<ControllerInfo> get(Context context) {

        ArrayList<ControllerInfo> list = new ArrayList<ControllerInfo>();
        ArrayList<String> keys = getKeys(context);
        for (String key : keys) {
            ControllerInfo info = get(context, key);
            if (info != null)
                list.add(info);
        }

        return list;
    }

    public static ControllerInfo get(Context context, int id) {
        return get(context, key(id));
    }

    public static ControllerInfo get(Context context, String key) {
        if (!isValid(context, key))
            return null;

        ControllerInfo info = new ControllerInfo();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        info.id = prefs.getInt(key, 0);
        info.name = prefs.getString(key + PREF_NAME, DEFAULT_CONTROLLER_NAME);
        info.address = prefs.getString(key + PREF_ADDRESS, DEFAULT_CONTROLLER_ADDRESS);

        return info;
    }

    public static String add(Context context, String name, String address) {

        int lastId = 0;
        ArrayList<String> keys = getKeys(context);
        for (String key : keys) {
            String keyNr = key.replace(CONTROLLER_PREFIX, "");
            int id = Integer.valueOf(keyNr);
            if (id > lastId)
                lastId = id;
        }

        int newId = lastId + 1;
        String newKey = key(newId);

        SharedPreferences prefs = context.getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(newKey, newId);
        editor.putString(newKey + PREF_NAME, name);
        editor.putString(newKey + PREF_ADDRESS, address);
        editor.commit();

        return newKey;
    }

    public static void remove(Context context, int id) {
        remove(context, key(id));
    }

    public static void remove(Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> prefKeys = prefs.getAll().keySet();

        Editor editor = prefs.edit();

        for (String prefKey : prefKeys) {
            if (prefKey.startsWith(key))
                editor.remove(prefKey);
        }

        editor.commit();
    }

    public static ArrayList<String> getKeys(Context context) {

        ArrayList<String> names = new ArrayList<String>();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> prefKeys = prefs.getAll().keySet();
        for (String prefKey : prefKeys) {
            if (prefKey.startsWith(CONTROLLER_PREFIX)) {
                if (!prefKey.replace(CONTROLLER_PREFIX, "").contains("_")) {
                    names.add(prefKey);
                }
            }
        }

        return names;
    }

    public static String getName(Context context, int id) {
        return getName(context, key(id));
    }

    public static String getName(Context context, String key) {
        SharedPreferences profile = PreferenceManager.getDefaultSharedPreferences(context);
        return profile.getString(key + PREF_NAME, DEFAULT_CONTROLLER_NAME);
    }

    public static boolean isValid(Context context, String key) {
        if (key == null)
            return false;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return (prefs.getInt(key, -1) != -1);
    }

    public static String newKey(Context context) {
        int lastId = 0;
        ArrayList<String> keys = getKeys(context);
        for (String key : keys) {
            String keyNr = key.replace(CONTROLLER_PREFIX, "");
            int id = Integer.valueOf(keyNr);
            if (id > lastId)
                lastId = id;
        }
        int newId = lastId + 1;

        return key(newId);
    }

    public static int id(String key) {
        return Integer.valueOf(key.replace(CONTROLLER_PREFIX, ""));
    }

    public static String key(int id) {
        return String.format(Locale.getDefault(), "%s%d", CONTROLLER_PREFIX, id);
    }
}
