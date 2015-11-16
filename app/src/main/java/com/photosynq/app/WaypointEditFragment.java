package com.photosynq.app;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.photosynq.app.db.LocationDatabaseHelper;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/*
 * Created by Kevin on 8/10/2015.
 */
public class WaypointEditFragment extends Fragment implements StartActivityForResultInterface, UpdateTableForDatabaseInterface{

    private static int mSectionNumber;

    private List<String> mListHeader;

    private HashMap mListChild;

    private Cursor cursor;

    private static CustomExpandableListAdapter listAdapter;

    private LocationDatabaseHelper mDatabaseHelper;

    private ExpandableListView expListView;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    static final int RESULT_OKAY = -1;

    static final int RESULT_LOAD_IMAGE = 2;

    private int orientation;


    public static WaypointSetFragment newInstance(int sectionNumber) {
        WaypointSetFragment fragment = new WaypointSetFragment();
        mSectionNumber = sectionNumber;
        return fragment;
    }

    public WaypointEditFragment() {
        //required empty constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDatabaseHelper = LocationDatabaseHelper.getHelper(getActivity());

        mListChild = new HashMap();

        createListData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.fragment_waypoint_edit, null);

        Button wayBtnBack = (Button) rootView.findViewById(R.id.edit_back);

        wayBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager().popBackStackImmediate();
            }
        });

        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        mDatabaseHelper.setCallback(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        expListView = (ExpandableListView) getActivity().findViewById(R.id.list_location);

        createListAdapter();

        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPosition, int childPosition, long id) {
                if (childPosition == 0) {
                    buildAlert("Enter a name", childPosition, groupPosition);
                } else if (childPosition == 1) {
                    buildAlert("Enter a latitude:", childPosition, groupPosition);
                } else if (childPosition == 2) {
                    buildAlert("Enter a longitude:", childPosition, groupPosition);
                }
                return true;
            }
        });

        expListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                int itemType = ExpandableListView.getPackedPositionType(id);
                if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    int groupPosition = ExpandableListView.getPackedPositionGroup(id) + 1;
                    mDatabaseHelper.deleteWaypoint(groupPosition);
                    mDatabaseHelper.idReset();
                    return true;
                } else {
                    return false;
                }
            }
        });



    }

    private void createListAdapter(){
        listAdapter = new CustomExpandableListAdapter(this.getActivity(), mListHeader, mListChild, mDatabaseHelper);

        listAdapter.setCallback(this);

        expListView.setAdapter(listAdapter);

    }
/*
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OKAY){

            //create content resolver, initialize bitmap and photo path
            ContentResolver contentResolver = this.getActivity().getContentResolver();
            Bitmap bitmap;
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
                        mCurrentPhotoPath = listAdapter.saveBitmap(thumb);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix = new Matrix();
                        matrix.postRotate(90);
                        rotatedBitmap = Bitmap.createBitmap(thumb, 0, 0, thumb.getWidth(), thumb.getHeight(), matrix, true);
                        mCurrentPhotoPath = listAdapter.saveBitmap(rotatedBitmap);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix = new Matrix();
                        matrix.postRotate(180);
                        rotatedBitmap = Bitmap.createBitmap(thumb, 0, 0, thumb.getWidth(), thumb.getHeight(), matrix, true);
                        mCurrentPhotoPath = listAdapter.saveBitmap(rotatedBitmap);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix = new Matrix();
                        matrix.postRotate(270);
                        rotatedBitmap = Bitmap.createBitmap(thumb, 0, 0, thumb.getWidth(), thumb.getHeight(), matrix, true);
                        mCurrentPhotoPath = listAdapter.saveBitmap(rotatedBitmap);
                        break;
                    default:
                        mCurrentPhotoPath = listAdapter.saveBitmap(thumb);
                        break;
                }

            }
            else{
                //if not, set to default thing
                mCurrentPhotoPath = "test answers";
            }

            //save both filepaths to database
            mDatabaseHelper.updateUserAnswersToFilePath(mCurrentPhotoPath, groupID + 1);
            mDatabaseHelper.updateLargeFilePath(CustomListViewAdapter.getFullPhotoPath(), groupID + 1);
            //refresh to get image
            createListAdapter();
        }
        if(requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OKAY){
            //gets a picture from the gallery
            Uri selectedImage = data.getData();
            int groupID = data.getIntExtra("GROUP_ID", 1);

            //get file path for image
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursorImage = getActivity().getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursorImage.moveToFirst();
            int columnIndex = cursorImage.getColumnIndex(MediaStore.Images.Media.DATA);
            String path = cursorImage.getString(columnIndex);

            //set filepath in database
            mDatabaseHelper.updateLargeFilePath(path, groupID + 1);

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
                    mCurrentPhotoPath = listAdapter.saveBitmap(thumb);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix = new Matrix();
                    matrix.postRotate(90);
                    rotatedBitmap = Bitmap.createBitmap(thumb, 0, 0, thumb.getWidth(), thumb.getHeight(), matrix, true);
                    mCurrentPhotoPath = listAdapter.saveBitmap(rotatedBitmap);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix = new Matrix();
                    matrix.postRotate(180);
                    rotatedBitmap = Bitmap.createBitmap(thumb, 0, 0, thumb.getWidth(), thumb.getHeight(), matrix, true);
                    mCurrentPhotoPath = listAdapter.saveBitmap(rotatedBitmap);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix = new Matrix();
                    matrix.postRotate(270);
                    rotatedBitmap = Bitmap.createBitmap(thumb, 0, 0, thumb.getWidth(), thumb.getHeight(), matrix, true);
                    mCurrentPhotoPath = listAdapter.saveBitmap(rotatedBitmap);
                    break;
                default:
                    mCurrentPhotoPath = listAdapter.saveBitmap(thumb);
                    break;
            }

            //add filepath to database
            mDatabaseHelper.updateUserAnswersToFilePath(mCurrentPhotoPath, groupID + 1);
            //close the cursor
            cursorImage.close();
        }

    }*/




    private void createListData() {
        mListHeader = new ArrayList<>();
        if(mListChild != null) {
            mListChild.clear();
        }
        cursor = mDatabaseHelper.getAllWaypoints();
        cursor.moveToFirst();

        List<String> latLongList = new ArrayList<>();
        List<List<String>> listLatLongList = new ArrayList<>();
        while(!cursor.isAfterLast()){
            String name = cursorToStringName(cursor, 1);
            String latitude = cursorToStringName(cursor, 2);
            String longitude = cursorToStringName(cursor, 3);
            mListHeader.add(name);
            latLongList.add(name);
            latLongList.add(latitude);
            latLongList.add(longitude);
            latLongList.add("map");
            latLongList.add("image");
            cursor.moveToNext();
        }

        for(int i = 0; i < latLongList.size();  i+=5){
            listLatLongList.add(new ArrayList<>( latLongList.subList(i, i + 5)));
        }

        for(String name : mListHeader){
            int pos = mListHeader.indexOf(name);
            mListChild.put(name, listLatLongList.get(pos));
        }

    }

    // required interface method
    @Override
    public void myStartActivityForResultInterface(Intent intent, int requestCode){
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void myUpdateTableForDatabaseInterface(){
        createListData();
        createListAdapter();
    }

    protected void buildAlert(String title, final int child, final int parent){

        //create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //inflates prompt window
        LayoutInflater promptInflater = LayoutInflater.from(getActivity());
        //set view to prompts.xml
        View promptView = promptInflater.inflate(R.layout.prompts, null);
        final EditText userInput = (EditText) promptView.findViewById(R.id.editTextDialogUserInput);
        builder.setView(promptView);

        //build prompt window
        builder.setTitle(title)
                .setPositiveButton(R.string.okay_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int parent1 = parent + 1;
                        String value = userInput.getText().toString();
                        if (child == 1 || child == 2) {
                            mDatabaseHelper.setOneWaypointLatLong(parent1, child, value);
                        } else {
                            mDatabaseHelper.updateName(parent1, value);
                        }

                    }
                })
                .setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //stop editing waypoint on cancel
                        dialogInterface.dismiss();
                    }
                });
        //create and show builder
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }


    private String cursorToStringName(Cursor cursor, int location){
        return cursor.getString(location);
    }
}

class CustomExpandableListAdapter extends BaseExpandableListAdapter implements PopupMenu.OnMenuItemClickListener{
    protected static Context context;
    private List parentList;
    private LocationDatabaseHelper mDatabaseHelper;
    private HashMap<String, List<String>> childMap;
    private String mFullPhotoPath;
    private Uri mPhotoUri;
    private int groupID;
    protected static StartActivityForResultInterface adapterInterface;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int RESULT_LOAD_IMAGE = 2;

    public CustomExpandableListAdapter(Context context, List parent, HashMap<String, List<String>> child, LocationDatabaseHelper helper){
        this.context = context;
        this.parentList = parent;
        this.childMap = child;
        this.mDatabaseHelper = helper;
        //expand constructor later
    }

    @Override
    public Object getChild(int groupPosition, int childPosition){
        return this.childMap.get(this.parentList.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition){
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild,
                             View convertView,  ViewGroup parent){
        final String childText = (String) getChild(groupPosition, childPosition);

        if(convertView == null){
            LayoutInflater inflater= (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.custom_child, null);
        }

        TextView textView1 = (TextView) convertView.findViewById(R.id.textViewChild1);
        ImageView mapImage = (ImageView) convertView.findViewById(R.id.mapImage);
        ImageView largeImage = (ImageView) convertView.findViewById(R.id.largeImage);

        if(childPosition == 3) {

            String mapGetURL = "http://maps.googleapis.com/maps/api/staticmap?zoom=18&size=960x480&markers=size:mid|color:red|" +
                    getChild(groupPosition, 1) + "," + getChild(groupPosition, 2) + "&sensor=false";
            Picasso.with(context).load(mapGetURL).into(mapImage);

            mapImage.setVisibility(View.VISIBLE);
            textView1.setVisibility(View.GONE);
            largeImage.setVisibility(View.GONE);

        } else if(childPosition == 4){

            String largeFilePath = getLargeFilePath(groupPosition + 1);
            Picasso.with(context).load(Uri.parse("file://" + largeFilePath)).into(largeImage);

            largeImage.setVisibility(View.VISIBLE);
            mapImage.setVisibility(View.GONE);
            textView1.setVisibility(View.GONE);
        }

        else{
            textView1.setText(childText);
            textView1.setVisibility(View.VISIBLE);
            mapImage.setVisibility(View.GONE);
            largeImage.setVisibility(View.GONE);
        }

        return convertView;
    }

    public void showPopup(View view, int position){
        groupID = position;
        PopupMenu menu = new PopupMenu(context, view);
        MenuInflater inflater = menu.getMenuInflater();
        inflater.inflate(R.menu.menu_picture, menu.getMenu());
        menu.setOnMenuItemClickListener(this);
        menu.show();
    }

    public String saveBitmap(Bitmap bmp){
        //filename that shouldn't collide with other filenames
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "PNG_" + timeStamp;
        String path = Environment.getExternalStorageDirectory().toString();
        File file = new File(path, imageFileName + ".png");
        //part that saves stuff
        FileOutputStream out = null;
        try{
            out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                if (out != null){
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        //gets filepath
        return file.getAbsolutePath();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item){
        switch(item.getItemId()){
            //send gallery intent
            case R.id.gallery:
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                adapterInterface.myStartActivityForResultInterface(intent, RESULT_LOAD_IMAGE);
                intent.putExtra("GROUP_ID", groupID);
                return true;
            //send camera intent
            case R.id.camera:
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
                        takePictureIntent.putExtra("GROUP_ID", groupID);
                        mPhotoUri = Uri.fromFile(photoFile);
                        adapterInterface.myStartActivityForResultInterface(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
                return true;
            default:
                return false;
        }
    }

    public void setCallback(StartActivityForResultInterface adapterInterface){
        this.adapterInterface = adapterInterface;
    }

    //creates full image
    private File createPhotoFile()throws IOException{
        String timeStamp  = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "PNG_FULL_" + timeStamp;
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".png", directory);
        mFullPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public int getChildrenCount(int groupPosition){
        return this.childMap.get(this.parentList.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition){
        return this.parentList.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this.parentList.size();
    }

    @Override
    public long getGroupId(int groupPosition){
        return groupPosition;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent){
        String headerTitle = (String) getGroup(groupPosition);

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.custom_header, null);
        }

        TextView textView = (TextView) convertView.findViewById(R.id.parentHeader);

        textView.setText(headerTitle);

        ImageView imageView = (ImageView) convertView.findViewById(R.id.parentImage);

        String smallFilePath = getFilePath(groupPosition + 1);

        Picasso.with(context).load("file:" + smallFilePath).into(imageView);



       /* imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view, groupPosition);
            }
        });
*/
        return convertView;
    }

    @Override
    public boolean hasStableIds(){
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition){
        return true;
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

    //pulls one waypoint data column as a string
    private String cursorToString(Cursor cursor) {
        String string = cursor.getString(0);
        cursor.close();
        return string;
    }
}