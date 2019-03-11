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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import java.util.ArrayList;

public class SettingsActivity extends PreferenceActivity {

    PreferenceCategory controllersCategory;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        addDynamicPreferences();
    }

    @SuppressWarnings("deprecation")
    private void addDynamicPreferences() {

        PreferenceScreen screen = getPreferenceScreen();

        controllersCategory = new PreferenceCategory(this);
        controllersCategory.setTitle(R.string.controllers);
        controllersCategory.setSummary(R.string.settings_controllers_summary);

        screen.addPreference(controllersCategory);
        reloadControllersCategory();

        PreferenceCategory detailsCategory = new PreferenceCategory(this);
        detailsCategory.setTitle(R.string.app_name);

        screen.addPreference(detailsCategory);

        Preference versionPref = new Preference(this);
        versionPref.setTitle(R.string.settings_version);
        versionPref.setSummary(R.string.app_version);

        Preference supportPref = new Preference(this);
        supportPref.setTitle(R.string.settings_support);
        supportPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("http://forum.xda-developers.com/showthread.php?t=2468149"));
                startActivity(i);

                return true;
            }
        });


        detailsCategory.addPreference(versionPref);
        detailsCategory.addPreference(supportPref);

    }

    private void reloadControllersCategory() {

        controllersCategory.removeAll();

        ArrayList<String> keys = ControllerInfo.getKeys(this);
        for (String key : keys) {
            controllersCategory.addPreference(createControllerScreen(key));
        }

        Preference addControllerPref = new Preference(this);
        addControllerPref.setTitle(R.string.settings_controller_add);
        addControllerPref.setIcon(android.R.drawable.ic_menu_add);
        addControllerPref.setOrder(1000);
        addControllerPref
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        createControllerScreen(ControllerInfo.newKey(SettingsActivity.this));
                        reloadControllersCategory();
                        return true;
                    }
                });

        controllersCategory.addPreference(addControllerPref);
    }

    @SuppressWarnings("deprecation")
    private PreferenceScreen createControllerScreen(String key) {

        PreferenceManager manager = getPreferenceManager();

        final String controllerName = ControllerInfo.getName(this, key);
        final String controllerKey = key;

        final PreferenceScreen controllerScreen = manager
                .createPreferenceScreen(this);

        controllerScreen.setTitle(controllerName);
        controllerScreen.setIcon(R.drawable.ic_action_dock);

        /* name */

        EditTextPreference namePref = new EditTextPreference(this);
        namePref.setTitle(R.string.settings_controller_name);
        namePref.setSummary(R.string.settings_controller_name_summary);
        namePref.setDialogTitle(R.string.settings_controller_name);
        namePref.setKey(key + ControllerInfo.PREF_NAME);
        namePref.setDefaultValue(ControllerInfo.DEFAULT_CONTROLLER_NAME);
        namePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {

                String name = (String) newValue;

                /* update screen and dialog title */

                controllerScreen.setTitle(name);
                controllerScreen.getDialog().setTitle(name);

                return true;
            }
        });

        controllerScreen.addPreference(namePref);

        EditTextPreference addressPref = new EditTextPreference(this);
        addressPref.setTitle(R.string.settings_controller_address);
        addressPref.setSummary(R.string.settings_controller_address_summary);
        addressPref.setDialogTitle(R.string.settings_controller_address);
        addressPref.setKey(key + ControllerInfo.PREF_ADDRESS);
        addressPref.setDefaultValue(ControllerInfo.DEFAULT_CONTROLLER_ADDRESS);
        addressPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {

                /* force restart controller */

                Intent intent = new Intent(SettingsActivity.this, ControllerService.class)
                        .putExtra(ControllerService.EXTRA_EVENT_ID, ControllerInfo.id(controllerKey))
                        .putExtra(ControllerService.EXTRA_EVENT_ACTION, ControllerService.EVENT_RECONNECT)
                        .putExtra(ControllerService.EXTRA_EVENT_DATA, 1);

                SettingsActivity.this.startService(intent);

                return true;
            }
        });

        controllerScreen.addPreference(addressPref);

        /* delete button */

        Preference deleteControllerPref = new Preference(this);
        deleteControllerPref.setTitle(R.string.settings_controller_remove);
        deleteControllerPref.setIcon(android.R.drawable.ic_menu_delete);
        deleteControllerPref.setKey(key);
        deleteControllerPref
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {

                        ControllerInfo.remove(SettingsActivity.this,
                                preference.getKey());

                        Intent intent = new Intent(SettingsActivity.this,
                                SettingsActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        SettingsActivity.this.startActivity(intent);
                        SettingsActivity.this.finish();

                        return true;
                    }
                });

        controllerScreen.addPreference(deleteControllerPref);
        /* make sure the record has a valid id based on its key */

        manager
                .getSharedPreferences()
                .edit()
                .putInt(key, ControllerInfo.id(key))
                .commit();

        return controllerScreen;
    }
}
