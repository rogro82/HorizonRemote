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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
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

public class MainFragment extends Fragment {

    private ListView mControllerList;
    ControllerInfoAdapter mControllerAdapter;

    public MainFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();

        mControllerAdapter = new ControllerInfoAdapter(this.getActivity(), ControllerInfo.get(this.getActivity()));
        if (mControllerList != null) {
            mControllerList.setAdapter(mControllerAdapter);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container,
                false);

        mControllerList = (ListView) rootView.findViewById(R.id.listview_controllers);
        mControllerList.setAdapter(mControllerAdapter);
        mControllerList.setEmptyView(rootView.findViewById(R.id.empty_view));
        mControllerList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showController((int) id);
                }
            }
        });

        Button buttonSettings = (Button) rootView.findViewById(R.id.button_settings);
        buttonSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });

        return rootView;
    }

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
