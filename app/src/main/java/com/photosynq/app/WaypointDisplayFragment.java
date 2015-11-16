package com.photosynq.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import com.google.android.gms.location.LocationListener;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.photosynq.app.db.LocationDatabaseHelper;
import com.photosynq.app.utils.CommonUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

/*
 * Created by Kevin on 8/7/2015.
 */
public class WaypointDisplayFragment extends Fragment implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, PopupMenu.OnMenuItemClickListener{
    private static int mSectionNumber;

    private Location mCurrentLocation;

    private Location mDestinationLocation;

    private GoogleApiClient mGoogleApiClient;

    private LocationDatabaseHelper mDatabaseHelper;

    private LocationRequest mLocationRequest;

    private MapView mapView;

    private GoogleMap map;

    private Cursor cursor;

    private HashMap<Marker, Integer> mMarkerHashMap;

    private TextView mDistanceText;

    private Button wayBtnNext;

    private MyGLSurfaceView mGLSurfaceView;

    static final int REQUEST_IMAGE_FROM_GALLERY = 3;

    private static boolean SHOW_DISTANCE = false;

    public static WaypointDisplayFragment newInstance(int sectionNumber) {
        WaypointDisplayFragment fragment = new WaypointDisplayFragment();
        mSectionNumber = sectionNumber;
        return fragment;
    }

    public WaypointDisplayFragment(){
        //required public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //start up stuff
        mDatabaseHelper = LocationDatabaseHelper.getHelper(getActivity());
        mMarkerHashMap = new HashMap<>();
        mDestinationLocation = new Location("blank");
        mCurrentLocation = mDestinationLocation;
        createLocationRequest();
        buildGoogleApiClient();

        //make the arrow disappear when navigation drawer opens and reappear when it closes
        DrawerLayout drawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        DrawerLayout.DrawerListener listener = new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                mGLSurfaceView.setVisibility(View.GONE);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                mGLSurfaceView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        };
        if(drawerLayout != null) drawerLayout.setDrawerListener(listener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        final View rootView = inflater.inflate(R.layout.fragment_waypoint_display, container, false);

        //instantiate the arrow view
        LinearLayout mGLLayout = (LinearLayout) rootView.findViewById(R.id.arrow_layout);
        mGLSurfaceView = new MyGLSurfaceView(this.getActivity());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mGLLayout.addView(mGLSurfaceView, layoutParams);


        //set up the set/edit waypoint button
        Button wayBtnPopup = (Button) rootView.findViewById(R.id.btn_wypnt_popup);
        wayBtnPopup.setTypeface(CommonUtils.getInstance(getActivity()).getFontRobotoMedium());
        wayBtnPopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });

        //set up the next waypoint button
        wayBtnNext = (Button) rootView.findViewById(R.id.btn_wypnt_next);
        wayBtnNext.setTypeface(CommonUtils.getInstance(getActivity()).getFontRobotoMedium());
        wayBtnNext.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        wayBtnNext.setEnabled(false);
        wayBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //retrieves current waypoint from database
                double latitude_orig = mDestinationLocation.getLatitude();
                int id = getId(latitude_orig);
                id = id + 1;
                double latitude = getLatitude(id);
                double longitude = getLongitude(id);
                //checks if last waypoint, if so, moves to first waypoint
                if (latitude != -1) {
                    getDistance(latitude,longitude);
                }
                else {
                    id = 1;
                    getDistance(getLatitude(id), getLongitude(id));
                }
                setNextEnabled();

            }
        });


        //find the mapview, set up the map
        mapView = (MapView) rootView.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        map = mapView.getMap();
        //use the custom map marker
        map.setInfoWindowAdapter(new CustomMapMarkerAdapter(this.getActivity()));
        map.setMyLocationEnabled(true);
        MapsInitializer.initialize(this.getActivity());

        //passes a position to the adapter so it can display an image
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //pass waypoint ID to the marker
                CustomMapMarkerAdapter.setPosition(mMarkerHashMap.get(marker));

                //show distance
                setShowDistanceTrue();
                //find distance to that waypoint
                mDestinationLocation.setLatitude(marker.getPosition().latitude);
                mDestinationLocation.setLongitude(marker.getPosition().longitude);
                if(SHOW_DISTANCE) mDistanceText.setText(String.valueOf(mCurrentLocation.distanceTo(mDestinationLocation)));

                //point arrow towards waypoint
                moveArrow(mCurrentLocation, mDestinationLocation);

                //update next button behavior
                setNextEnabled();
                //false causes all default marker activity to happen
                return false;
            }
        });

        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                //request full image from the gallery for viewing
                String largeFilePath = getLargeFilePath(mMarkerHashMap.get(marker));
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + largeFilePath), "image/*");
                getActivity().startActivityForResult(intent, REQUEST_IMAGE_FROM_GALLERY);

            }
        });

        return rootView;
    }

    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        //find textview, set to blank
        mDistanceText = (TextView) getActivity().findViewById(R.id.distance_to_text);
        mDistanceText.setText("");
        setShowDistanceFalse();
    }

    //populate hashmap from database, add markers to map from database
    private void populateHashMap(){
            for(int l = 1; l <= getAllLocations().size(); l++){
                String name = getName(l);
                LatLng latLng = new LatLng(getLatitude(l), getLongitude(l));
                Marker marker = map.addMarker(new MarkerOptions().position(latLng).title(name));
                mMarkerHashMap.put(marker, l);
            }
    }

    //rather self-explanatory
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this.getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    //inflate set/edit menu
    public void showPopup(View view){
        PopupMenu menu = new PopupMenu(getActivity(), view);
        MenuInflater inflater = menu.getMenuInflater();
        inflater.inflate(R.menu.menu_waypoint_popup, menu.getMenu());
        menu.setOnMenuItemClickListener(this);
        menu.show();
    }

    //define set/edit menu actions
    @Override
    public boolean onMenuItemClick(MenuItem item){
        Fragment newFragment;
        FragmentTransaction transaction;
        switch (item.getItemId()){
            case R.id.set_waypoint:

                //move to set waypoint fragment
                newFragment = new WaypointSetFragment();
                transaction = getFragmentManager().beginTransaction();

                transaction.replace(R.id.container, newFragment);
                transaction.addToBackStack(null);

                transaction.commit();
                return true;

            case R.id.edit_waypoint:

                //move to edit waypoint fragment
                newFragment = new WaypointEditFragment();
                transaction = getFragmentManager().beginTransaction();

                transaction.replace(R.id.container, newFragment);
                transaction.addToBackStack(null);

                transaction.commit();
                return true;

            default:

                return false;
        }
    }

    //update behavior of the next button- only clickable within 5 meters
    private void setNextEnabled(){
        if(mCurrentLocation.distanceTo(mDestinationLocation) < 5){
            if(wayBtnNext != null){
                wayBtnNext.setEnabled(true);
                wayBtnNext.getBackground().clearColorFilter();
            }
        }
        else{
            if(wayBtnNext != null){
                wayBtnNext.setEnabled(false);
                wayBtnNext.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            }
        }
    }

    //helper method that moves map camera to defined lat/long/zoom level
    protected void cameraUpdater(double latitude, double longitude, int zoom) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), zoom);
        map.animateCamera(cameraUpdate);
    }

    //helper method that creates a location request
    protected void createLocationRequest() {
        mLocationRequest = LocationRequest.create()
                .setInterval(0).setFastestInterval(0)
                .setSmallestDisplacement(0)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    //helper method that starts location updates
    protected void startLocationUpdates(){
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    //defines what happens on location change
    @Override
    public void onLocationChanged(Location location){
        mCurrentLocation = location;
        //point arrow correctly
        if (mDestinationLocation.getLatitude() != 0 && mDestinationLocation.getLongitude() != 0)
            moveArrow(mCurrentLocation, mDestinationLocation);
        if(SHOW_DISTANCE) {
            if (mDistanceText != null) {
                //set distance text
                double distance = mCurrentLocation.distanceTo(mDestinationLocation);
                mDistanceText.setText(String.valueOf(distance));
            }
        }

        //update next button behavior
        setNextEnabled();
    }

    //update current location when connected to Google Play Services
    @Override
    public void onConnected(Bundle connectionHint){
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(2 == map.getCameraPosition().zoom) {
            //TODO try to fix this - stop moving the map
            cameraUpdater(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), 10);
        }

        startLocationUpdates();
    }

    //interface method TODO add a toast
    @Override
    public void onConnectionFailed(ConnectionResult result){}

    //interface method TODO add a toast
    @Override
    public void onConnectionSuspended(int cause){}

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        populateHashMap();
    }

    @Override
    public void onStop(){
        super.onStop();
        if(mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
    }

    //helper method that iterates through the database and reads all the lat/lngs and names into an arraylist
    protected List<String> getAllLocations(){

        List<String> locations = new ArrayList<>();

        cursor = mDatabaseHelper.getAllWaypoints();
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            String location = cursorToStringName(cursor);
            locations.add(location);
            cursor.moveToNext();
        }
        return locations;
    }

    //helper method that gets distance from current location to lat/lng
    protected void getDistance(double latitude, double longitude){
        mDestinationLocation = latLongToLocation(latitude, longitude);
        double distance = mCurrentLocation.distanceTo(mDestinationLocation);
        moveArrow(mCurrentLocation, mDestinationLocation);
        if(SHOW_DISTANCE) {
            if (mDistanceText != null) {
                mDistanceText.setText(String.valueOf(distance));
            }
        }
    }

    //helper method that moves arrow
    private void moveArrow(Location currentLocation, Location targetLocation){
        mGLSurfaceView.setAngle(currentLocation.bearingTo(targetLocation));
    }

    //helper method that converts a lat and a long to a Location object
    private Location latLongToLocation(double latitude, double longitude){

        //set a blank provider
        Location location = new Location("blank");
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        return location;
    }

    protected int getId(double latitude) {
        cursor = mDatabaseHelper.getOneWaypointId(latitude);
        if(cursor.moveToFirst()) {
            return cursorToInt(cursor);
        }
        else{
            return -1;
        }
    }

    //gets latitude for waypoint in specified location
    protected double getLatitude(int position){
        cursor = mDatabaseHelper.getOneWaypointLat(position);
        if(cursor.moveToFirst()) {
            return cursorToDouble(cursor, 0);
        }
        else{
            return -1;
        }
    }

    //gets longitude for waypoint in specified location
    protected double getLongitude(int position){
        cursor = mDatabaseHelper.getOneWaypointLong(position);
        if(cursor.moveToFirst()) {
            return cursorToDouble(cursor, 0);
        }
        else{
            return -1;
        }
    }

    //gets the user-inputted name for waypoint in specified location
    protected String getName(int position) {
        cursor = mDatabaseHelper.getOneWaypointName(position);
        if(cursor.moveToFirst()) {
            return cursorToString(cursor);
        }
        else{
            return "empty";
        }
    }

    //gets the file path for the full image
    private String getLargeFilePath(int Id){
        Cursor cursor = mDatabaseHelper.getOneWaypointLargeFilePath(Id);
        cursor.moveToFirst();
        return cursorToString(cursor);
    }


    //pulls the waypoint name
    private String cursorToStringName(Cursor cursor){
        return cursor.getString(1);
    }

    //pulls one waypoint data column as a string
    private String cursorToString(Cursor cursor) {
        return cursor.getString(0);
    }

    //pulls one waypoint data column as a double
    private double cursorToDouble(Cursor cursor, int integer) {
        return cursor.getDouble(integer);
    }

    //pulls one waypoint data column as an int
    private int cursorToInt(Cursor cursor) {
        return cursor.getInt(0);
    }

    private void setShowDistanceTrue(){
        SHOW_DISTANCE = true;
    }

    private void setShowDistanceFalse(){
        SHOW_DISTANCE = false;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(mSectionNumber);
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
        setShowDistanceFalse();
        mGLSurfaceView.startSensors();
        mGLSurfaceView.setAngle(0);
        mGLSurfaceView.onResume();
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        cursor.close();
    }

    @Override
    public void onPause() {
        super.onPause();
        mGLSurfaceView.stopSensors();
        mGLSurfaceView.onPause();
    }
}

//Required OpenGL ES 2.0 class
class MyGLSurfaceView extends GLSurfaceView implements SensorEventListener{
    private SensorManager mSensorManager;
    public static double mAngle;
    public static volatile float[] quat = {1, 0, 0, 0};

    //Constructor- to directly inflate, add AttributeSet to the parameters
    public MyGLSurfaceView(Context context){
        super(context);

        setEGLContextClientVersion(2);

        //required to show map behind GLSurfaceView and enable multisampling
        setEGLConfigChooser(new MultisampleConfigChooser());


        //instantiate and set renderer
        MyGLRenderer mRenderer = new MyGLRenderer();

        setRenderer(mRenderer);

        //required to show map behind GLSurfaceView
        getHolder().setFormat(PixelFormat.TRANSPARENT);

        //only redraw when requestRender() is called
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        //required to show map behind GLSurfaceView
        setZOrderOnTop(true);

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getQuaternionFromVector(quat, event.values);
            requestRender();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    //setter and getter methods
    public void setAngle(double angle){
        mAngle = angle;
    }

    public static double getAngle(){
        return mAngle;
    }

    public void startSensors() {
        List<Sensor> listSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        if(listSensors.size() > 0) {
            for(int i = 0; i < listSensors.size(); i++) {
                Sensor itemSensor= listSensors.get(i);

                //find the rotation sensor
                switch(itemSensor.getType()){

                    case Sensor.TYPE_ROTATION_VECTOR:
                        //set polling frequency to 15Hz
                        mSensorManager.registerListener(this, itemSensor, SensorManager.SENSOR_DELAY_GAME);
                        break;

                    default:
                        break;
                }
            }
        }
    }

    public void stopSensors() {
        mSensorManager.unregisterListener(this);
    }
}

//another required OpenGL class
class MyGLRenderer implements GLSurfaceView.Renderer {

    private Arrow mArrow;
    private ArrowBorder mArrowBorder;

    //MVP = Model View Projection
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mRotationMatrix = new float[16];
    private final float[] mBearingMatrix = new float[16];

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        //set background to clear
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);


        mArrow = new Arrow();
        mArrowBorder = new ArrowBorder();
    }

    @Override
    public void onDrawFrame(GL10 unused){


        float[] scratch = new float[16];
        float[] yetAnotherMatrix = new float[16];

        float[] quat = MyGLSurfaceView.quat;

        double bearing = MyGLSurfaceView.getAngle();

        //clear color and depth buffers
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        //set matricies that define camera location - if param 4 > -4 arrow starts clipping on rotation
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -4, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        //multiply matricies
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        //create another rotation matrix from quaternion output
        Matrix.setRotateM(mRotationMatrix, 0,
                (float) (2.0f * Math.acos(quat[0]) * 180.0f / Math.PI),
                quat[1], -quat[2], quat[3]);

        //create yet another rotation matrix from bearing to target location
        Matrix.setRotateM(mBearingMatrix, 0, (float) bearing, 0, 0, 1);

        //multiply more matricies together
        Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);

        Matrix.multiplyMM(yetAnotherMatrix, 0, scratch, 0, mBearingMatrix, 0);

        //draw arrow and border with orientation from matricies
        mArrow.draw(yetAnotherMatrix);
        mArrowBorder.draw(yetAnotherMatrix);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {

        //set viewport and frustum
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        android.opengl.Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    //helper method that loads the shaders
    public static int loadShader(int type, String shaderCode) {

        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    //helper method that throws exceptions when OpenGL creates errors
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("render", glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }
}

//class that defines the Arrow object
class Arrow {

    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;

    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    //each vertex is defined by 3 coordinates b/c 3d
    static final int COORDS_PER_VERTEX = 3;

    //list of vertices the create the arrow
    static float arrowCoords[] = {
            0.0f, 0.5f, 0.125f,
            0.0f, 0.5f, -0.125f,
            0.25f, 0.125f, 0.125f,
            0.25f, 0.125f, -0.125f,
            0.125f, 0.125f, 0.125f,
            0.125f, 0.125f, -0.125f,
            0.125f, -0.5f, 0.125f,
            0.125f, -0.5f, -0.125f,
            -0.125f, -0.5f, 0.125f,
            -0.125f, -0.5f, -0.125f,
            -0.125f, 0.125f, 0.125f,
            -0.125f, 0.125f, -0.125f,
            -0.25f, 0.125f, 0.125f,
            -0.25f, 0.125f, -0.125f
    };

    //order in which to draw the arrow by triangles
    //orientation: arrow pointing up
    private short drawOrder[] = {
            //front face
            2, 0, 12, 4, 8, 6, 4, 10, 8,
            //back face
            13, 1, 3, 11, 7, 9, 11, 5, 7,
            //top faces
            0, 3, 1, 0, 2, 3,
            13, 0, 1, 13, 12, 0,
            //bottom faces
            5, 3, 2, 5, 2, 4,
            11, 10, 12, 11, 12, 13,
            7, 6, 8, 7, 8, 9,
            //right face
            5, 4, 6, 5, 6, 7,
            //left face
            10, 11, 9, 10, 9, 8
    };

    //RGBA - currently translucent red
    float color[] = { 1.0f, 0.0f, 0.0f, 0.5f };

    //shader code
    private final String vertexShaderCode =
            "uniform mat4 mMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = mMVPMatrix * vPosition;" +
                "}";

    //shader code
    private final String fragmentShaderCode =
            "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}";

    private final int mProgram;

    private final int vertexStride = COORDS_PER_VERTEX * 4;

    public Arrow() {

        //load the shaders- these ints hold references to them
        int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        //instantiate byte buffers to hold vertices and draw orders
        ByteBuffer bb = ByteBuffer.allocateDirect(arrowCoords.length * 4);

        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(arrowCoords);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);

        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        //holds a reference to a program object
        mProgram = GLES20.glCreateProgram();

        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);

        GLES20.glLinkProgram(mProgram);
        //program object can be used after shaders are attached and compiled and program is linked
    }

    public void draw(float[] mvpMatrix) {

        GLES20.glUseProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "mMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(mPositionHandle);

    }


}

//draws lines to create a border
class ArrowBorder{

    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;

    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    //3d, so 3 numbers per vertex
    static final int COORDS_PER_VERTEX = 3;

    //defines vertices
    static float arrowCoords[] = {
            0.0f, 0.5f, 0.125f,
            0.0f, 0.5f, -0.125f,
            0.25f, 0.125f, 0.125f,
            0.25f, 0.125f, -0.125f,
            0.125f, 0.125f, 0.125f,
            0.125f, 0.125f, -0.125f,
            0.125f, -0.5f, 0.125f,
            0.125f, -0.5f, -0.125f,
            -0.125f, -0.5f, 0.125f,
            -0.125f, -0.5f, -0.125f,
            -0.125f, 0.125f, 0.125f,
            -0.125f, 0.125f, -0.125f,
            -0.25f, 0.125f, 0.125f,
            -0.25f, 0.125f, -0.125f
    };

    //defines order
    private short pathOrder[] = {
            2, 0, 0, 12, 12, 10,
            10, 8, 8, 6, 6, 4,
            4, 2, 1, 0, 3, 2,
            5, 4, 7, 6, 8, 9,
            10, 11, 12, 13, 1, 3,
            3, 5, 5, 7, 7, 9,
            9, 11, 11, 13, 13, 1

    };

    //RGBA - black, full opacity
    float color[] = { 0.0f, 0.0f, 0.0f, 1.0f };

    //shader code
    private final String vertexShaderCode =
            "uniform mat4 mMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = mMVPMatrix * vPosition;" +
                    "}";

    //shader code
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private final int mProgram;

    private final int vertexStride = COORDS_PER_VERTEX * 4;

    public ArrowBorder() {

        //load the shaders
        int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        //instantiate and fill both byte buffers
        ByteBuffer bb = ByteBuffer.allocateDirect(arrowCoords.length * 4);

        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(arrowCoords);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(pathOrder.length * 2);

        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(pathOrder);
        drawListBuffer.position(0);

        mProgram = GLES20.glCreateProgram();

        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);

        GLES20.glLinkProgram(mProgram);
    }

    public void draw(float[] mvpMatrix) {

        GLES20.glUseProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "mMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glDrawElements(GLES20.GL_LINES, pathOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(mPositionHandle);

    }


}


//allows multisampling
class MultisampleConfigChooser implements GLSurfaceView.EGLConfigChooser {
    static private final String kTag = "GDC11";
    @Override
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        mValue = new int[1];

        // Try to find a normal multisample configuration first.
        int[] configSpec = {
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_DEPTH_SIZE, 16,
                // Requires that setEGLContextClientVersion(2) is called on the view.
                EGL10.EGL_RENDERABLE_TYPE, 4 /* EGL_OPENGL_ES2_BIT */,
                EGL10.EGL_SAMPLE_BUFFERS, 1 /* true */,
                EGL10.EGL_SAMPLES, 2,
                EGL10.EGL_NONE
        };

        if (!egl.eglChooseConfig(display, configSpec, null, 0,
                mValue)) {
            throw new IllegalArgumentException("eglChooseConfig failed");
        }
        int numConfigs = mValue[0];

        if (numConfigs <= 0) {
            // No normal multisampling config was found. Try to create a
            // converage multisampling configuration, for the nVidia Tegra2.
            // See the EGL_NV_coverage_sample documentation.

            final int EGL_COVERAGE_BUFFERS_NV = 0x30E0;
            final int EGL_COVERAGE_SAMPLES_NV = 0x30E1;

            configSpec = new int[]{
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_STENCIL_SIZE, 0,
                    EGL10.EGL_DEPTH_SIZE, 16,
                    EGL10.EGL_RENDERABLE_TYPE, 4 /* EGL_OPENGL_ES2_BIT */,
                    EGL_COVERAGE_BUFFERS_NV, 1 /* true */,
                    EGL_COVERAGE_SAMPLES_NV, 2,  // always 5 in practice on tegra 2
                    EGL10.EGL_NONE
            };

            if (!egl.eglChooseConfig(display, configSpec, null, 0,
                    mValue)) {
                throw new IllegalArgumentException("2nd eglChooseConfig failed");
            }
            numConfigs = mValue[0];

            if (numConfigs <= 0) {
                // Give up, try without multisampling.
                configSpec = new int[]{
                        EGL10.EGL_RED_SIZE, 8,
                        EGL10.EGL_GREEN_SIZE, 8,
                        EGL10.EGL_BLUE_SIZE, 8,
                        EGL10.EGL_ALPHA_SIZE, 8,
                        EGL10.EGL_STENCIL_SIZE, 0,
                        EGL10.EGL_DEPTH_SIZE, 16,
                        EGL10.EGL_RENDERABLE_TYPE, 4 /* EGL_OPENGL_ES2_BIT */,
                        EGL10.EGL_NONE
                };

                if (!egl.eglChooseConfig(display, configSpec, null, 0,
                        mValue)) {
                    throw new IllegalArgumentException("3rd eglChooseConfig failed");
                }
                numConfigs = mValue[0];
                if (numConfigs <= 0) {
                    throw new IllegalArgumentException("No configs match configSpec");
                }
            } else {
                mUsesCoverageAa = true;
            }
        }

        // Get all matching configurations.
        EGLConfig[] configs = new EGLConfig[numConfigs];
        if (!egl.eglChooseConfig(display, configSpec, configs, numConfigs,
                mValue)) {
            throw new IllegalArgumentException("data eglChooseConfig failed");
        }

        // CAUTION! eglChooseConfigs returns configs with higher bit depth
        // first: Even though we asked for rgb565 configurations, rgb888
        // configurations are considered to be "better" and returned first.
        // You need to explicitly filter the data returned by eglChooseConfig!
        int index = -1;
        for (int i = 0; i < configs.length; ++i) {
            if (findConfigAttrib(egl, display, configs[i], EGL10.EGL_RED_SIZE, 0) == 8) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            Log.w(kTag, "Did not find sane config, using first");
        }
        EGLConfig config;
        if(configs.length > 0){
            config = configs[index];
        }
        else{
            config = null;
        }
        if (config == null) {
            throw new IllegalArgumentException("No config chosen");
        }
        return config;
    }

    private int findConfigAttrib(EGL10 egl, EGLDisplay display,
                                 EGLConfig config, int attribute, int defaultValue) {
        if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
            return mValue[0];
        }
        return defaultValue;
    }

    public boolean usesCoverageAa() {
        return mUsesCoverageAa;
    }

    private int[] mValue;
    private boolean mUsesCoverageAa;
}