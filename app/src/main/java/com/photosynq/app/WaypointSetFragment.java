package com.photosynq.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.photosynq.app.utils.CommonUtils;
import com.photosynq.app.db.LocationDatabaseHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WaypointSetFragment extends Fragment implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener, StartActivityForResultInterface,
UpdateTableForDatabaseInterface{

    private static int mSectionNumber;

    public static WaypointSetFragment newInstance(int sectionNumber) {
        WaypointSetFragment fragment = new WaypointSetFragment();
        mSectionNumber = sectionNumber;
        return fragment;
    }

    protected static GoogleApiClient mGoogleApiClient;

    protected Location mCurrentLocation;

    protected TextView mDistanceText;

    protected LocationRequest mLocationRequest;

    protected LocationDatabaseHelper mDatabaseHelper;

    private ListView mListView;

    protected CustomListViewAdapter arrayAdapter;

    private Cursor cursor;

    protected Location destinationLocation;

    protected double distance;

    private Button wayBtnNext;

    protected MapView mapView;

    protected GoogleMap map;

    protected LatLng wayptPosition;

    protected String name;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    static final int RESULT_OKAY = -1;

    static final int RESULT_LOAD_IMAGE = 2;

    static final int REQUEST_IMAGE_FROM_GALLERY = 3;

    private int orientation;

    protected static Map mMarkerHashMap;

    private static boolean SHOW_DISTANCE = false;

    public WaypointSetFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //startup stuff to do
        buildGoogleApiClient();
        createLocationRequest();
        //both start on (0,0)
        destinationLocation = new Location("also blank");
        mCurrentLocation = destinationLocation;
        mMarkerHashMap = new HashMap();

    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        if(CustomListViewAdapter.getPhotoUri() != null){
             outState.putString("cameraImageUri", CustomListViewAdapter.getPhotoUri().toString());
        }
        if(CustomListViewAdapter.getFullPhotoPath() != null){
            outState.putString("cameraImageFilePath", CustomListViewAdapter.getFullPhotoPath());
        }
        outState.putInt("position", CustomListViewAdapter.getClickPosition());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_waypoint_set, container, false);

        //Set up database helper
        mDatabaseHelper = LocationDatabaseHelper.getHelper(getActivity());
        mDatabaseHelper.setCallback(this);

        //Set up waypoint set button
        Button wayBtnSet = (Button) rootView.findViewById(R.id.btn_set_wypnt);
        wayBtnSet.setTypeface(CommonUtils.getInstance(getActivity()).getFontRobotoMedium());
        //click response
        wayBtnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient);
                buildAlert();
            }
        });

        //set up button for deleting all waypoints
        Button wayBtnDelete = (Button) rootView.findViewById(R.id.btn_dlt_wypnt);
        wayBtnDelete.setTypeface(CommonUtils.getInstance(getActivity()).getFontRobotoMedium());

        wayBtnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
            public void onClick(View view) {

                setShowDistanceFalse();

                //removes all markers from map
                for(Marker marker : (Set<Marker>) mMarkerHashMap.keySet()){
                    marker.remove();
                }

                //removes locally stored images
                for(int id : (Collection<Integer>) mMarkerHashMap.values()){
                    String filePath = getFilePath(id);
                    File smallFile = new File(filePath);
                    smallFile.delete();

                    String largeFilePath = getLargeFilePath(id);
                    File largeFile = new File(largeFilePath);
                    largeFile.delete();
                }

                //deletes all waypoints
                mDatabaseHelper.deleteTable();
                //refreshes view
                createArrayAdapter();
                //clears hashmap
                mMarkerHashMap.clear();
            }
        });

        //set up button to get next waypoint + distance
        wayBtnNext = (Button) rootView.findViewById(R.id.btn_nxt_wypnt);
        wayBtnNext.setTypeface(CommonUtils.getInstance(getActivity()).getFontRobotoMedium());

        wayBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //retrieves current waypoint from database
                double latitude_orig = destinationLocation.getLatitude();
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
                setNextVisibility();

            }
        });

        Button wayBtnBack = (Button) rootView.findViewById(R.id.set_back);
        wayBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager().popBackStackImmediate();
            }
        });

        //create map and find it
        mapView = (MapView) rootView.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        map = mapView.getMap();
        //use the custom map marker
        map.setInfoWindowAdapter(new CustomMapMarkerAdapter(this.getActivity()));
        map.setMyLocationEnabled(true);
        MapsInitializer.initialize(this.getActivity());

        //event for adding waypoint on map click
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
            public void onMapClick(LatLng clickLocation){
                //round latitudes and longitudes to 7 decimal places
                double latitude = Double.parseDouble(String.format("%.7f", clickLocation.latitude));
                double longitude = Double.parseDouble(String.format("%.7f", clickLocation.longitude));
                buildAlert(latitude, longitude);
            }
        });



        //passes a position to the adapter so it can display an image
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //pass waypoint ID to the marker
                CustomMapMarkerAdapter.setPosition((Integer) mMarkerHashMap.get(marker));
                //false causes all default marker activity to happen
                return false;
            }
        });

        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                //request full image from the gallery for viewing
                String largeFilePath = getLargeFilePath((Integer) mMarkerHashMap.get(marker));
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + largeFilePath), "image/*");
                getActivity().startActivityForResult(intent, REQUEST_IMAGE_FROM_GALLERY);

            }
        });

        return rootView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(savedInstanceState != null) {
            if (savedInstanceState.containsKey("cameraImageUri")) {
                CustomListViewAdapter.setPhotoUri(savedInstanceState.getString("cameraImageUri"));
            }
            if(savedInstanceState.containsKey("cameraImageFilePath")) {
                CustomListViewAdapter.setFullPhotoPath(savedInstanceState.getString("cameraImageFilePath"));
            }
            if(savedInstanceState.containsKey("position")){
                CustomListViewAdapter.setClickPosition(savedInstanceState.getInt("position"));
            }

        }

        //create and find the listview
        mListView = (ListView) getActivity().findViewById(R.id.location_list);
        //populate it
        createArrayAdapter();
        //find textview for distance
        mDistanceText = (TextView) getActivity().findViewById(R.id.distance_text);
        //keep it blank for now
        mDistanceText.setText("");
        //define short click action
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long rowId) {
                //add 1 because position starts at 0, db starts at 1
                position = position + 1;
                //show the distance
                setShowDistanceTrue();
                //find the distance
                getDistance(getLatitude(position), getLongitude(position));
                //show "next" button
                setNextVisibility();
            }
        });

        //define long click action
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long rowId) {
                position = position + 1;

                //delete locally stored images
                //using the iterator prevents concurrent access problems
                Iterator<Marker> markerIterator = mMarkerHashMap.keySet().iterator();
                while (markerIterator.hasNext()) {
                    Marker marker = markerIterator.next();
                    if ((Integer) mMarkerHashMap.get(marker) == position) {
                        marker.remove();

                        String filePath = getFilePath(position);
                        File smallFile = new File(filePath);
                        smallFile.delete();

                        String largeFilePath = getLargeFilePath(position);
                        File largeFile = new File(largeFilePath);
                        largeFile.delete();

                        markerIterator.remove();
                    }

                }

                //reset IDs in hashmap
                for (Marker marker : (Set<Marker>) mMarkerHashMap.keySet()) {
                    if ((Integer) mMarkerHashMap.get(marker) > position) {
                        int Id = (Integer) mMarkerHashMap.get(marker);
                        mMarkerHashMap.put(marker, Id - 1);
                    }
                }

                setShowDistanceFalse();

                //deleting a waypoint
                mDatabaseHelper.deleteWaypoint(position);
                //reset IDs
                mDatabaseHelper.idReset();
                //refresh view
                arrayAdapter.notifyDataSetChanged();
                createArrayAdapter();

                return true;
            }
        });


    }

    //builds Google API Client
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this.getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    //creates location service request
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
       // mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);
       // mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);
        mLocationRequest.setInterval(30);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    //actually starts the location updates
    protected void startLocationUpdates(){
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    //makes the next waypoint button visible if distance < 5 meters
    protected void setNextVisibility(){
        if(mCurrentLocation.distanceTo(destinationLocation) < 5) {
            wayBtnNext.setVisibility(View.VISIBLE);
        }
        else {
            wayBtnNext.setVisibility(View.INVISIBLE);
        }
    }

    //populates an arraylist with every waypoint in the database
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

    // required interface method
    @Override
    public void myStartActivityForResultInterface(Intent intent, int requestCode){
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void myUpdateTableForDatabaseInterface(){}

    //adds markers to map and populates hashmap from database-only called on start right now
    @SuppressWarnings("unchecked")
    private void drawAllMarkers(){
        for(int l = 1; l <= getAllLocations().size(); l++){
            String name = getName(l);
            LatLng latLng = new LatLng(getLatitude(l), getLongitude(l));
            Marker marker = map.addMarker(new MarkerOptions().position(latLng).title(name));
            mMarkerHashMap.put(marker, l);
        }
    }

    //gets distance to waypoint
    protected void getDistance(double latitude, double longitude){
        destinationLocation = latLongToLocation(latitude, longitude);
        distance = mCurrentLocation.distanceTo(destinationLocation);
        if(SHOW_DISTANCE) {
            if (mDistanceText != null) {
                mDistanceText.setText(String.valueOf(distance));
            }
        }
    }

    //moves camera to location
    protected void cameraUpdater(double latitude, double longitude) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 19);
        map.animateCamera(cameraUpdate);
    }


    //adds a waypoint to database and map from set waypoint TODO do something about test answers
    @SuppressWarnings("unchecked")
    protected void addWaypoint(){
        //add waypoint to database
        mDatabaseHelper.insertWaypoint(name, String.valueOf(mCurrentLocation.getLatitude()),
                String.valueOf(mCurrentLocation.getLongitude()), "test answers", "blank path");

        //refresh the listview
        createArrayAdapter();

        //add marker to map and marker array
        cameraUpdater(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        wayptPosition = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        Marker marker = map.addMarker(new MarkerOptions().position(wayptPosition).title(name));

        //add marker to hashmap
        int id = getId(wayptPosition.latitude);
        mMarkerHashMap.put(marker, id);
    }

    //adds waypoint on map click event
    @SuppressWarnings("unchecked")
    protected void addWaypoint(double latitude, double longitude){
        //add waypoint to database
        mDatabaseHelper.insertWaypoint(name, String.valueOf(latitude), String.valueOf(longitude), "test answers", "blank path");

        //refresh the listview
        createArrayAdapter();

        //add marker to map and marker array
        cameraUpdater(latitude, longitude);
        wayptPosition = new LatLng(latitude, longitude);
        Marker marker = map.addMarker(new MarkerOptions().position(wayptPosition).title(name));

        //add marker to hashmap
        int id = getId(wayptPosition.latitude);
        mMarkerHashMap.put(marker, id);
    }

    //build alert dialog for button click event
    protected void buildAlert(){
        //create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //inflates prompt window
        LayoutInflater promptInflater = LayoutInflater.from(getActivity());
        //set view to prompts.xml
        View promptView = promptInflater.inflate(R.layout.prompts, null);
        final EditText userInput = (EditText) promptView.findViewById(R.id.editTextDialogUserInput);
        builder.setView(promptView);

        //build prompt window
        builder.setTitle(R.string.dialog_title)
                .setPositiveButton(R.string.okay_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //adds user inputted name
                        name = userInput.getText().toString();
                        if(name.equals("")){
                            //default name
                            name = "Waypoint";
                        }
                        addWaypoint();
                    }
                })
                .setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //stop adding waypoint on cancel
                        dialogInterface.dismiss();
                    }
                });
        //create and show builder
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    //build alert dialog for map click event
    protected void buildAlert(final double latitude, final double longitude){
        //create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //inflates prompt window
        LayoutInflater promptInflater = LayoutInflater.from(getActivity());
        //set view to prompts.xml
        View promptView = promptInflater.inflate(R.layout.prompts, null);
        final EditText userInput = (EditText) promptView.findViewById(R.id.editTextDialogUserInput);
        builder.setView(promptView);

        //build prompt window
        builder.setTitle(R.string.dialog_title)
                .setPositiveButton(R.string.okay_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //adds user inputted name
                        name = userInput.getText().toString();
                        if(name.equals("")) {
                            //default name
                            name = "Waypoint";
                        }
                        addWaypoint(latitude, longitude);
                    }
                })
                .setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //stop adding waypoint on cancel
                        dialogInterface.dismiss();
                    }
                });
        //create and show builder
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /*private void buildCameraAlert(){
        //create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //inflates prompt window
        LayoutInflater promptInflater = LayoutInflater.from(getActivity());
        //set view to prompts.xml
        View promptView = promptInflater.inflate(R.layout.prompt_camera, null);
        builder.setView(promptView);

        //build prompt window
        builder.setTitle(R.string.dialog_title)
                .setPositiveButton(R.string.camera_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
                            File photoFile = null;
                            try {
                                photoFile = createPhotoFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (photoFile != null) {
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                                mPhotoUri = Uri.fromFile(photoFile);
                                adapterInterface.myStartActivityForResultInterface(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                            }
                        }
                    }
                })
                .setNeutralButton(R.string.gallery_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        adapterInterface.myStartActivityForResultInterface(intent, RESULT_LOAD_IMAGE);
                    }
                })
                .setNegativeButton(R.string.later_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //stop adding waypoint on cancel
                        dialogInterface.dismiss();
                    }
                });
        //create and show builder
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
*/
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
                //stuff to do when new image is captured
                if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OKAY){

                    //create content resolver, initialize bitmap and photo path
                    ContentResolver contentResolver = this.getActivity().getContentResolver();
                    Bitmap bitmap = null;
                    String mCurrentPhotoPath;
                    //get full size bitmap from camera
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(contentResolver, CustomListViewAdapter.getPhotoUri());
                    } catch (IOException | NullPointerException e1) {
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(contentResolver, CustomListViewAdapter.getPhotoUri());
                        } catch (IOException | NullPointerException e2) {
                            try {
                                //3rd time's the charm
                                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, CustomListViewAdapter.getPhotoUri());
                            } catch (IOException | NullPointerException e3) {
                                throw new RuntimeException(e3);
                            }
                        }
                    }

                    try {
                        ExifInterface exif = new ExifInterface(CustomListViewAdapter.getFullPhotoPath());
                        orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //scale bitmap down if possible
                    if(bitmap != null) {
                        Bitmap rotatedBitmap;
                        Matrix matrix;
                        double scale = bitmap.getHeight() * 96 / bitmap.getWidth();
                        Bitmap thumb = Bitmap.createScaledBitmap(bitmap, 96, (int) scale, false);
                        //rotate image based on orientation
                        switch (orientation) {
                            case ExifInterface.ORIENTATION_NORMAL:
                                mCurrentPhotoPath = arrayAdapter.saveBitmap(thumb);
                                break;
                            case ExifInterface.ORIENTATION_ROTATE_90:
                                matrix = new Matrix();
                                matrix.postRotate(90);
                                rotatedBitmap = Bitmap.createBitmap(thumb, 0, 0, thumb.getWidth(), thumb.getHeight(), matrix, true);
                                mCurrentPhotoPath = arrayAdapter.saveBitmap(rotatedBitmap);
                                break;
                            case ExifInterface.ORIENTATION_ROTATE_180:
                                matrix = new Matrix();
                                matrix.postRotate(180);
                                rotatedBitmap = Bitmap.createBitmap(thumb, 0, 0, thumb.getWidth(), thumb.getHeight(), matrix, true);
                                mCurrentPhotoPath = arrayAdapter.saveBitmap(rotatedBitmap);
                                break;
                            case ExifInterface.ORIENTATION_ROTATE_270:
                                matrix = new Matrix();
                                matrix.postRotate(270);
                                rotatedBitmap = Bitmap.createBitmap(thumb, 0, 0, thumb.getWidth(), thumb.getHeight(), matrix, true);
                                mCurrentPhotoPath = arrayAdapter.saveBitmap(rotatedBitmap);
                                break;
                            default:
                                mCurrentPhotoPath = arrayAdapter.saveBitmap(thumb);
                                break;
                        }

                    }
                    else{
                        //if not, set to default thing
                        mCurrentPhotoPath = "test answers";
                    }

                    //save both filepaths to database
                    mDatabaseHelper.updateUserAnswersToFilePath(mCurrentPhotoPath, CustomListViewAdapter.getClickPosition() + 1);
                    mDatabaseHelper.updateLargeFilePath(CustomListViewAdapter.getFullPhotoPath(), CustomListViewAdapter.getClickPosition() + 1);
                    //refresh to get image
                    createArrayAdapter();
                }
                if(requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OKAY){
                    //gets a picture from the gallery
                    Uri selectedImage = data.getData();

                    //get file path for image
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };
                    Cursor cursorImage = getActivity().getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
                    cursorImage.moveToFirst();
                    int columnIndex = cursorImage.getColumnIndex(MediaStore.Images.Media.DATA);
                    String path = cursorImage.getString(columnIndex);

                    //set filepath in database
                    mDatabaseHelper.updateLargeFilePath(path, CustomListViewAdapter.getClickPosition() + 1);

                    //get orientation of image
                    try {
                        ExifInterface exif = new ExifInterface(path);
                        orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //scale down gallery images
                    BitmapFactory.Options compressor = new BitmapFactory.Options();
                    compressor.inSampleSize = 2;
                    Bitmap bitmap = BitmapFactory.decodeFile(path, compressor);
                    double scale = bitmap.getHeight() * 96 / bitmap.getWidth();
                    Bitmap thumb = Bitmap.createScaledBitmap(bitmap, 96, (int) scale, false);

                    //initialize other things
                    Bitmap rotatedBitmap;
                    String mCurrentPhotoPath;
                    Matrix matrix;

                    //rotate image based on orientation
                    switch (orientation) {
                        case ExifInterface.ORIENTATION_NORMAL:
                            mCurrentPhotoPath = arrayAdapter.saveBitmap(thumb);
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            matrix = new Matrix();
                            matrix.postRotate(90);
                            rotatedBitmap = Bitmap.createBitmap(thumb, 0, 0, thumb.getWidth(), thumb.getHeight(), matrix, true);
                            mCurrentPhotoPath = arrayAdapter.saveBitmap(rotatedBitmap);
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            matrix = new Matrix();
                            matrix.postRotate(180);
                            rotatedBitmap = Bitmap.createBitmap(thumb, 0, 0, thumb.getWidth(), thumb.getHeight(), matrix, true);
                            mCurrentPhotoPath = arrayAdapter.saveBitmap(rotatedBitmap);
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            matrix = new Matrix();
                            matrix.postRotate(270);
                            rotatedBitmap = Bitmap.createBitmap(thumb, 0, 0, thumb.getWidth(), thumb.getHeight(), matrix, true);
                            mCurrentPhotoPath = arrayAdapter.saveBitmap(rotatedBitmap);
                            break;
                        default:
                            mCurrentPhotoPath = arrayAdapter.saveBitmap(thumb);
                            break;
                    }

                    //add filepath to database
                    mDatabaseHelper.updateUserAnswersToFilePath(mCurrentPhotoPath, CustomListViewAdapter.getClickPosition() + 1);
                    //refresh listview
                    createArrayAdapter();
                    //close the cursor
                    cursorImage.close();
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

    //gets location in database for waypoint with specified latitude
    protected int getId(double latitude) {
        cursor = mDatabaseHelper.getOneWaypointId(latitude);
        if(cursor.moveToFirst()) {
            return cursorToInt(cursor);
        }
        else{
            return -1;
        }
    }

    //gets the file path for the full image
    private String getLargeFilePath(int Id){
        Cursor cursor = mDatabaseHelper.getOneWaypointLargeFilePath(Id);
        cursor.moveToFirst();
        return cursorToString(cursor);
    }

    //get file path for small image
    private String getFilePath(int Id){
        Cursor cursor = mDatabaseHelper.getOneWaypointFilePath(Id);
        cursor.moveToFirst();
        return cursorToString(cursor);
    }

    //creates a location object from a latitude and a longitude
    protected Location latLongToLocation(double latitude, double longitude){
        Location location = new Location("no actual provider");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }

    //pulls doubles out of values returned from query
    private double cursorToDouble(Cursor cursor, int integer) {
        return cursor.getDouble(integer);
    }

    //pulls the waypoint name
    private String cursorToStringName(Cursor cursor){
        return cursor.getString(1);
    }

    //pulls one waypoint data column as a string
    private String cursorToString(Cursor cursor) {
        return cursor.getString(0);
    }

    //pulls one waypoint data column as an int
    private int cursorToInt(Cursor cursor) {
        return cursor.getInt(0);
    }

    //creates arrayAdapter from the custom adapter that populates the listview
    private void createArrayAdapter(){
        arrayAdapter = new CustomListViewAdapter(
                (MainActivity) this.getActivity(),
                getAllLocations()
        );
        arrayAdapter.setCallback(this);
        mListView.setAdapter(arrayAdapter);
    }

    private void setShowDistanceTrue(){
        SHOW_DISTANCE = true;
    }

    private void setShowDistanceFalse(){
        SHOW_DISTANCE = false;
    }

    //required for interface
    @Override
    public void onConnectionSuspended(int cause){}

    //required for interface
    @Override
    public void onConnectionFailed(ConnectionResult result){}

    //connect to location services, redraw all markers
    @Override
    public void onStart() {
        super.onStart();
        drawAllMarkers();
        mGoogleApiClient.connect();
    }

    //disconnect from location services
    @Override
    public void onStop() {
        super.onStop();
        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    //update distance calculation when user location changes
    @Override
    public void onLocationChanged(Location location){
        mCurrentLocation = location;
        if(mDistanceText!=null) {
            if(SHOW_DISTANCE) {
                double distance = mCurrentLocation.distanceTo(destinationLocation);
                mDistanceText.setText(String.valueOf(distance));
            }
        }
       setNextVisibility();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        cameraUpdater(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        startLocationUpdates();
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


}
