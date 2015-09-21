package com.photosynq.app;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.photosynq.app.db.LocationDatabaseHelper;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
 * Created by Kevin on 7/25/2015.
 */
public class CustomListViewAdapter extends BaseAdapter implements PopupMenu.OnMenuItemClickListener{
    List result;
    private Context context;
    private static LayoutInflater layoutInflater = null;
    public static int clickPosition;
    static String mFullPhotoPath;
    static Uri mPhotoUri;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int RESULT_LOAD_IMAGE = 2;
    private StartActivityForResultInterface adapterInterface;

    //constructor
    public CustomListViewAdapter(MainActivity mainActivity, List<String> list) {
        result = list;
        context = mainActivity;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    //saves bitmap to file and returns the filepath
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
    public int getCount(){
        return result.size();
    }

    @Override
    public Object getItem(int position){
        return position;
    }

    //implemented thing
    public long getItemId(int position){
        return position;
    }

    public String cursorToString(Cursor cursor){
       return cursor.getString(0);
    }

    public class Holder{
        public TextView tv1;
        public ImageView iv1;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent){
        final Holder holder;
        LocationDatabaseHelper mDatabaseHelper = LocationDatabaseHelper.getHelper(context);
        //find and inflate views
        if(convertView == null) {
            convertView = layoutInflater.inflate(R.layout.custom_list, null);
        }
        holder = new Holder();
        holder.tv1 = (TextView) convertView.findViewById(R.id.textView1);
        holder.iv1 = (ImageView) convertView.findViewById(R.id.imageView1);
        //sets text to waypoint name
        holder.tv1.setText(String.valueOf(result.get(position)));
        int position_increment = position + 1;
        Cursor cursor = mDatabaseHelper.getOneWaypointFilePath(position_increment);
        String imageLocation = null;
        if(cursor.moveToFirst()) {
            imageLocation = "file:" + cursorToString(cursor);
        }
        //image loader
        if(imageLocation == null) {
            //default image
            Picasso.with(context).load(R.drawable.ic_launcher).into(holder.iv1);
        }
        else if (imageLocation.equals("file:test answers")){
            //also default image
            Picasso.with(context).load(R.drawable.ic_launcher).into(holder.iv1);
        }
        else{
            Picasso.with(context).load(imageLocation).into(holder.iv1);
        }

        //allows users to choose between gallery and camera
        holder.iv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickPosition = position;
//                buildAlert();
                showPopup(holder.iv1);
            }
        });



        return convertView;
    }

    public void setCallback(StartActivityForResultInterface adapterInterface){
        this.adapterInterface = adapterInterface;
    }

    //create popup menu
    public void showPopup(View view){
        PopupMenu menu = new PopupMenu(context, view);
        MenuInflater inflater = menu.getMenuInflater();
        inflater.inflate(R.menu.menu_picture, menu.getMenu());
        menu.setOnMenuItemClickListener(this);
        menu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item){
        switch(item.getItemId()){
            //send gallery intent
            case R.id.gallery:
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                adapterInterface.myStartActivityForResultInterface(intent, RESULT_LOAD_IMAGE);
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
                        Uri mPhotoUriRaw = Uri.fromFile(photoFile);
                        URI mPhotoURIRaw;
                        try {
                            mPhotoURIRaw = new URI(mPhotoUriRaw.toString());
                            if(mPhotoUriRaw.getScheme() == null){
                                mPhotoUri = mPhotoUri.parse(mPhotoURIRaw.resolve(mPhotoURIRaw).toString());
                            }
                            else{
                                mPhotoUri = mPhotoUriRaw;
                            }
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }


                        adapterInterface.myStartActivityForResultInterface(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
                return true;
            default:
                return false;
        }
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

    public static int getClickPosition(){
        return clickPosition;
    }

    public static Uri getPhotoUri(){
        return mPhotoUri;
    }

    public static void setPhotoUri(String uri){
        mPhotoUri = Uri.parse(uri);
    }

    public static String getFullPhotoPath(){
        return mFullPhotoPath;
    }

    public static void setFullPhotoPath(String path){
        mFullPhotoPath = path;
    }

    public static void setClickPosition(int position){
        clickPosition = position;
    }
    }
