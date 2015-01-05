package com.hitron.streaming;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
//import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.hitron.GlobalVariable;
import com.hitron.streaming.R;
//Felix: for remote control
import com.hitron.nsd.*;

public class MainActivity extends SherlockActivity {
	
	private String vgwHttpIP = "192.168.0.3";
	private int vgwHttpPort  = 50050 ;
	private int vgwRecordingsPort  = 8080 ;
	
	//Felix: for remote control
    NsdHelper mNsdHelper;
    private Handler mUpdateHandler;
    public static final String TAG = "MainActivity";
    ChatConnection mConnection;
   
    GlobalVariable globalVariable ;
	
   
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//live streaming button
		final RelativeLayout x1y1 = (RelativeLayout) findViewById(R.id.x1y1);
		globalVariable = (GlobalVariable) getApplicationContext();
		 
		x1y1.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {		
				if (event.getAction()==MotionEvent.ACTION_DOWN){
					//x1y1.setBackgroundResource(R.drawable.main_button_on);
					x1y1.requestFocus();
				}else if (event.getAction()==MotionEvent.ACTION_UP){
					StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
				    StrictMode.setThreadPolicy(policy);
					if (checkRdkReady()) {
						Intent intent = new Intent();
						intent.setClass(MainActivity.this, StreamingActivity.class);
						intent.putExtra("live_streaming", true);
						startActivity(intent); 
						
					}
					else {
							Toast.makeText(getApplicationContext(), "The Video Gateway is not ready. Please check it!", Toast.LENGTH_LONG).show();
						}
				}
				return true;
			}
		});
		
		//playback button
		final RelativeLayout x2y1 = (RelativeLayout) findViewById(R.id.x2y1);
		x2y1.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction()==MotionEvent.ACTION_DOWN){
					//x2y1.setBackgroundResource(R.drawable.main_button_on);
					x2y1.requestFocus();
				}else if (event.getAction()==MotionEvent.ACTION_UP){
					StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
				    StrictMode.setThreadPolicy(policy);
					if (checkRdkReady()) {
						Intent intent = new Intent();
						intent.setClass(MainActivity.this, StreamingActivity.class);
						startActivity(intent); 						
					}
					else {
							Toast.makeText(getApplicationContext(), "The Video Gateway is not ready. Please check it!", Toast.LENGTH_LONG).show();
					}
				}
				return true;
			}
		});
		//Felix: for remote control
		
		x1y1.requestFocus(); 
		//Felix: for remote control
		if (globalVariable.bRemoteControl) {
		        mUpdateHandler = new Handler() {
		            @Override
		        public void handleMessage(Message msg) {
		            String chatLine = msg.getData().getString("msg");
		            if (chatLine.equals("left")) {
		            	getCurrentFocus().focusSearch(View.FOCUS_LEFT).requestFocus();
		            } else if (chatLine.equals("right")) {
		            	getCurrentFocus().focusSearch(View.FOCUS_RIGHT).requestFocus();
		            	
		            } else if (chatLine.equals("ok")) {
		            	getCurrentFocus().callOnClick();           	
		            } else if (chatLine.equals("volumeup")) {
		            	AudioManager mgr;
		            	mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		            	setVolumeControlStream(AudioManager.STREAM_MUSIC);
		            	mgr.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);             	         	
		            } else if (chatLine.equals("volumedown")) {
		            	AudioManager mgr;
		            	mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		            	setVolumeControlStream(AudioManager.STREAM_MUSIC);
		            	mgr.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
		            } 
		        }
		    };		
		    
		    mConnection = new ChatConnection(mUpdateHandler);
		    mNsdHelper = new NsdHelper(this);
		    
		    //Felix:for remote control: set global variables
		    globalVariable.mConnection = mConnection;  
		    globalVariable.mNsdHelper = mNsdHelper; 
		    
		    mNsdHelper.initializeNsd();
		    if(mConnection.getLocalPort() > -1) {
		        mNsdHelper.registerService(mConnection.getLocalPort());
		    } else {
		        Log.d(TAG, "ServerSocket isn't bound.");
		    }

	}

    PackageInfo pinfo;
		String versionName = "";
		try {
			pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionName = pinfo.versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		TextView version_text = (TextView) findViewById(R.id.version_text);
		version_text.setTextColor(Color.GREEN) ;
		version_text.setText(getResources().getString(R.string.version)+ versionName);		
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
        	finish();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }    
    
    @Override
    protected void onDestroy() {
    	
    	if (globalVariable.bRemoteControl) {
	        mNsdHelper.tearDown();
	        mConnection.tearDown();
    	}
        super.onDestroy();
    }
    
    @Override
    public void onResume()
    {
    	if (globalVariable.bRemoteControl)
    		mConnection.mUpdateHandler = mUpdateHandler;
    	
        super.onResume();
        Log.v(TAG,"onResume");
    }
    
    public void clickPlayback(View v) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	    StrictMode.setThreadPolicy(policy);
		if (checkRdkReady()) {
			Intent intent = new Intent();
			intent.setClass(MainActivity.this, StreamingActivity.class);
			startActivity(intent); 
			mConnection.mUpdateHandler = mUpdateHandler;
		}
		else {
				Toast.makeText(getApplicationContext(), "The Video Gateway is not ready. Please check it!", Toast.LENGTH_LONG).show();
		}
    }
    public void clickLiveStreaming(View v) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	    StrictMode.setThreadPolicy(policy);
		if (checkRdkReady()) {
			Intent intent = new Intent();
			intent.setClass(MainActivity.this, StreamingActivity.class);
			intent.putExtra("live_streaming", true);
			startActivity(intent); 
			mConnection.mUpdateHandler = mUpdateHandler;
		}
		else {
				Toast.makeText(getApplicationContext(), "The Video Gateway is not ready. Please check it!", Toast.LENGTH_LONG).show();
			}    }
	
	
	@Override 
	public void onBackPressed(){ 
		moveTaskToBack(true); 
	}
	protected boolean checkRdkReady() {
		
	    InputStream is = null;
	    
	    String serverResponse = "";
	    
    // Making HTTP request
    try {
    	DefaultHttpClient httpClient = new DefaultHttpClient();
    	HttpGet request = new HttpGet();
    	request.setURI(new URI("http://"+vgwHttpIP+":"+vgwHttpPort));
        HttpResponse httpResponse = httpClient.execute(request);
        HttpEntity httpEntity = httpResponse.getEntity();
        is = httpEntity.getContent();    

    } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
    } catch (ClientProtocolException e) {
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    } catch (URISyntaxException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

    try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                is, "Big5"), 8);
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        is.close();
        serverResponse = sb.toString();
    } catch (Exception e) {
        Log.e("Buffer Error", "Error converting result " + e.toString());
    }
    
		return serverResponse.indexOf("Welcome") >= 0;
	}	

}
