package com.photosynq.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.http.PhotosynqResponse;
import com.photosynq.app.model.AppSettings;
import com.photosynq.app.model.Macro;
import com.photosynq.app.model.Option;
import com.photosynq.app.model.Protocol;
import com.photosynq.app.model.Question;
import com.photosynq.app.model.ResearchProject;
import com.photosynq.app.utils.BluetoothService;
import com.photosynq.app.utils.CommonUtils;
import com.photosynq.app.utils.Constants;
import com.photosynq.app.utils.PrefUtils;
import com.photosynq.app.utils.SyncHandler;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;


public class QuickModeFragment extends Fragment implements PhotosynqResponse , SwipeRefreshLayout.OnRefreshListener{

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static int mSectionNumber;

    private DatabaseHelper dbHelper;
    private ProtocolArrayAdapter arrayAdapter;
    private ListView protocolList;
    private SwipeRefreshLayout mListViewContainer;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static QuickModeFragment newInstance(int sectionNumber) {
        QuickModeFragment fragment = new QuickModeFragment();
        mSectionNumber = sectionNumber;
        return fragment;
    }

    public QuickModeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_quick_mode, container, false);

        mListViewContainer = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout_listView);
        onCreateSwipeToRefresh(mListViewContainer);

        dbHelper = DatabaseHelper.getHelper(getActivity());

        // Initialize ListView
        protocolList = (ListView) rootView.findViewById(R.id.lv_protocol);
        showAllProtocolList();

//        if(arrayAdapter.isEmpty())
//        {
//            MainActivity mainActivity = (MainActivity)getActivity();
//            SyncHandler syncHandler = new SyncHandler(mainActivity);
//            syncHandler.DoSync();
//        }

//        final Button showAllProtocolsBtn = (Button) rootView.findViewById(R.id.show_all_protocol_btn);
//        showAllProtocolsBtn.setTypeface(CommonUtils.getInstance(getActivity()).getFontRobotoMedium());
//        showAllProtocolsBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if(showAllProtocolsBtn.getText().equals("Show All Protocols")){
//                    showAllProtocolList();
//                    showAllProtocolsBtn.setText("Show Pre-Selected Protocols");
//                }else if (showAllProtocolsBtn.getText().equals("Show Pre-Selected Protocols")){
//                    showFewProtocolList();
//                    showAllProtocolsBtn.setText("Show All Protocols");
//                }
//            }
//        });

        protocolList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                Protocol protocol = (Protocol) protocolList.getItemAtPosition(position);
                Log.d("GEtting protocol id : ", protocol.getId());
                Intent intent = new Intent(getActivity(),QuickMeasurmentActivity.class);
                intent.putExtra(Protocol.ID, protocol.getId());
                intent.putExtra(DatabaseHelper.C_PROTOCOL_JSON, protocol.getProtocol_json());
                intent.putExtra(Protocol.NAME, protocol.getName());
                intent.putExtra(Protocol.DESCRIPTION, protocol.getDescription());
                try {
                    StringBuffer dataString = new StringBuffer();
                    dataString.append("var protocols={");
                    JSONObject detailProtocolObject = new JSONObject();
                    detailProtocolObject.put("protocolid", protocol.getId());
                    detailProtocolObject.put("protocol_name", protocol.getName());
                    detailProtocolObject.put("macro_id", protocol.getMacroId());
                    dataString.append("\"" + protocol.getId() + "\"" + ":" + detailProtocolObject.toString());
                    dataString.append("}");

                    //	System.out.println("###### writing macros_variable.js :"+dataString);
                    System.out.println("###### writing macros_variable.js :......");
                    CommonUtils.writeStringToFile(getActivity(), "macros_variable.js", dataString.toString());

                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                startActivity(intent);
            }
        });

        return rootView;
    }

    private void showFewProtocolList() {
        List<Protocol> protocols = dbHelper.getFewProtocolList();
        arrayAdapter = new ProtocolArrayAdapter(getActivity(), protocols);
        protocolList.setAdapter(arrayAdapter);
    }

    private void showAllProtocolList() {
        List<Protocol> protocols = dbHelper.getAllProtocolsList();
        arrayAdapter = new ProtocolArrayAdapter(getActivity(), protocols);
        protocolList.setAdapter(arrayAdapter);
    }

    @Override
    public void onResponseReceived(String result) {

        if(result.equals(Constants.SERVER_NOT_ACCESSIBLE)){
            Toast.makeText(getActivity(), R.string.server_not_reachable, Toast.LENGTH_LONG).show();
        }else {
            showFewProtocolList();
            Toast.makeText(getActivity(), "Protocol list up to date", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(mSectionNumber);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private class ProtocolArrayAdapter extends BaseAdapter implements ListAdapter {

        public final Context context;
        public final List<Protocol> protocolList;
        LayoutInflater mInflater;

        public ProtocolArrayAdapter(Context context, List<Protocol> protocolList) {
            assert context != null;
            assert protocolList != null;

            this.protocolList = protocolList;
            this.context = context;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            if (null == protocolList)
                return 0;
            else
                return protocolList.size();
        }

        @Override
        public Protocol getItem(int position) {
            if (null == protocolList)
                return null;
            else
                return protocolList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = mInflater.inflate(R.layout.protocol_list_item, null);

            TextView tvProtocolName = (TextView) convertView.findViewById(R.id.tv_protocol_name);
            tvProtocolName.setTypeface(CommonUtils.getInstance(getActivity()).getFontRobotoRegular());
            Protocol protocol = getItem(position);
            if (null != protocol) {
                try {
                    tvProtocolName.setText(protocol.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            return convertView;
        }
    }
    private void onCreateSwipeToRefresh(SwipeRefreshLayout refreshLayout) {

        refreshLayout.setOnRefreshListener(this);

        refreshLayout.setColorScheme(
                android.R.color.holo_blue_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_green_light,
                android.R.color.holo_red_light);

    }
    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                String authToken = PrefUtils.getFromPrefs(getActivity().getApplicationContext(), PrefUtils.PREFS_AUTH_TOKEN_KEY, PrefUtils.PREFS_DEFAULT_VAL);
                String email = PrefUtils.getFromPrefs(getActivity().getApplicationContext(), PrefUtils.PREFS_LOGIN_USERNAME_KEY, PrefUtils.PREFS_DEFAULT_VAL);
                new DownloadPreSelectedProtocols().execute(getActivity().getApplicationContext(), Constants.PHOTOSYNQ_PRE_SEL_PROTOCOLS_LIST_URL
                        + "&user_email=" + email + "&user_token="
                        + authToken);
            }
        }, 10);
    }

    public class DownloadPreSelectedProtocols extends AsyncTask<Object, Object, String> {
        @Override
        protected String doInBackground(Object... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            Context context = (Context)uri[0];
            HttpResponse response = null;
            HttpGet getRequest;
            String responseString = null;
            if(!CommonUtils.isConnected(context))
            {
                return Constants.SERVER_NOT_ACCESSIBLE;
            }
            Log.d("PHTTPC", "in async task");
            try {
                Log.d("PHTTPC", "$$$$ URI"+uri[1]);
                getRequest = new HttpGet((String) uri[1]);
                Log.d("PHTTPC", "$$$$ Executing GET request");
                response = httpclient.execute(getRequest);

                if (null != response) {
                    try {
                        StatusLine statusLine = response.getStatusLine();
                        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            response.getEntity().writeTo(out);
                            out.close();
                            responseString = out.toString();

                            processResult(responseString);

                        } else {
                            //Closes the connection.
                            response.getEntity().getContent().close();
                            throw new IOException(statusLine.getReasonPhrase());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // Notify swipeRefreshLayout that the refresh has finished
            showAllProtocolList();
            mListViewContainer.setRefreshing(false);

        }

        private void processResult(String result) {
            JSONArray jArray;
            DatabaseHelper db = DatabaseHelper.getHelper(getActivity());
            if (null != result) {
                if(result.equals(Constants.SERVER_NOT_ACCESSIBLE))
                {
                    if(null != getActivity()) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), R.string.server_not_reachable, Toast.LENGTH_LONG).show();
                            }

                        });
                    }
                    return;
                }

                try {
                    JSONObject resultJsonObject = new JSONObject(result);

                    if (resultJsonObject.has("protocols")) {
                        String newobj = resultJsonObject.getString("protocols");
                        jArray = new JSONArray(newobj);
                        for (int i = 0; i < jArray.length(); i++) {

                            JSONObject obj = jArray.getJSONObject(i);
                            String id = obj.getString("id");
                            Protocol protocol = new Protocol(id,
                                    obj.getString("name"),
                                    obj.getString("protocol_json"),
                                    obj.getString("description"),
                                    obj.getString("macro_id"), "slug",
                                    obj.getString("pre_selected"));
                            db.updateProtocol(protocol);

                            String authToken = PrefUtils.getFromPrefs(getActivity().getApplicationContext(), PrefUtils.PREFS_AUTH_TOKEN_KEY, PrefUtils.PREFS_DEFAULT_VAL);
                            String email = PrefUtils.getFromPrefs(getActivity().getApplicationContext(), PrefUtils.PREFS_LOGIN_USERNAME_KEY, PrefUtils.PREFS_DEFAULT_VAL);
                            new DownloadMacros().execute(getActivity().getApplicationContext(), Constants.PHOTOSYNQ_MACRO_URL
                                    +obj.getString("macro_id")+".json"
                                    + "?user_email=" + email + "&user_token="
                                    + authToken);

                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            if(null != getActivity()) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

                            QuickModeFragment fragmentSelectProtocol = (QuickModeFragment) fragmentManager.findFragmentByTag(QuickModeFragment.class.getName());
                            if (fragmentSelectProtocol != null) {
                                fragmentSelectProtocol.onResponseReceived(Constants.SUCCESS);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    }

                });
            }

        }

    }

    public class DownloadMacros extends AsyncTask<Object, Object, String> {
        @Override
        protected String doInBackground(Object... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            Context context = (Context) uri[0];
            HttpResponse response = null;
            HttpGet getRequest;
            String responseString = null;
            if (!CommonUtils.isConnected(context)) {
                return Constants.SERVER_NOT_ACCESSIBLE;
            }
            Log.d("PHTTPC", "in async task");
            try {
                Log.d("PHTTPC", "$$$$ URI" + uri[1]);
                getRequest = new HttpGet((String) uri[1]);
                Log.d("PHTTPC", "$$$$ Executing GET request");
                response = httpclient.execute(getRequest);

                if (null != response) {
                    try {
                        StatusLine statusLine = response.getStatusLine();
                        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            response.getEntity().writeTo(out);
                            out.close();
                            responseString = out.toString();

                            processResult(responseString);

                        } else {
                            //Closes the connection.
                            response.getEntity().getContent().close();
                            throw new IOException(statusLine.getReasonPhrase());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // Notify swipeRefreshLayout that the refresh has finished
            showAllProtocolList();
            mListViewContainer.setRefreshing(false);

        }

        private void processResult(String result) {

            Date date = new Date();
            System.out.println("UpdateMacro Start onResponseReceived: " + date.getTime());

            DatabaseHelper db = DatabaseHelper.getHelper(getActivity());
            JSONArray jArray;

            if (null != result) {
                if (result.equals(Constants.SERVER_NOT_ACCESSIBLE)) {
                    if (null != getActivity()) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), R.string.server_not_reachable, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    return;
                }

                try {
                    JSONObject resultJsonObject = new JSONObject(result);

                    if (resultJsonObject.has("macros")) {
                        String newobj = resultJsonObject.getString("macros");
                        jArray = new JSONArray(newobj);
                        for (int i = 0; i < jArray.length(); i++) {

                            JSONObject obj = jArray.getJSONObject(i);
                            Macro macro = new Macro(obj.getString("id"),
                                    obj.getString("name"),
                                    obj.getString("description"),
                                    obj.getString("default_x_axis"),
                                    obj.getString("default_y_axis"),
                                    obj.getString("javascript_code"),
                                    "slug");
                            db.updateMacro(macro);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}