package com.photosynq.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.model.Protocol;
import com.photosynq.app.model.ResearchProject;
import com.photosynq.app.utils.BluetoothService;
import com.photosynq.app.utils.CommonUtils;

public class ResultActivity extends ActionBarActivity {
	
	 private static final String TAG = "BluetoothChat";
	    private static final boolean D = true;
	    
	    // Name of the connected device
	    private String mConnectedDeviceName = null;
	    
	  // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_STOP = 6;
    
    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    
    
	//private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	private BluetoothService mBluetoothService = null;
	private BluetoothAdapter mBluetoothAdapter = null;
	private String projectId;
	private String deviceAddress;
	private TextView mStatusLine;
	private String protocolJson="";
	private String options="";
	
	DatabaseHelper db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_result);
		
		mStatusLine = (TextView) findViewById(R.id.statusMessage);
 		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			projectId = extras.getString(DatabaseHelper.C_PROJECT_ID)!=null?extras.getString(DatabaseHelper.C_PROJECT_ID):"";
			deviceAddress = extras.getString(BluetoothService.DEVICE_ADDRESS);
			protocolJson = extras.getString(DatabaseHelper.C_PROTOCOL_JSON)!=null?extras.getString(DatabaseHelper.C_PROTOCOL_JSON):"";
			options = extras.getString(DatabaseHelper.C_OPTION_TEXT)!=null?extras.getString(DatabaseHelper.C_OPTION_TEXT):"";
		}		
		

		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}
		if (mBluetoothService == null) {
			mBluetoothService = new BluetoothService(getApplicationContext(), mHandler);
		}
		 // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        mBluetoothService.connect(device);
	}


    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth  services
        if (mBluetoothService != null) mBluetoothService.stop();
    }
    
    private void sendData(String data) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(getApplicationContext(),"Not Connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (data.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send;
				send = data.getBytes();
				 mBluetoothService.write(send);
            //byte[] bytes = ByteBuffer.allocate(4).putInt(9).array();
            
           
        }
    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.result, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
//		int id = item.getItemId();
//		if (id == R.id.action_settings) {
//			return true;
//		}
		return super.onOptionsItemSelected(item);
	}
	
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                	mStatusLine.setText(R.string.title_connected_to);
                	mStatusLine.append(mConnectedDeviceName);
                	if(protocolJson.length() == 0)
                	{
                		System.out.println("Setting default protocol ");
                		db = new DatabaseHelper(getApplicationContext());
                		ResearchProject rp =  db.getResearchProject(projectId);
                		String[] protocol_ids = rp.getProtocols_ids().trim().split(",");
                		if(rp.getProtocols_ids().length() >=1)
                		{
	                		for (String protocol_id : protocol_ids) {
	                			Protocol protocol = db.getProtocol(protocol_id);
	                			System.out.println("######## protocol :"+protocol.getProtocol_json());
	                			if(protocol.getProtocol_json().trim().length() > 1)
	                			{
	                				protocolJson +=  protocol.getProtocol_json().trim().substring(1, protocol.getProtocol_json().trim().length()-1)+",";
	                			}
							}
	                		protocolJson = "["+protocolJson.substring(0, protocolJson.length()-1) +"]"; // remove last comma and add suqare brackets and start and end.
	                		
	                		System.out.println("$$$$$$$$$$$$$$ protocol json sending to evice :"+protocolJson);
	                		db.closeDB();
	                		//String obj = "[{\"environmental\":[[\"light_intensity\",0]],\"tcs_to_act\":100,\"protocol_name\":\"baseline_sample\",\"protocols_delay\":5,\"act_background_light\":20,\"actintensity1\":5,\"actintensity2\":5,\"averages\":1,\"wait\":0,\"cal_true\":2,\"analog_averages\":1,\"pulsesize\":10,\"pulsedistance\":3000,\"calintensity\":255,\"pulses\":[400],\"detectors\":[[34]],\"measlights\":[[14]]},{\"tcs_to_act\":100,\"environmental\":[[\"relative_humidity\",0],[\"temperature\",0],[\"light_intensity\",0]],\"protocols_delay\":5,\"act_background_light\":20,\"protocol_name\":\"fluorescence\",\"baselines\":[1,1,1,1],\"averages\":1,\"wait\":0,\"cal_true\":0,\"analog_averages\":1,\"act_light\":20,\"pulsesize\":10,\"pulsedistance\":10000,\"actintensity1\":5,\"actintensity2\":50,\"measintensity\":7,\"calintensity\":255,\"pulses\":[50,50,50,50],\"detectors\":[[34],[34],[34],[34]],\"measlights\":[[15],[15],[15],[15]],\"act\":[0,1,0,0]},{\"protocol_name\":\"chlorophyll_spad_ndvi\",\"baselines\":[0,0,0,0],\"environmental\":[[\"relative_humidity\",1],[\"temperature\",1],[\"light_intensity\",1]],\"measurements\":1,\"measurements_delay\":1,\"averages\":1,\"wait\":0,\"cal_true\":0,\"analog_averages\":1,\"pulsesize\":20,\"pulsedistance\":3000,\"actintensity1\":8,\"actintensity2\":8,\"measintensity\":80,\"calintensity\":255,\"pulses\":[100],\"detectors\":[[34,35,35,34]],\"measlights\":[[12,20,12,20]]}]";
	                		//	String protocol= "[{\"protocol_name\":\"fluorescence\",\"baselines\":[1,1,1,1],\"averages\":1,\"wait\":0,\"cal_true\":0,\"analog_averages\":12,\"act_light\":20,\"pulsesize\":50,\"pulsedistance\":3000,\"actintensity1\":100,\"actintensity2\":100,\"measintensity\":3,\"calintensity\":255,\"pulses\":[50,50,50,50],\"detectors\":[[34],[34],[34],[34]],\"measlights\":[[15],[15],[15],[15]],\"act\":[2,1,2,2]}]";
	                		sendData(protocolJson);
                		}
                		else{
                				mStatusLine.setText("No protocol defined for this project.");
                				Toast.makeText(getApplicationContext(), "No protocol defined for this project.", Toast.LENGTH_LONG).show();
                				break;
                			}
                	}else
                	{
                		//change this once you get actual protocol
                		//String obj = "[{\"measurements\":2,\"protocol_name\":\"baseline_sample\",\"averages\":1,\"wait\":0,\"cal_true\":2,\"analog_averages\":1,\"pulsesize\":10,\"pulsedistance\":3000,\"actintensity1\":1,\"actintensity2\":1,\"measintensity\":255,\"calintensity\":255,\"pulses\":[400],\"detectors\":[[34]],\"measlights\":[[14]]},{\"measurements\":2,\"protocol_name\":\"fluorescence\",\"baselines\":[1,1,1,1],\"environmental\":[[\"relative_humidity\",1],[\"temperature\",1]],\"averages\":2,\"wait\":0,\"cal_true\":0,\"analog_averages\":1,\"act_light\":20,\"pulsesize\":10,\"pulsedistance\":10000,\"actintensity1\":100,\"actintensity2\":100,\"measintensity\":3,\"calintensity\":255,\"pulses\":[50,50,50,50],\"detectors\":[[34],[34],[34],[34]],\"measlights\":[[15],[15],[15],[15]],\"act\":[2,1,2,2]}]";
                		sendData(protocolJson);
                	}
                    
                    mStatusLine.setText("Initializing measurement . .");

                	
                    break;
                case BluetoothService.STATE_CONNECTING:
                	mStatusLine.setText(R.string.title_connecting);
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                	mStatusLine.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                //byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                //String writeMessage = new String(writeBuf);
                break;
            case MESSAGE_READ:
               // byte[] readBuf = (byte[]) msg.obj;
            	StringBuffer measurement = (StringBuffer)msg.obj;
                // construct a string from the valid bytes in the buffer
               // String readMessage = new String(readBuf, 0, msg.arg1);
                mStatusLine.setText("Receiving data from device");
                String dataString;
                long time= System.currentTimeMillis();
                if (options.equals(""))
                {
                	 dataString = "var data = [\n"+measurement.toString().replaceAll("\\r\\n", "").replaceAll("\\{", "{\"time\":\""+time+"\",")+"\n];";
                }
                else
                {
                	 dataString = "var data = [\n"+measurement.toString().replaceAll("\\r\\n", "").replaceFirst("\\{", "{"+options).replaceAll("\\{", "{\"time\":\""+time+"\",")+"\n];";
                }
                CommonUtils.writeStringToFile(getApplicationContext(), "data.js", dataString);
                mBluetoothService.stop();
                Intent intent = new Intent(getApplicationContext(),DisplayResultsActivity.class);
        		intent.putExtra(DatabaseHelper.C_PROJECT_ID, projectId);
        		intent.putExtra(DatabaseHelper.C_PROTOCOL_JSON, protocolJson);
        		String reading = measurement.toString().replaceAll("\\r\\n", "").replaceFirst("\\{", "{"+options).replaceAll("\\{", "{\"time\":\""+time+"\",");
        		//reading = reading.replaceFirst("\\{", "{"+options);
        		intent.putExtra(DatabaseHelper.C_READING, reading);
        		startActivity(intent);
        		finish();
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_STOP:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                mBluetoothService.stop();
                break;
            }
        }
    };
}