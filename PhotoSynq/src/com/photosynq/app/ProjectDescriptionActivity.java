package com.photosynq.app;


import java.text.SimpleDateFormat;
import java.util.Locale;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.model.ResearchProject;
import com.photosynq.app.utils.CommonUtils;

public class ProjectDescriptionActivity extends ActionBarActivity {

	private String recordHash = ""; 
	DatabaseHelper db;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_project_description);
		db = new DatabaseHelper(getApplicationContext());
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			recordHash = extras.getString(DatabaseHelper.C_RECORD_HASH);
			ResearchProject rp = db.getResearchProject(recordHash);
			SimpleDateFormat outputDate = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
			
			DisplayMetrics displaymetrics = new DisplayMetrics();
			this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
			int screenWidth = displaymetrics.widthPixels;
			//int screenHeight = displaymetrics.heightPixels;
			
			TextView tvProjetTitle = (TextView) findViewById(R.id.project_name);
			TextView tvProjetDesc = (TextView) findViewById(R.id.project_desc);
			TextView tvStartDate = (TextView) findViewById(R.id.start_date);
			TextView tvEndDate = (TextView) findViewById(R.id.end_date);
			TextView tvBeta = (TextView) findViewById(R.id.beta);
			tvProjetTitle.setText(rp.getName());
			tvProjetTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX,(int)(screenWidth*0.06));
			if(!"null".equals(rp.getDesc()))
			{
				tvProjetDesc.setText(rp.getDesc());
			}else{tvProjetDesc.setText(getResources().getString(R.string.no_data_found));}
			
			if(!"null".equals(rp.getStart_date()))
			{
				tvStartDate.setText(outputDate.format(CommonUtils.convertToDate(rp.getStart_date())));
			}else{tvStartDate.setText(getResources().getString(R.string.no_data_found));}
			
			if(!"null".equals(rp.getEnd_date()))
			{
				tvEndDate.setText(outputDate.format(CommonUtils.convertToDate(rp.getEnd_date())));
			}else{tvEndDate.setText(getResources().getString(R.string.no_data_found));}
			
			if(!	"null".equals(rp.getBeta()))
			{
				tvBeta.setText(rp.getBeta());
			}else{tvBeta.setText(getResources().getString(R.string.no_data_found));}
		}
		db.closeDB();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.project_description, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void onParticipateClicked(View view)
	{
		Intent intent = new Intent(getApplicationContext(),DirectionsActivity.class);
		Log.d("##### Project description record hash :", recordHash);
		intent.putExtra(DatabaseHelper.C_RECORD_HASH, recordHash);
		startActivity(intent);
	}

}
