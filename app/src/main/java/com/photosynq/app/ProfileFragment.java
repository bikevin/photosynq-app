package com.photosynq.app;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.photosynq.app.model.ResearchProject;
import com.photosynq.app.utils.CommonUtils;
import com.photosynq.app.utils.Constants;
import com.photosynq.app.utils.PrefUtils;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;


public class ProfileFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private ProgressDialog progress;
    TextView tvLoggedUser;
    TextView tvInstituteName;
    TextView tvContact;
    TextView tvProjects;
    TextView tvContributions;
    TextView tvBadges;
    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static ProfileFragment newInstance(int sectionNumber) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile, container, false);

        final Context context = getActivity();
        String imageUrl = PrefUtils.getFromPrefs(context, PrefUtils.PREFS_THUMB_URL_KEY, PrefUtils.PREFS_DEFAULT_VAL);
        ImageView profileImage = (ImageView) rootView.findViewById(R.id.user_profile_image);
        Picasso.with(context)
                .load(imageUrl)
                .error(R.drawable.ic_launcher)
                .into(profileImage);

        tvLoggedUser = (TextView) rootView.findViewById(R.id.user_name);
        tvInstituteName = (TextView) rootView.findViewById(R.id.institute_name);
        tvContact = (TextView) rootView.findViewById(R.id.tv_contact);
        tvProjects = (TextView) rootView.findViewById(R.id.tv_projects);
        tvContributions = (TextView) rootView.findViewById(R.id.tv_contrb);
        tvBadges = (TextView) rootView.findViewById(R.id.tv_badges);


        Button signOut = (Button) rootView.findViewById(R.id.sign_out_btn);
        signOut.setTypeface(CommonUtils.getInstance(context).getFontRobotoMedium());
        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.clear();
                editor.commit();
                Intent intent = new Intent(context, LoginActivity.class);
                intent.putExtra("change_user", true);
                startActivity(intent);
                getActivity().finish();
            }
        });

        String authToken = PrefUtils.getFromPrefs(getActivity().getApplicationContext(), PrefUtils.PREFS_AUTH_TOKEN_KEY, PrefUtils.PREFS_DEFAULT_VAL);
        String email = PrefUtils.getFromPrefs(getActivity().getApplicationContext(), PrefUtils.PREFS_LOGIN_USERNAME_KEY, PrefUtils.PREFS_DEFAULT_VAL);

        new DownloadProfile().execute(getActivity().getApplicationContext(), Constants.PHOTOSYNQ_USER_INFO
                + "&user_email=" + email + "&user_token="
                + authToken);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public class DownloadProfile extends AsyncTask<Object, Object, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = ProgressDialog.show(getActivity(), "Please wait . . .", "Updating profile", true);
        }

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

                            //processResult(context, responseString);

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
            processResult(result);
            progress.dismiss();

        }

        private void processResult(String result) {

            JSONObject userInfoJsonObject;

            if (null != result) {

                try {
                    JSONObject resultJsonObject = new JSONObject(result);
                    if (resultJsonObject.getString("status").equals("success")) {
                        userInfoJsonObject = resultJsonObject.getJSONObject("user");

                        tvLoggedUser.setText(userInfoJsonObject.getString("name"));
                        tvInstituteName.setText(userInfoJsonObject.getString("institute"));
                        tvContact.setText(userInfoJsonObject.getString("email"));
                        tvProjects.setText(userInfoJsonObject.getString("projects"));
                        tvContributions.setText(userInfoJsonObject.getString("contributions"));
                        tvBadges.setText(Integer.toString(userInfoJsonObject.getJSONArray("badges").length()));

//                        tvLoggedUser.setTypeface(CommonUtils.getInstance(context).getFontRobotoRegular());
//                        tvInstituteName.setTypeface(CommonUtils.getInstance(context).getFontRobotoRegular());
//                        tvContact.setTypeface(CommonUtils.getInstance(context).getFontRobotoRegular());


                    }
                    } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Date date1 = new Date();
            System.out.println("Profile fetch onResponseReceived: " + date1.getTime());
        }

    }
}