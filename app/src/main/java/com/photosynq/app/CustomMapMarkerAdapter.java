package com.photosynq.app;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.photosynq.app.db.LocationDatabaseHelper;
import com.squareup.picasso.Picasso;

/*
 * Created by Kevin on 7/29/2015.
 */
public class CustomMapMarkerAdapter implements GoogleMap.InfoWindowAdapter {

    ImageView imageView;
    TextView textView;
    LayoutInflater layoutInflater;
    LocationDatabaseHelper mDatabaseHelper;
    static int position;
    Context context;

    //required constructor
    public CustomMapMarkerAdapter(Context context){
        imageView = new ImageView(context);
        textView = new TextView(context);
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getInfoWindow(Marker marker){
        //instantiate database helper
        mDatabaseHelper = LocationDatabaseHelper.getHelper(context);
        //inflate and find the views
        View view = layoutInflater.inflate(R.layout.custom_list, null);
        imageView = (ImageView) view.findViewById(R.id.imageView1);
        textView = (TextView) view.findViewById(R.id.textView1);
        //set the marker name
        textView.setText(marker.getTitle());
        //loads image
        Cursor cursor = mDatabaseHelper.getOneWaypointFilePath(position);
        cursor.moveToFirst();
        final String filePath = cursorToString(cursor);
        if(filePath.equals("test answers") || filePath.equals("")){
            //default image
            Picasso.with(context).load(R.drawable.ic_launcher).into(imageView);
        }
        else {
            Picasso.with(context).load("file:" + filePath).into(imageView);
        }

        cursor.close();

        return view;
    }

    public static void setPosition(int value){
        position = value;
    }

    @Override
    public View getInfoContents(Marker marker){
        return null;
    }

    private String cursorToString(Cursor cursor) {
        return cursor.getString(0);
    }

}
