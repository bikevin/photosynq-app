package com.photosynq.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.ui.widget.Widget;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.model.ProjectResult;
import com.photosynq.app.utils.Constants;
import com.photosynq.app.utils.LocationUtils;
import com.photosynq.app.utils.PrefUtils;
import com.photosynq.app.utils.SyncHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class DisplayResultsActivity extends ActionBarActivity implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        DrawGraphWhenDataProcessedInterface{

    String projectId;
    String reading;
    String protocolJson;
    String appMode;
    private ProgressBar progressBar;

    Button keep;
    Button discard;

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private GoogleApiClient mLocationClient = null;

    ProgressDialog dialog;
    private boolean keepClickFlag = false;
    private boolean isResultSaved = false;

    private ExpandableListView expandableListView;
    private ExpandableGraphListViewAdapter listViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_display_results);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = (ProgressBar) findViewById(R.id.toolbar_progress_bar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_bg));
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle("Result");

        expandableListView = (ExpandableListView) findViewById(R.id.dataList);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            projectId = extras.getString(DatabaseHelper.C_PROJECT_ID);
            reading = extras.getString(DatabaseHelper.C_READING);
            protocolJson = extras.getString(DatabaseHelper.C_PROTOCOL_JSON);
            appMode = extras.getString(Constants.APP_MODE);
            System.out.println(this.getClass().getName() + "############app mode=" + appMode);
        }
        keep = (Button)findViewById(R.id.keep_btn);
        discard = (Button)findViewById(R.id.discard_btn);

        if(appMode.equals(Constants.APP_MODE_QUICK_MEASURE))
        {
            keep.setText("Return");
            keep.setVisibility(View.VISIBLE);
            discard.setVisibility(View.INVISIBLE);
        }

        dataDisplay();

        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();
        //  Set the update interval
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        //-------------------- Start your GPS Reading ------------------ //
        dialog = new ProgressDialog(this);
        dialog.setMessage("Acquiring GPS location");
        dialog.setCancelable(false);

    }

    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {

        super.onStart();

        /*
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         */
        mLocationClient.connect();

        isResultSaved = false;
        keepClickFlag = false;

    }

    private void loadGraphs(DataContainer data){

        listViewAdapter = new ExpandableGraphListViewAdapter(this, data);

        listViewAdapter.setCallback(this);

        expandableListView.setAdapter(listViewAdapter);


    }

    public void myDrawGraphWhenDataProcessedInterface(){
        //callback-currently empty
    }

    private DataContainer dataDisplay(){

        DataContainer adapterData = new DataContainer();
        //TODO REWRITE TO USE CONTAINER CLASS
        org.mozilla.javascript.Context context = org.mozilla.javascript.Context.enter();
        context.setOptimizationLevel(-1);
        ArrayList<ArrayList> dataTotal = new ArrayList<>();
        ArrayList<Object> parentTotal = new ArrayList<>();
        ArrayList<Object> childTotal = new ArrayList<>();
        try{
            String jsData = getStringFromFile(this.getExternalFilesDir(null) + File.separator + "data.js");
            List<String> jsMacros = parseMacros(this.getExternalFilesDir(null) + File.separator + "macros.js");
            String jsMacroVariables = getStringFromFile(this.getExternalFilesDir(null) + File.separator + "macros_variable.js");
            String jsMath = getStringFromFile(this.getExternalFilesDir(null) + File.separator + "math.js");
            String jsInitVariables = getStringFromFile(this.getExternalFilesDir(null) + File.separator + "initalvariables.js");
            String jsJSONObject = "var json = {};";
            String jsStringToArray = "function stringToArray(string) { " +
                    "var array = new Array();" +
                    "string = string.replace(/\\[/g, '');" +
                    "string = string.replace(/\\]/g, '');" +
                    "array = string.split(','); " +
                    "for ( a in array ) { array[a] = parseInt( array[a], 10) };" +
                    "return array };";
            String jsStringToTwoArrays = "function stringToTwoArrays(string) { " +
                    "var array = new Array();" +
                    "var array2 = new Array();" +
                    "var array3 = new Array();" +
                    "var arrayLarge = new Array();" +
                    "var string2 = new String();" +
                    "string = string.replace(/\\[/g, '');" +
                    "string = string.replace(/\\]/g, '');" +
                    "array = string.split(',');" +
                    "var length = array.length; " +
                    "array2 = array.slice(0, length / 2);" +
                    "array3 = array.slice(length / 2, length);" +
                    "for ( a in array2 ) { array2[a] = parseInt( array2[a], 10) };" +
                    "for ( a in array3 ) { array3[a] = parseInt( array3[a], 10) };" +
                    "arrayLarge = [array2, array3];" +
                    "return arrayLarge };";
            String jsRemoveBrackets = "function removeBrackets(string) {" +
                    "string = string.replace(/\\[/g, '');" +
                    "string = string.replace(/\\]/g, '');" +
                    "return string }";
            String jsAll = jsData + jsInitVariables  + jsMacroVariables + ";" + jsMath
                    + jsJSONObject + jsStringToArray + jsStringToTwoArrays + jsRemoveBrackets;
            Scriptable scope = context.initStandardObjects();
            try {
                context.evaluateString(scope, jsAll, "resources", 1, null);
            } catch(EvaluatorException e) {
                Log.e("source name", e.sourceName());
                Log.e("source text", e.lineSource());
            }

            ArrayList<HashMap> data = parseData(scope.get("data", scope));
            HashMap macroIDs = macroIdentifier(scope.get("protocols", scope));

            Function stringToArray = (Function) scope.get("stringToArray", scope);
            Function stringToTwoArrays = (Function) scope.get("stringToTwoArrays", scope);
            Function removeBrackets = (Function) scope.get("removeBrackets", scope);

            ScriptableObject json = (ScriptableObject) scope.get("json", scope);

            for(HashMap map : data) {
                for(Object object : json.getAllIds()){
                    json.delete(object.toString());
                }
                int index = data.indexOf(map);
                for (Object name : data.get(index).keySet()) {
                    json.put((String) name, json, data.get(index).get(name).toString());
                }
                for(Object object : json.getIds()){
                    Object[] dataArgs = {json.get(object).toString()};

                    if(object.toString().contains("get_userdef")){
                        Log.e("array1", "this is working");
                        Object dataArray = stringToTwoArrays.call(context, scope, stringToTwoArrays, dataArgs);
                        ScriptableObject.putProperty(json, object.toString(), dataArray);
                        NativeArray nativeArray = (NativeArray) dataArray;
                        for(Object object1 : nativeArray.toArray()){
                            NativeArray array1 = (NativeArray) object1;
                            for(Object object2 : array1.toArray())
                            Log.e("array1", object2.toString());
                        }
                    }

                    if(object.toString().equals("data_raw") || object.toString().contains("get_userdef")
                            || object.toString().contains("calibration")
                            || object.toString().contains("all_pins")){
                        Object dataArray = stringToArray.call(context, scope, stringToArray, dataArgs);
                        ScriptableObject.putProperty(json, object.toString(), dataArray);
                    }
                    else if(object.toString().equals("get_ir_baseline")){
                        Object dataArray = stringToTwoArrays.call(context, scope, stringToTwoArrays, dataArgs);
                        ScriptableObject.putProperty(json, object.toString(), dataArray);
                        Log.e("ir", dataArray.toString());
                    }else {
                        if(!object.toString().contains("get_userdef")) {
                            Object dataString = removeBrackets.call(context, scope, removeBrackets, dataArgs);
                            ScriptableObject.putProperty(json, object.toString(), dataString);
                        }
                    }
                }

                int protocolId = Integer.valueOf((String) json.get("protocol_id"));
                int macroId = Integer.valueOf((String) macroIDs.get(protocolId));
                String macroName = "macro_" + String.valueOf(macroId);
                Function macro = null;

                for(int i = 0; i < jsMacros.size(); i++) {
                    try {
                    context.evaluateString(scope, jsMacros.get(i), "macroList", 1, null);
                    if(!(scope.get(macroName, scope)).getClass().getSimpleName().equals("UniqueTag")){
                        macro = (Function) scope.get(macroName, scope);
                    }
                } catch (EvaluatorException e){
                        Log.e("warning", "invalid macro: " + macroName);
                    }
                }



                Object[] params = {json};


                List ignoredVariables = new ArrayList();
                ignoredVariables.add("protocol_number");
                ignoredVariables.add("protocol_id");
                ignoredVariables.add("protocol_name");
                ignoredVariables.add("baseline_values");
                ignoredVariables.add("chlorophyll_spad_calibration");
                ignoredVariables.add("averages");
                ignoredVariables.add("data_raw");
                ignoredVariables.add("baseline_sample");
                ignoredVariables.add("HTML");
                ignoredVariables.add("Macro");
                ignoredVariables.add("GraphType");
                ignoredVariables.add("time");
                ignoredVariables.add("time_offset");
                ignoredVariables.add("get_ir_baseline");
                ignoredVariables.add("get_blank_cal");
                ignoredVariables.add("error");
                LinkedHashMap otherDataFields = new LinkedHashMap();
                for(Object object : json.getIds()){
                   if(!ignoredVariables.contains(object.toString())) {
                        otherDataFields.put(object.toString(), json.get(object));
                    }
                }
                try {
                    if(macro != null) {
                        NativeObject result = (NativeObject) macro.call(context, scope, macro, params);
                        if(result == null){
                            Log.e("result", null);
                        }
//                        childTotal.add(result);
                        try {
                            adapterData.setDataOutput(result);
                        }
                        catch (Exception e){
                            Log.e("exception", e.toString());
                            e.printStackTrace();
                        }
                        adapterData.setDataDisplayLeftList(result.get(0, scope));
                        adapterData.setDataDisplayRightList(result.get(1, scope));
//                        displayDataList.add(result.get(0, scope));
//                        displayDataList.add(result.get(1, scope));
                    }

//                    childTotal.add(otherDataFields);
                    adapterData.setDataInput(otherDataFields);
                    adapterData.setDataTrace((NativeArray) json.get("data_raw"));
//                    displayDataList.add(((NativeArray) json.get("data_raw")).toArray());
//                    parentTotal.add(displayDataList);
                } catch (EcmaError e){
                    /*Log.e("warning", "invalid macro: " + macroName);*/
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            org.mozilla.javascript.Context.exit();
        }
//        dataTotal.add(parentTotal);
//        dataTotal.add(childTotal);
        loadGraphs(adapterData);
        return adapterData;
    }

    private HashMap macroIdentifier(Object data){
        ArrayList<Integer> protocolList = new ArrayList<>();
        NativeObject protocols = (NativeObject) data;
        for(Object object : protocols.keySet()){
            protocolList.add((Integer) object);
        }

        HashMap macroMap = new HashMap();
        for(int i : protocolList) {
            macroMap.put(i, ((NativeObject) protocols.get(i)).get("macro_id"));
        }

        return macroMap;
    }

    private ArrayList<HashMap> parseData(Object data){

        Object[] samples= ((NativeArray)
                (((NativeArray) (((NativeObject)
                        (((NativeArray)data).toArray())[0])
                        .get("sample"))).toArray())[0]).toArray();
        ArrayList<NativeObject> objectArrayList = new ArrayList<>();
        for(Object sample : samples) objectArrayList.add((NativeObject) sample);
        ArrayList<HashMap> sampleList = new ArrayList<>();
        for(NativeObject sample : objectArrayList) {
            HashMap map = new HashMap();
            for(Object identifier : sample.keySet()) {
                ArrayList<Object> valueList = new ArrayList<>();
                if(sample.get(identifier).getClass().getSimpleName().equals("NativeArray")){
                    Object[] valueParsed = ((NativeArray) sample.get(identifier)).toArray();
                    if(valueParsed[0].getClass().getSimpleName().equals("NativeArray")){
                        ArrayList<Object> incompleteList;
                        for(Object object : valueParsed){
                            Object[] valueParsedAgain = ((NativeArray) object).toArray();
                            incompleteList = new ArrayList<>(Arrays.asList(valueParsedAgain));
                            valueList.add(incompleteList);
                        }
                    }else {
                        valueList = new ArrayList<>(Arrays.asList(valueParsed));
                    }
                }
                else{
                    valueList = new ArrayList<>();
                    valueList.add(sample.get(identifier));
                }
                map.put(String.valueOf(identifier), valueList);
            }
            sampleList.add(map);
        }

        return sampleList;
    }



    public static List<String> parseMacros(String filepath) throws Exception{
        List<String> macros = new ArrayList<>();
        FileInputStream instream = new FileInputStream(filepath);
        InputStreamReader inread = new InputStreamReader(instream);
        BufferedReader reader = new BufferedReader(inread);
        try {
            StringBuilder stringBuilder = new StringBuilder();
            int counter = 0;
            while (reader.ready()) {
                char macroRead = (char) reader.read();
                stringBuilder.append(macroRead);
                if (macroRead == '{') {
                    counter++;
                } else if (macroRead == '}') {
                    counter--;
                    if (counter == 0) {
                        macros.add(stringBuilder.toString());
                        stringBuilder = new StringBuilder();
                    }
                }
            }
        } finally {
            reader.close();
            inread.close();
            instream.close();
        }
        return macros;
    }

    public String getStringFromFile(String filePath) throws Exception{
        InputStream inputStream = new FileInputStream(filePath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        reader.close();
        inputStream.close();
        return stringBuilder.toString();
    }

    public void keep_click(View view) throws UnsupportedEncodingException, JSONException {

        if(appMode.equals(Constants.APP_MODE_QUICK_MEASURE))
        {
            finish();
        }
        else
        {
            keepClickFlag = true;
            PrefUtils.saveToPrefs(getApplicationContext(), PrefUtils.PREFS_KEEP_BTN_CLICK, "KeepBtnCLickYes");

            if (!reading.contains("location")) {

                String currLocation = getLocation();

                new CountDownTimer(1000, 1000) {
                    public void onTick(long millisUntilFinished) {

                        System.out.print("@@@@@@@@@@@@@@ test tick");

                    }

                    public void onFinish() {
                        String checkLocation = PrefUtils.getFromPrefs(getApplicationContext(), PrefUtils.PREFS_CURRENT_LOCATION, "");
                        if(checkLocation.equals("")) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isFinishing()) {

                                        new AlertDialog.Builder(DisplayResultsActivity.this)
                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                .setMessage("Your location is temporarily not available\n\n" +
                                                        "Check if GPS is turned on.")
                                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int which) {

                                                                dialog.show();
                                                            }

                                                        }

                                                )
                                                .show();

                                    }
                                }
                            });



                        }
                    }
                }.start();



                if(!currLocation.equals(""))

                {

                    saveResult();
                }

            }else{

                saveResult();
            }
        }
    }

    public void discard_click(View view) {
        keepClickFlag = false;
        Toast.makeText(this, R.string.result_discarded, Toast.LENGTH_LONG).show();
        view.setVisibility(View.INVISIBLE);
        keep.setVisibility(View.INVISIBLE);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_display_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                dialog.show();
//                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
//                errorFragment.setDialog(dialog);
//                errorFragment.show(getSupportFragmentManager(), "PHOTOSYNQ-RESULTACTIVITY");
            }
            return false;
        }
    }

    public String getLocation() {

        // If Google Play Services is available
        if (servicesConnected()) {

            // Get the current location
            Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);

            if (currentLocation == null) {

                startLocationUpdates();

            } else{

                String currLocation = LocationUtils.getLatLng(this, currentLocation);

                PrefUtils.saveToPrefs(getApplicationContext(), PrefUtils.PREFS_CURRENT_LOCATION, currLocation);
                dialog.dismiss();
              //  Toast.makeText(DisplayResultsActivity.this, "GPS acquisition complete!", Toast.LENGTH_SHORT).show();

            }

            return LocationUtils.getLatLng(this, currentLocation);
        }
        return "";
    }

    protected void startLocationUpdates () {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mLocationClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mLocationClient, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
       // getLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

        Log.d("PHOTOSYNQ", "Location changed:" + LocationUtils.getLatLng(this, location));
        PrefUtils.saveToPrefs(getApplicationContext(), PrefUtils.PREFS_CURRENT_LOCATION, LocationUtils.getLatLng(this, location));

        if(dialog.isShowing()) {
            dialog.dismiss();
            Toast.makeText(DisplayResultsActivity.this, "GPS acquisition complete!", Toast.LENGTH_SHORT).show();
        }
      //  stopLocationUpdates();

        if(null != reading && !reading.isEmpty() && keepClickFlag) {
            saveResult();
            reading = "";
            keepClickFlag = false;
        }
    }

    private void saveResult(){

        if(isResultSaved == false) {
            isResultSaved = true;

            int index = Integer.parseInt(PrefUtils.getFromPrefs(this, PrefUtils.PREFS_QUESTION_INDEX, "1"));
            PrefUtils.saveToPrefs(this, PrefUtils.PREFS_QUESTION_INDEX, "" + (index + 1));

            if (!reading.contains("location")) {

                String currentLocation = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_CURRENT_LOCATION, "");
                reading = reading.replaceFirst("\\{", "{\"location\":[" + currentLocation + "],");
            } else {
                String currentLocation = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_CURRENT_LOCATION, "");
                String locationStr = "\"location\":[";
                int locationIdx = reading.indexOf(locationStr);
                String tempReading = reading.substring(0, locationIdx + locationStr.length());
                tempReading += currentLocation;
                tempReading += reading.substring(reading.indexOf("]", locationIdx));

                reading = tempReading;
                //reading = reading.replaceFirst("\"location\":", "{\"location\":[" + currentLocation + "],");
            }


            // Reading store into database if is in correct format (correct json format), Otherwise we discard reading.
            if (isJSONValid(reading)) {

                Log.d("IsJSONValid", "Valid Json");
                DatabaseHelper databaseHelper = DatabaseHelper.getHelper(this);
                ProjectResult result = new ProjectResult(projectId, reading, "N");
                databaseHelper.createResult(result);

            } else {
                Log.d("IsJSONValid", "Invalid Json");
                Toast.makeText(getApplicationContext(), "Invalid Json", Toast.LENGTH_SHORT).show();
            }

            SyncHandler syncHandler = new SyncHandler(this, MainActivity.getProgressBar());
            syncHandler.DoSync(SyncHandler.UPLOAD_RESULTS_MODE);

            finish();

        }
    }

    public boolean isJSONValid(String jsonStr) {
        try {
            new JSONObject(jsonStr);
        } catch (JSONException ex) {
            try {
                new JSONArray(jsonStr);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {

                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {

                // Log the error
                e.printStackTrace();
            }
        } else {

            // If no resolution is available, display a dialog to the user with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this,
                LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            errorDialog.show();
        }
    }
}

class ExpandableGraphListViewAdapter extends BaseExpandableListAdapter{

    private Context context;
    private DataContainer dataContainer;
    private DrawGraphWhenDataProcessedInterface dataProcessedInterface;


    public ExpandableGraphListViewAdapter(Context context, DataContainer dataContainer){
        this.context = context;
        this.dataContainer = dataContainer;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition){
        int size = dataContainer.getDataOutput().get(groupPosition).size();

        Object[] keySetOutput = dataContainer.getDataOutput().get(groupPosition).keySet().toArray();
        Object[] keySetInput = dataContainer.getDataInput().get(groupPosition).keySet().toArray();

        if (childPosition < size){
            return dataContainer.getDataOutput().get(groupPosition).get(keySetOutput[childPosition]);
        }
        else{
            return dataContainer.getDataInput().get(groupPosition).get(keySetInput[childPosition - size]);
        }
    }

    @Override
    public long getChildId(int groupPosition, int childPosition){
        return childPosition;
    }

    public void setCallback(DrawGraphWhenDataProcessedInterface dataProcessedInterface){
        this.dataProcessedInterface = dataProcessedInterface;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent){
        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.custom_child_data, null);
        }

        Object[] keySetOutput = dataContainer.getDataOutput().get(groupPosition).keySet().toArray();
        Object[] keySetInput = dataContainer.getDataInput().get(groupPosition).keySet().toArray();

        Log.e("output length", String.valueOf(keySetOutput.length));
        Log.e("input length", String.valueOf(keySetInput.length));

        int lengthOutput = dataContainer.getDataOutput().get(groupPosition).size();

        int lengthInput = dataContainer.getDataInput().get(groupPosition).size();

        int length;

        if(lengthInput % 2 == 1 && lengthOutput % 2 == 1) {
            length = lengthInput + lengthOutput - 2;
        }else {
            length = (lengthInput + lengthOutput - 1);
        }

        TextView textView1 = (TextView) convertView.findViewById(R.id.childText1);

        TextView textView2 = (TextView) convertView.findViewById(R.id.childText2);

        List outerContainer = new ArrayList();
        List innerContainer = new ArrayList();
        if(keySetOutput.length > childPosition * 2 + 1) {
                textView1.setBackgroundColor(Color.WHITE);
                textView2.setBackgroundColor(Color.WHITE);

            textView1.setText(String.valueOf(keySetOutput[childPosition * 2])
                    + ": "
                    + String.valueOf(dataContainer.getDataOutput().get(groupPosition).get(keySetOutput[childPosition * 2])));

            textView2.setText(String.valueOf(keySetOutput[childPosition * 2 + 1])
                    + ": "
                    + String.valueOf(dataContainer.getDataOutput().get(groupPosition).get(keySetOutput[childPosition * 2 + 1])));

        }
        else{
            textView1.setBackgroundColor(Color.YELLOW);
            textView2.setBackgroundColor(Color.YELLOW);
            if(keySetInput.length > childPosition * 2 + 2 - lengthOutput) {
                if(dataContainer.getDataInput().get(groupPosition).get(keySetInput[childPosition * 2 + 1 - lengthOutput]).getClass().getSimpleName().equals("NativeArray")){
                    NativeArray nativeArray = (NativeArray) dataContainer.getDataInput().get(groupPosition).get(keySetInput[childPosition * 2 + 1 - lengthOutput]);
                    Object[] array = nativeArray.toArray();
                    for(Object object : array){
                        innerContainer.clear();
                        if(object.getClass().getSimpleName().equals("NativeArray")) {
                            NativeArray nativeArray1 = (NativeArray) object;
                            for (Object object1 : nativeArray1.toArray()) {
                                innerContainer.add(object1.toString());
                            }
                            outerContainer.add(innerContainer);
                        }
                        else{
                            outerContainer.add(object);
                        }
                    }
                    textView1.setText(String.valueOf(keySetInput[childPosition * 2 + 1 - lengthOutput])
                            + ": "
                            + String.valueOf(outerContainer));
                }
                else{
                    textView1.setText(String.valueOf(keySetInput[childPosition * 2 + 1  - lengthOutput])
                            + ": "
                            + String.valueOf(dataContainer.getDataInput().get(groupPosition).get(keySetInput[childPosition * 2 + 1 - lengthOutput])));
                }

                if(dataContainer.getDataInput().get(groupPosition).get(keySetInput[childPosition * 2 + 2 - lengthOutput]).getClass().getSimpleName().equals("NativeArray")){
                    NativeArray nativeArray = (NativeArray) dataContainer.getDataInput().get(groupPosition).get(keySetInput[childPosition * 2 + 2  - lengthOutput]);
                    Object[] array = nativeArray.toArray();
                    for(Object object : array){
                        innerContainer.clear();
                        if(object.getClass().getSimpleName().equals("NativeArray")) {
                            NativeArray nativeArray1 = (NativeArray) object;
                            for (Object object1 : nativeArray1.toArray()) {
                                innerContainer.add(object1.toString());
                            }
                            outerContainer.add(innerContainer);
                        }
                        else{
                            outerContainer.add(object);
                        }
                    }
                    textView2.setText(String.valueOf(keySetInput[childPosition * 2 + 2  - lengthOutput])
                            + ": "
                            + String.valueOf(outerContainer));
                }
                else {
                    textView2.setText(String.valueOf(keySetInput[childPosition * 2 + 2 - lengthOutput])
                            + ": "
                            + String.valueOf(dataContainer.getDataInput().get(groupPosition).get(keySetInput[childPosition * 2 + 2 - lengthOutput])));
                }


            }

        }
        if (childPosition > length / 2){
            textView1.setVisibility(View.GONE);
            textView2.setVisibility(View.GONE);
        }

        if (childPosition <= length / 2){
            textView1.setVisibility(View.VISIBLE);
            textView2.setVisibility(View.VISIBLE);
        }
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition){

        int lengthOutput = dataContainer.getDataOutput().get(groupPosition).size();

        int lengthInput = dataContainer.getDataInput().get(groupPosition).size();

        Log.e("length", String.valueOf(lengthInput));

        return lengthOutput + lengthInput;
    }

    @Override
    public Object getGroup(int groupPosition){
        return dataContainer;
    }

    @Override
    public long getGroupId(int groupPosition){
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent) {

        ArrayList<Double> dataRaw = new ArrayList<>();

        for(Object object : dataContainer.getDataTrace().get(groupPosition)){
            double dataDouble = Double.valueOf(String.valueOf(object));
            dataRaw.add(dataDouble);
        }

        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.custom_header_data, null);
        }

        TextView textViewLeft = (TextView) convertView.findViewById(R.id.dataText1);
        textViewLeft.setText(dataContainer.getDataOutput().get(groupPosition).keySet().toArray()[0] + ":\n"
                + dataContainer.getDataOutput().get(groupPosition).get(dataContainer.getDataOutput().get(groupPosition).keySet().toArray()[0]));

        TextView textViewRight = (TextView) convertView.findViewById(R.id.dataText2);
        textViewRight.setText(dataContainer.getDataOutput().get(groupPosition).keySet().toArray()[1] + ":\n"
                + dataContainer.getDataOutput().get(groupPosition).get(dataContainer.getDataOutput().get(groupPosition).keySet().toArray()[1]));

        dataProcessedInterface.myDrawGraphWhenDataProcessedInterface();

        XYPlot xyPlot = (XYPlot) convertView.findViewById(R.id.dataPlot);

        XYSeries dataSeries = new SimpleXYSeries(dataRaw,
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
                "");

        LineAndPointFormatter dataSeriesFormatter = new LineAndPointFormatter();
        dataSeriesFormatter.setPointLabelFormatter(new PointLabelFormatter());
        dataSeriesFormatter.configure(this.context, R.xml.line_point_formatter);

        xyPlot.addSeries(dataSeries, dataSeriesFormatter);
        xyPlot.setTicksPerRangeLabel(3);
        xyPlot.setPlotMargins(0, 0, 0, 0);
        xyPlot.setGridPadding(0, 0, 0, 0);
        xyPlot.setPlotPadding(0, 0, 0, 0);
        xyPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.LTGRAY);
        xyPlot.setDomainLabelWidget(null);
        xyPlot.setRangeLabelWidget(null);
        xyPlot.getGraphWidget().setRangeLabelPaint(null);
        xyPlot.getGraphWidget().setDomainLabelPaint(null);
        xyPlot.getGraphWidget().setDomainOriginLabelPaint(null);
        xyPlot.getGraphWidget().setRangeLabelPaint(null);
        xyPlot.getGraphWidget().setBackgroundPaint(null);
        xyPlot.getGraphWidget().setGridPadding(0, 0, 0, 0);
        xyPlot.getGraphWidget().setMargins(-40, 0, 0, -15);
        xyPlot.getGraphWidget().getDomainGridLinePaint().setColor(Color.TRANSPARENT);
        xyPlot.getGraphWidget().getRangeGridLinePaint().setColor(Color.TRANSPARENT);
        xyPlot.getGraphWidget().getRangeSubGridLinePaint().setColor(Color.TRANSPARENT);
        xyPlot.getGraphWidget().getDomainSubGridLinePaint().setColor(Color.TRANSPARENT);
        xyPlot.getGraphWidget().setSize(new SizeMetrics(0, SizeLayoutType.FILL, 0, SizeLayoutType.FILL));
        xyPlot.getGraphWidget().position(-0.5f, XLayoutStyle.ABSOLUTE_FROM_LEFT, -0.5f, YLayoutStyle.ABSOLUTE_FROM_TOP);
        xyPlot.getLayoutManager().remove(xyPlot.getDomainLabelWidget());
        xyPlot.getLayoutManager().remove(xyPlot.getRangeLabelWidget());
        xyPlot.getLayoutManager().remove(xyPlot.getLegendWidget());

        return convertView;
    }

    @Override
    public int getGroupCount(){
        return dataContainer.getDataOutput().size();
    }

    @Override
    public boolean hasStableIds(){
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition){
        return true;
    }

}

interface DrawGraphWhenDataProcessedInterface{
    public void myDrawGraphWhenDataProcessedInterface();
}

class DataContainer{
    public List<Object> dataDisplayLeftList = new LinkedList<>();
    public List<Object> dataDisplayRightList = new LinkedList<>();

    public List<NativeObject> dataOutput = new LinkedList<>();

    public List<LinkedHashMap> dataInput = new LinkedList<>();

    public List<NativeArray> dataTrace = new LinkedList<>();

    public DataContainer(){
        dataDisplayLeftList = new ArrayList<>();
        dataDisplayRightList = new ArrayList<>();
    }

    public void setDataDisplayLeftList(Object object){
        dataDisplayLeftList.add(object);
    }

    public void setDataDisplayRightList(Object object){
        dataDisplayRightList.add(object);
    }

    public void setDataOutput(NativeObject output){
        dataOutput.add(output);
    }

    public void setDataInput(LinkedHashMap input){
        dataInput.add(input);
    }

    public void setDataTrace(NativeArray data){
        dataTrace.add(data);
    }

    public List<Object> getDataDisplayLeftList(){
        return dataDisplayLeftList;
    }

    public List<Object> getDataDisplayRightList(){
        return dataDisplayRightList;
    }

    public List<NativeObject> getDataOutput(){
        return dataOutput;
    }

    public List<LinkedHashMap> getDataInput(){
        return dataInput;
    }

    public List<NativeArray> getDataTrace(){
        return dataTrace;
    }

}
