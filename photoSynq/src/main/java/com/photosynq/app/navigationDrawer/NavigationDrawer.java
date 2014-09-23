
package com.photosynq.app.navigationDrawer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.photosynq.app.BluetoothActivity;
import com.photosynq.app.LoginActivity;
import com.photosynq.app.ProjectListActivity;
import com.photosynq.app.R;
import com.photosynq.app.StreamlinedModeActivity;
import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.model.AppSettings;
import com.photosynq.app.utils.PrefUtils;

public class NavigationDrawer extends ActionBarActivity implements FragmentHome.OnFragmentInteractionListener{
	
	public static final String LAST_POSITION = "LAST_POSITION";
    private int lastPosition = 0;
	private ListView listDrawer;    
	private int counterItemDownloads;
	protected DrawerLayout layoutDrawer;		
	private LinearLayout linearDrawer;
	private RelativeLayout userDrawer;
	private NavigationAdapter navigationAdapter;
	private ActionBarDrawerToggleCompat drawerToggle;
	private String mEmail;
	private TextView user_email;
    private boolean desktopflag = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean finish = getIntent().getBooleanExtra("finish", false);
        if (finish) {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, new FragmentHome()).commit();

        getSupportActionBar().setIcon(R.drawable.ic_launcher);
		setContentView(R.layout.navigation_drawer);		
		
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);		
        
        mEmail = PrefUtils.getFromPrefs(getApplicationContext() , PrefUtils.PREFS_LOGIN_USERNAME_KEY, PrefUtils.PREFS_DEFAULT_VAL);
        user_email = (TextView) findViewById(R.id.userEmail);
        user_email.setText(mEmail);

		listDrawer = (ListView) findViewById(R.id.listDrawer);        
		linearDrawer = (LinearLayout) findViewById(R.id.linearDrawer);		
		layoutDrawer = (DrawerLayout) findViewById(R.id.layoutDrawer);	
		
		userDrawer = (RelativeLayout) findViewById(R.id.userDrawer);
		userDrawer.setOnClickListener(userOnClick);
		
		if (listDrawer != null) {
			navigationAdapter = NavigationAdapter.getNavigationAdapter(this);
		}

		listDrawer.setAdapter(navigationAdapter);
		listDrawer.setOnItemClickListener(new DrawerItemClickListener());

		drawerToggle = new ActionBarDrawerToggleCompat(this, layoutDrawer);		
		layoutDrawer.setDrawerListener(drawerToggle);
       		
		//user sign out after click on signout option from navigation drawer.
        TextView sign_out=(TextView) findViewById(R.id.signOut);
        sign_out.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                	//Delete user preferences(Credentials).
                	SharedPreferences settings =  PreferenceManager.getDefaultSharedPreferences(getBaseContext());                          
    		        SharedPreferences.Editor editor = settings.edit();
    		        editor.clear();
    		        editor.commit();
    		        Intent intent = new Intent(getApplicationContext(),NavigationDrawer.class);
    		        intent.putExtra("finish", true);
	            	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	                startActivity(intent);
	                finish();	
                }
            });
	
        
	}

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
	        @Override
	        public void onItemClick(AdapterView<?> parent, View view, int posicao, long id) {          	        	
		    	setLastPosition(posicao);        	
		    	setFragmentList(lastPosition);	  
		    	layoutDrawer.closeDrawer(linearDrawer);	    	
	        }
	    }	
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub		
		super.onSaveInstanceState(outState);		
		outState.putInt(LAST_POSITION, lastPosition);					
	}


	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);	     
	    drawerToggle.syncState();	
	 }	
	
	public void setTitleActionBar(CharSequence informacao) {    	
    	getSupportActionBar().setTitle(informacao);
    }	
	
	public void setSubtitleActionBar(CharSequence informacao) {    	
    	getSupportActionBar().setSubtitle(informacao);
    }	

	public void setIconActionBar(int icon) {    	
    	getSupportActionBar().setIcon(icon);
    }	
	
	public void setLastPosition(int posicao){		
		this.lastPosition = posicao;
	}	
		
	private class ActionBarDrawerToggleCompat extends ActionBarDrawerToggle {

		public ActionBarDrawerToggleCompat(Activity mActivity, DrawerLayout mDrawerLayout){
			super(
			    mActivity,
			    mDrawerLayout, 
  			    R.drawable.ic_drawer,
  			    0,0);
		}
		
		@Override
		public void onDrawerClosed(View view) {			
			supportInvalidateOptionsMenu();				
		}

		@Override
		public void onDrawerOpened(View drawerView) {	
			navigationAdapter.notifyDataSetChanged();			
			supportInvalidateOptionsMenu();			
		}		
	}
		  
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);		
	}
	
   
    
	private OnClickListener userOnClick = new OnClickListener() {		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			layoutDrawer.closeDrawer(linearDrawer);
		}
	};	
	
	private void setFragmentList(int position){
		
		FragmentManager fragmentManager = getSupportFragmentManager();
		switch (position) {

            case 0:

                fragmentManager.beginTransaction().replace(R.id.content_frame, new FragmentHome()).commit();
                break;
            case 1:
                fragmentManager.beginTransaction().replace(R.id.content_frame, new FragmentMode()).commit();
                break;
            case 2:
                fragmentManager.beginTransaction().replace(R.id.content_frame, new FragmentUser()).commit();
                break;
            case 3:
                fragmentManager.beginTransaction().replace(R.id.content_frame, new FragmentConnection()).commit();
                break;
            case 4:
                fragmentManager.beginTransaction().replace(R.id.content_frame, new FragmentProjectList()).commit();
                break;
            case 5:
                fragmentManager.beginTransaction().replace(R.id.content_frame, new FragmentData()).commit();
                break;
            case 6:
                fragmentManager.beginTransaction().replace(R.id.content_frame, new FragmentReview()).commit();
                break;
            case 7:
                fragmentManager.beginTransaction().replace(R.id.content_frame, new FragmentSync()).commit();
                break;
		}			
		//show selection of navigation drawer item.(set selected item color is dark).
		//our navigation contain 7 elements i.e we check here with 7
		if (position < 8){
			navigationAdapter.resetarCheck();			
			navigationAdapter.setChecked(position, true);
		}
	}

	public void setTitleFragments(int position){	
		setIconActionBar(Utils.iconNavigation[position]);
		setSubtitleActionBar(Utils.getTitleItem(NavigationDrawer.this, position));				
	}

	public int getCounterItemDownloads() {
		return counterItemDownloads;
	}

	public void setCounterItemDownloads(int value) {
		this.counterItemDownloads = value;
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) 
	{
		super.onActivityResult(requestCode, resultCode, intent);
		mEmail = PrefUtils.getFromPrefs(getApplicationContext() , PrefUtils.PREFS_LOGIN_USERNAME_KEY, PrefUtils.PREFS_DEFAULT_VAL);
		user_email.setText(mEmail);

	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item);
        }

    public void listResearchProjects(View view)
    {
        DatabaseHelper db = DatabaseHelper.getHelper(getApplicationContext());
        String userId = PrefUtils.getFromPrefs(getApplicationContext() , PrefUtils.PREFS_LOGIN_USERNAME_KEY, PrefUtils.PREFS_DEFAULT_VAL);
        AppSettings appSettings = db.getSettings(userId);
        if(appSettings.getModeType().equals(Utils.APP_MODE_NORMAL))
        {
            Intent intent = new Intent(getApplicationContext(),ProjectListActivity.class);
            intent.putExtra(Utils.APP_MODE, Utils.APP_MODE_NORMAL);
            startActivity(intent);
        }
        else if(appSettings.getModeType().equals(Utils.APP_MODE_STREAMLINE))
        {
            Intent intent = new Intent(getApplicationContext(),StreamlinedModeActivity.class);
            startActivity(intent);
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Select mode type first", Toast.LENGTH_LONG).show();
        }
    }
    public void quickMeasurement(View view)
    {
        Intent intent = new Intent(getApplicationContext(),BluetoothActivity.class);
        intent.putExtra(Utils.APP_MODE, Utils.APP_MODE_QUICK_MEASURE);
        startActivity(intent);
    }

}