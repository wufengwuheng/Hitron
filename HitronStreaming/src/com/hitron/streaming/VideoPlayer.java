package com.hitron.streaming;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import com.hitron.streaming.R;

import android.widget.ImageButton;
//public class VideoPlayer extends SherlockActivity {
public class VideoPlayer extends Activity {


	/**
	 * TODO: Set the path variable to a streaming video URL or a local media file
	 * path.
	 */
	private String path = "";
	private VideoView mVideoView;
	private Uri uri;
	private ImageButton ibtn_rec;
	private ImageView im_hitronlogo;
	boolean Recording=false;
	boolean isRecordingComplete=true;
	Timer recording_timer = new Timer();
	boolean live_streaming = false;
	
	String videoLocatorId = "";
	private String vgwIP = "";
	private int vgwHttpPort ;
	private int vgwRecordingsPort ;
	
	ArrayList<String> jaStringList = new ArrayList<String>();
	private static final String TAG_SCHEDULERECORDING = "scheduleRecordings";
	private static final String TAG_STATUS = "status";
	
	JSONArray recordingResult = null;	

	public class MyVideoView extends VideoView {
	    public MyVideoView(Context context)
	    {
	        super(context);
	    }
	    public MyVideoView(Context context, AttributeSet attrs)
	    {
	        super(context, attrs);
	    }
	    public MyVideoView(Context context, AttributeSet attrs, int defStyle)
	    {
	        super(context, attrs, defStyle);
	    }

	    public void SetOnResizeListener(MyOnResizeListener orlExt)
	    {
	        orl = orlExt;
	    }	    

	    @Override
	    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld)
	    {
	        super.onSizeChanged(xNew, yNew, xOld, yOld);

			Toast.makeText(VideoPlayer.this, xNew+","+  yNew+","+  
					xOld+"," + yOld, Toast.LENGTH_LONG).show();
	    }
	    MyOnResizeListener orl = null;
	}
	public class MyOnResizeListener
	 {
	    public MyOnResizeListener(){}

	    public void OnResize(int id, int xNew, int yNew, int xOld, int yOld){}
	 }	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (!LibsChecker.checkVitamioLibs(this))
			return;
		setContentView(R.layout.activity_videoplayer);
		mVideoView = (VideoView) findViewById(R.id.surface_view);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			path = extras.getString("videoUri");
			live_streaming = extras.getBoolean("live_streaming", false);
			vgwIP = extras.getString("vgwIP");
			vgwHttpPort = extras.getInt("vgwHttpPort");
			vgwRecordingsPort = extras.getInt("vgwRecordingsPort");
			videoLocatorId = extras.getString("videoLocatorId");						
		}

		
		ibtn_rec = (ImageButton) findViewById(R.id.ibtn_rec1);
		im_hitronlogo = (ImageView) findViewById(R.id.ibtn_hitronlogo);
		
		android.view.WindowManager.LayoutParams p = getWindow().getAttributes();
		
	    //Returns the size of the entire window, including status bar and title.
	    DisplayMetrics dm = new DisplayMetrics();
	    this.getWindowManager().getDefaultDisplay().getMetrics(dm);
	    int winHeight =  dm.heightPixels ;
	    int winwidth = dm.widthPixels ;
	    int statusbarHeigth = getStatusBarHeight() ;

		ibtn_rec.setX(10);
		ibtn_rec.setY((winHeight-statusbarHeigth)/2);
		//ibtn_rec.setY(0);
		im_hitronlogo.setX(winwidth - 330);
		im_hitronlogo.setY(statusbarHeigth + 30);			    
		
		if (live_streaming)
			ibtn_rec.setVisibility(View.VISIBLE);
		else
			ibtn_rec.setVisibility(View.INVISIBLE);

	    	ibtn_rec.setOnClickListener(new View.OnClickListener() {  
	              
	            @Override  
	            public void onClick(View v) {  
	            	if (Recording) {
	            		 //ibtn_rec.setImageResource(R.drawable.record_start);
	            		 //Recording = false;
	            		 //new GetRecordingResult().execute();
	            	} else {
	            		 Recording = true ;
	            		 new GetRecordingResult().execute(); 
	            	}
	            		
	               
	            }  
	            
	        });  

		
		if (path == "") {
			// Tell the user to provide a media file URL/path.
			Toast.makeText(VideoPlayer.this, "Please set path" + " variable to your media file URL/path", Toast.LENGTH_LONG).show();
			return;
		} else {
			/*
			 * Alternatively,for streaming media you can use
			 * mVideoView.setVideoURI(Uri.parse(URLstring));
			 */

			uri = Uri.parse(path);
			mVideoView.setVideoURI(uri);
			mVideoView.setMediaController(new MediaController(this));
			mVideoView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					ibtn_rec.setX(300);
					ibtn_rec.setY(300);
					Toast.makeText(VideoPlayer.this, "Click event!!!", Toast.LENGTH_LONG).show();
					
				}
			}); 
			mVideoView.requestFocus();
			mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mediaPlayer) {
					// optional need Vitamio 4.0
					mediaPlayer.setPlaybackSpeed(1.0f);
				}
			});
		}

	}
	
	//Get the length of the status bar
	public int getStatusBarHeight() { 
	      int result = 0;
	      int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
	      if (resourceId > 0) {
	          result = getResources().getDimensionPixelSize(resourceId);
	      } 
	      return result;
	} 

	  public class recordingTimerTask extends TimerTask
	  {
	    public void run()	    
	    {
	    	new GetRecordingResult().execute();
	    }
	  };
	  
	
	class GetRecordingResult extends AsyncTask<Void, String, String> {  
		
        @Override  
        protected void onCancelled() {  
        }  
        @Override  
        protected void onPostExecute(String result) { 

        	jaStringList.clear();
        	
     
            JSONObject json;
            JSONObject json_result;
            
            if (!isRecordingComplete) {
	       		 ibtn_rec.setImageResource(R.drawable.record_start);
	       		 ibtn_rec.setEnabled(true);
	       		 isRecordingComplete = true ;
	       		 Recording = false;
	       		recording_timer.purge();
            	if (result.indexOf("status=1(Done)") >= 0) {
            		Toast.makeText(getApplicationContext(), "Stop recording!!", Toast.LENGTH_LONG).show(); 
            		}
            	else {
            		//recording_timer.schedule(new recordingTimerTask(), 20000);
            	
            	}
            	return;
            }
    		try {
    			String[] content = result.split("<body>\n");
    			String[] array_delRecordingsResult = content[1].split(" Connection closed by foreign host");
    			json = new JSONObject(array_delRecordingsResult[0]);
	    			
    			json_result = json.getJSONObject(TAG_SCHEDULERECORDING);
    			if (json_result.getString(TAG_STATUS).equalsIgnoreCase("OK")){
			       		 ibtn_rec.setImageResource(R.drawable.recording);
			       		 ibtn_rec.setEnabled(false);
			       		 isRecordingComplete = false ;
			       		 recording_timer.schedule(new recordingTimerTask(), 70000);
    				
    					Toast.makeText(getApplicationContext(), "Start recording!!", Toast.LENGTH_LONG).show();
    			}
    			else
	    			Toast.makeText(getApplicationContext(), "Failed to start recording!!", Toast.LENGTH_LONG).show();
    			
    		} catch (JSONException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
            
        }  
        @Override  
        protected void onPreExecute() {  
        }
		@Override
		protected String doInBackground(Void... params) {
		    InputStream is = null;
		    
		    String json = "";
			
        // Making HTTP request
        try {
            // defaultHttpClient
        	DefaultHttpClient httpClient = new DefaultHttpClient();
        	HttpGet request = new HttpGet();
        	
        	if (!isRecordingComplete) {
        		request.setURI(new URI("http://"+vgwIP+":"+vgwRecordingsPort+"/vldms/info/recordings"));
        	}
        	else if (Recording)
        	//if (Recording)
        		request.setURI(new URI("http://"+vgwIP+":"+vgwHttpPort+"/startRecording.sh?locator="+videoLocatorId));
        	else
        		request.setURI(new URI("http://"+vgwIP+":"+vgwHttpPort+"/deleteRecording.sh?recordingId=all"));
        		
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
            //BufferedReader reader = new BufferedReader(new InputStreamReader(
            //        is, "iso-8859-1"), 8);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    is, "Big5"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            json = sb.toString();
        } catch (Exception e) {
            Log.e("Buffer Error", "Error converting result " + e.toString());
        }
      
			return json;
		}  
		
	}	
}