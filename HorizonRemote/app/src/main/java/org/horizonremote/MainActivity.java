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

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends Activity {

    static final int FRAGMENT_MAIN = 0;
    static final int FRAGMENT_CONTROLLER = 1;


    private CharSequence mTitle;
    private CharSequence mDrawerTitle;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private View mDrawerContent;

    private ListView mControllerList;
    ControllerInfoAdapter mControllerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawerContent = (View) findViewById(R.id.drawer_content);

        mControllerList = (ListView) findViewById(R.id.listview_controllers);
        mControllerList.setEmptyView(findViewById(R.id.empty_view));
        mControllerList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                showController((int) id);
            }
        });

        Button buttonSettings = (Button) findViewById(R.id.button_settings);
        buttonSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);

                /* show main fragment as the current remote might get removed */
                showFragment(FRAGMENT_MAIN, 0);
            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setBackgroundDrawable(null);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.drawable.ic_drawer,
                R.string.drawer_open,
                R.string.drawer_close
        ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            showFragment(FRAGMENT_MAIN, 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        /* load remote controllers */

        mControllerAdapter = new ControllerInfoAdapter(this, ControllerInfo.get(this));
        mControllerList.setAdapter(mControllerAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerContent);

        /* hide menu items when drawer is opened */

        int items = menu.size();
        for (int i = 0; i < items; ++i)
            menu.getItem(i).setVisible(!drawerOpen);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showController(int id) {
        showFragment(FRAGMENT_CONTROLLER, id);
    }

    private void showFragment(int type, int data) {
        Fragment fragment = null;

        switch (type) {
            case FRAGMENT_MAIN:
                fragment = new MainFragment();
                break;
            case FRAGMENT_CONTROLLER:

                fragment = new ControllerFragment();
                Bundle args = new Bundle();

                ControllerInfo info = ControllerInfo.get(this, data);
                if (info != null) {

                    setTitle(info.name);

                    args.putInt(ControllerFragment.ARG_CONTROLLER_ID, data);
                    fragment.setArguments(args);

                    mDrawerLayout.closeDrawer(mDrawerContent);
                }
                break;
        }

        /* show fragment */

        if (fragment != null) {

            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

        }
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /* Basic ControllerInfo adapter */

    class ControllerInfoAdapter extends BaseAdapter {
        Context context;
        ArrayList<ControllerInfo> list;

        public ControllerInfoAdapter(Context context, ArrayList<ControllerInfo> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return list.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view = convertView;

            if (view == null) {

                LayoutInflater inflater;
                inflater = LayoutInflater.from(context);
                view = inflater.inflate(R.layout.controller_list_item, null);

            }

            ControllerInfo info = list.get(position);

            if (info != null) {

                TextView textViewName = (TextView) view
                        .findViewById(R.id.text_controller_name);

                textViewName.setText(info.name);
            }

            return view;

        }

    }

}
