package com.hitron.streaming;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.hitron.GlobalVariable;
import com.hitron.nsd.ChatConnection;
import com.hitron.streaming.R;

public class StreamingActivity extends SherlockActivity {
	//Felix: for debug
    boolean bDEBUG = false ;
    
	boolean external_player = true ; 
	
	StreamingAdapter streamingAdapter;
	GridView streamingGridView;
	boolean select_all_flag = false;
	ArrayList<String> jaStringList = new ArrayList<String>();
	SharedPreferences prefs;
	private String vgwHttpIP = "192.168.0.3";
	private int vgwHttpPort  = 50050 ;
	private int vgwRecordingsPort  = 8080 ;
	
	
	boolean log_changed = false;
	
	boolean live_streaming = false ;	
	
	boolean clear_recordings = false ;
	
	//Felix 20131129: parse the channel(JSON file) from http
    //JSON Node Names
    private static final String TAG_CHANNELS = "channels";
    private static final String TAG_TYPE = "type";
    private static final String TAG_TITLE = "title";
    private static final String TAG_NAME = "name";
    private static final String TAG_PATH = "path";
    private static final String TAG_LOCATOR = "locator";
    private static final String TAG_ALTERNATEURI = "alternateUri";
    private static final String TAG_HIDDEN = "hidden";
    private static final String TAG_ICON = "icon";
	private static final String TAG_DELETERECORDING = "deleteRecordingsSyncResponse";
	private static final String TAG_STATUS = "status";
	
	JSONArray recordingResult = null;	
    
    private static final boolean FROM_HTTP = true ;
    
    JSONArray channel = null;	
    
    //Felix: for remote control
    private Handler mUpdateHandler;
    private static final int PLAY_STREAMING_REQUEST = 1 ;
    ChatConnection mConnection;
    
    GlobalVariable globalVariable;
    
    //Felix: recording for external player
	boolean Recording=false;
	boolean isRecordingComplete=true;
	Timer recording_timer = new Timer();
	Timer Toasttimer = new Timer();
	private String vgwIP = "";
	private static final String TAG_SCHEDULERECORDING = "scheduleRecordings";
	String videoLocatorId="";
	boolean bExternalPlayerPlaying = false;
	Toast toast;
	int RECORDING_DURATION = 60000;

    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.options, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    	case android.R.id.home:
	    		// app icon in action bar clicked; go home
	    		finish();
	    		break;
	    	case R.id.option_button:  
	    		break;
	    	case R.id.clear_log_button:
	    		ClearRecordings();
	    		break;
	    }
	    return true;
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_gridview);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true); 
		
		globalVariable = (GlobalVariable) getApplicationContext();

	    //Felix: for remote control
		if (globalVariable.bRemoteControl) {
	        mUpdateHandler = new Handler() {
	            @Override
	        public void handleMessage(Message msg) {
	            String chatLine = msg.getData().getString("msg");
	            if (chatLine.equals("left")) {
	            try {
	            	getCurrentFocus().focusSearch(View.FOCUS_LEFT).requestFocus();
	            } catch (Exception e) {
	            	try {
	            		Log.d("Focus","FOCUS_BACKWARD");
	            		getCurrentFocus().focusSearch(View.FOCUS_BACKWARD).requestFocus();               		
	            	}
	            	catch (Exception e1) {
	            		Log.d("Focus execption","left");
	            	}
	            	}
	
	            } else if (chatLine.equals("right")) {
	            try {
	            	getCurrentFocus().focusSearch(View.FOCUS_RIGHT).requestFocus();
	            } catch (Exception e) {
	            	try {
	            		Log.d("Focus","FOCUS_FORWARD");
	            		getCurrentFocus().focusSearch(View.FOCUS_FORWARD).requestFocus();               		
	            	}
	            	catch (Exception e1) {
	            		Log.d("Focus execption","right");
	            	}
	            }
	            	
	            } else if (chatLine.equals("ok")) {
	            	getCurrentFocus().callOnClick();           	
	            } else if (chatLine.equals("back")) {
	            	ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
	            	List<RunningTaskInfo> list = am.getRunningTasks(100);
	            	boolean isAppRunning = false;
	            	String MY_PKG_NAME = "com.mxtech.videoplayer.ad";
	            	for (RunningTaskInfo info : list) {
	            
	            	if (info.topActivity.getPackageName().equals(MY_PKG_NAME) || info.baseActivity.getPackageName().equals(MY_PKG_NAME)) {
	            	isAppRunning = true;
	            	Log.i("StreamingActivity",info.toString()+info.topActivity.getPackageName() + " info.baseActivity.getPackageName()="+info.baseActivity.getPackageName());
	            	break;
	            	}
	            	}  
	            	if (isAppRunning) {
	            		String cmd="am force-stop com.mxtech.videoplayer.ad";  
	            		Log.d("Kill Process", "am force-stop com.mxtech.videoplayer.ad");
	            	    try {  
	            	        Process process = Runtime.getRuntime().exec(new String[] { "su", "-c", cmd });  
	            	        BufferedReader in = new BufferedReader(new InputStreamReader(  
	            	                        process.getInputStream()));  
	            	        String line = null;  
	            	        while ((line = in.readLine()) != null) {  
	            	            System.out.println("exec shell: == " + line);  
	            	        }
	            	        finishActivity(PLAY_STREAMING_REQUEST);
	            	        //Toast.makeText(getApplicationContext(),"Kill process"+"focus:"+getCurrentFocus().toString(), Toast.LENGTH_LONG).show();
	            	        
	            	    } catch (IOException e) {  
	            	        // TODO Auto-generated catch block  
	            	    	Log.d("Kill Process", "Exception: am force-stop com.mxtech.videoplayer.ad");
	            	    	Toast.makeText(getApplicationContext(),"Kill process fail~~~~~"+"focus:"+getCurrentFocus().toString(), Toast.LENGTH_LONG).show();
	            	        e.printStackTrace();  
	            	    }          	            			            		
	            	}            	
	            	else 
	            		finish();          	
	            } else if (chatLine.equals("down")) {
	            	try {
	            		getCurrentFocus().focusSearch(View.FOCUS_DOWN).requestFocus();     
	                } catch (Exception e) {
	                	try {
	                		Log.d("Focus","FOCUS_FORWARD");
	                		getCurrentFocus().focusSearch(View.FOCUS_FORWARD).requestFocus();               		
	                	}
	                	catch (Exception e1) {
	                		Log.d("Focus exception","down");
	                	
	                	}
	                }
	
	            } else if (chatLine.equals("up")) {
	            try {
	            	getCurrentFocus().focusSearch(View.FOCUS_UP).requestFocus();          	
	            } catch (Exception e) {
	            	try {
	            		Log.d("Focus","FOCUS_BACKWARD");
	            		getCurrentFocus().focusSearch(View.FOCUS_BACKWARD).requestFocus();               		
	            	}
	            	catch (Exception e1) {
	            		Log.d("Focus execption","up");
	            	
	            	}
	            }
	            } else if (chatLine.equals("volumeup")) {
	            	AudioManager mgr;
	            	mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	            	setVolumeControlStream(AudioManager.STREAM_MUSIC);
	            	mgr.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);             	         	
	            } else if (chatLine.equals("volumedown")) {
	            	AudioManager mgr;
	            	mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	            	setVolumeControlStream(AudioManager.STREAM_MUSIC);
	            	//mgr.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE, -2); 
	            	mgr.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
	            	
	            } else if (chatLine.equals("recording")) {
	             if (bExternalPlayerPlaying) {
	           		 Recording = true ;
	           		 new GetRecordingResult().execute();
	             }
	            } 
	        }
	    };		
		    //Felix:for remote control: read global variable
		    mConnection = globalVariable.mConnection;      
		    mConnection.mUpdateHandler = mUpdateHandler ;
	}
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			live_streaming = extras.getBoolean("live_streaming", false);
		}
		
		if (live_streaming)
			setTitle(getResources().getString(R.string.other_devices_text));
	    //Felix 20131129 : to solve the http request fail
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	    StrictMode.setThreadPolicy(policy);
	    if (FROM_HTTP) {
	    	new GetStreaming().execute(); //felix 20131201 : Get JSON file by http
	    	return ;
	    }

	    

	}	
	
	public void ClearRecordings(){
		clear_recordings = true;
		new GetStreaming().execute();
	}
	
	class GetStreaming extends AsyncTask<Void, String, String> {  
		
        @Override  
        protected void onCancelled() {  
        }  
        @Override  
        protected void onPostExecute(String result) { 
     		streamingGridView = (GridView) findViewById(R.id.gridView1);
        	jaStringList.clear();
        	
      	
        	if (clear_recordings) {
              
                JSONObject json;
                JSONObject json_result;
        		try {
        			String[] content = result.split("<body>\n");
        			String[] array_delRecordingsResult = content[1].split(" Connection closed by foreign host");
        			json = new JSONObject(array_delRecordingsResult[0]);
        			json_result = json.getJSONObject(TAG_DELETERECORDING);
        			if (json_result.getString(TAG_STATUS).equalsIgnoreCase("OK"))
    	    				Toast.makeText(getApplicationContext(), "Deleted recordings!!", Toast.LENGTH_LONG).show();
    	    			else
    	    				Toast.makeText(getApplicationContext(), "Failed to delete recordings!!", Toast.LENGTH_LONG).show();
        			
        		} catch (JSONException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
        	
        		clear_recordings = false ;
        		return ;
        	}
            // Getting JSON from URL
        	if (!live_streaming) {
   				String[] recordings = result.split("===== Recording ");
        		for(int i = 1 ; i < recordings.length ; i ++){ 
        			String[] array_content = recordings[i].split("\n");
        			String[] array_number = array_content[0].split(" ");  
        			String[] array_contentitem = array_content[1].split("<br />");
        			String[] array_duration = array_contentitem[1].split("Duration : ");
        			String[] array_contentitem1 = array_contentitem[2].split("URL :  <code>");
        			String[] array_uri = array_contentitem1[1].split("</code>");
        			String[] array_id = array_uri[0].split("rec_id=");
        			
                	String listString = ""; 
        				listString = listString + "{\"title\":\"" + "playback" + "\",";
        				listString = listString + "\"name\":\"" + array_id[1] + "\",";
        				listString = listString + "\"number\":\"" + array_number[0] + "\",";
        				listString = listString + "\"playbacktime\":\"" + array_duration[1] + "\",";
        				listString = listString + "\"alternateUri\":\"" + array_uri[0]+ "\",";
        				listString = listString + "\"icon\":\"" + "video.png" + "\"}";
         				jaStringList.add(listString);
        			
        		}       
        		if(jaStringList.size()>0){
        			streamingAdapter = new StreamingAdapter(StreamingActivity.this, jaStringList); 
        			streamingGridView.setAdapter(streamingAdapter);
        		}
        		
        		return ;
        	}
            JSONObject json;
    		try {
    			json = new JSONObject(result);
    			channel = json.getJSONArray(TAG_CHANNELS);
    		} catch (JSONException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
            
            
            for(int m=0; m < channel.length(); m++){
            	String listString = ""; 
            	try {
    				listString = listString + "{\"title\":\"" + channel.getJSONObject(m).getString(TAG_TITLE) + "\",";
    				listString = listString + "\"type\":\"" + channel.getJSONObject(m).getString(TAG_TYPE) + "\",";
    				listString = listString + "\"name\":\"" + channel.getJSONObject(m).getString(TAG_NAME) + "\",";
    				listString = listString + "\"locator\":\"" + channel.getJSONObject(m).getString(TAG_LOCATOR) + "\",";
    				listString = listString + "\"path\":\"" + channel.getJSONObject(m).getString(TAG_PATH) + "\",";
    				listString = listString + "\"alternateUri\":\"" + channel.getJSONObject(m).getString(TAG_ALTERNATEURI)+ "\",";
    				listString = listString + "\"icon\":\"" + channel.getJSONObject(m).getString(TAG_ICON)+ "\",";
    				listString = listString + "\"hidden\":\"" + channel.getJSONObject(m).getString(TAG_HIDDEN) + "\"}";
    				jaStringList.add(listString);
    			} catch (JSONException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
            }
    		
    		
    		if(jaStringList.size()>0){
    			streamingAdapter = new StreamingAdapter(StreamingActivity.this, jaStringList); 
    			streamingGridView.setAdapter(streamingAdapter);
    			
    			//streamingGridView.setStretchMode(GridView.STRETCH_SPACING);
    			//streamingGridView.setSelection(0);
    			//Toast.makeText(getApplicationContext(),"focus:"+getCurrentFocus().toString(), Toast.LENGTH_LONG).show();
    			//getCurrentFocus().
    			
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

        	DefaultHttpClient httpClient = new DefaultHttpClient();
        	HttpGet request = new HttpGet();
        	if (clear_recordings)
        		request.setURI(new URI("http://"+vgwHttpIP+":"+vgwHttpPort+"/deleteRecording.sh?recordingId=all"));
        	else if (live_streaming)
        		request.setURI(new URI("http://"+vgwHttpIP+":"+vgwHttpPort+"/channels.json"));
        	else 
        		request.setURI(new URI("http://"+vgwHttpIP+":"+vgwRecordingsPort+"/vldms/info/recordingurls"));
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
            json = sb.toString();
        } catch (Exception e) {
            Log.e("Buffer Error", "Error converting result " + e.toString());
        }
      
			return json;
		}  
		
	}
	

	public class StreamingAdapter extends BaseAdapter {
		private Context context;
		private final ArrayList<String> jaStringList;
 
		public StreamingAdapter(Context context, ArrayList<String> jaStringList) {
			this.context = context;
			this.jaStringList = jaStringList;
		}
	
		View listView;
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 
			if (convertView == null) {
				listView = new View(context);
				// get layout from bookshelf_gridview_content.xml
				listView = inflater.inflate(R.layout.video_items, null);
			} else {
				listView = (View) convertView;
			}
			try { 
				final JSONObject j_object = new JSONObject(jaStringList.get(position));

				final String channel_name = j_object.getString("name");
				String channel_icon = j_object.getString("icon");

				TextView textView = (TextView) listView.findViewById(R.id.text);
				if (live_streaming) {
					textView.setText(channel_name);
				} else {
					String playback_time = j_object.getString("playbacktime");
					
					//Felix: due to the wrong duration, we skip to show the time
					//textView.setText("Time: " + playback_time + "\n" + "ID: " +channel_name);
					
					textView.setText("ID: " +channel_name);	
				}
			
				ImageView streamingIcon = (ImageView) listView.findViewById(R.id.image);
				
				if (FROM_HTTP) {			
						streamingIcon.setImageBitmap(BitmapFactory.decodeStream(new URL("http://"+vgwHttpIP+":"+vgwHttpPort+"/"+channel_icon).openConnection().getInputStream()));
				}
				LinearLayout griditem = (LinearLayout) listView.findViewById(R.id.girditem);
				if (position==0)
					griditem.requestFocus();
				
				griditem.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View arg0) {
						String path = "";
						arg0.requestFocus();
						if (live_streaming) {							
							String channel_locator="";
							bExternalPlayerPlaying = true ;
							try {
								channel_locator = j_object.getString("locator");
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							//requestFocus();
							if (bDEBUG)
								path = "http://"+vgwHttpIP+":50050/1.mp4" ;	
							else 
								path = "http://"+vgwHttpIP+":"+vgwRecordingsPort+"/vldms/tuner?ocap_locator="+channel_locator ;	
							Toast.makeText(arg0.getContext(),path, Toast.LENGTH_LONG).show();
							playVideoAction(path,channel_locator);
						}
						else {
							String channel_alternateUri="";
							try {
								channel_alternateUri = j_object.getString("alternateUri");
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							path = channel_alternateUri ; 
							Toast.makeText(arg0.getContext(),path, Toast.LENGTH_LONG).show();
							playVideoAction(path,channel_name);
						}
						return ;
											
						
					}}
				);				
			} catch (JSONException e) {  
				throw new RuntimeException(e);  
			} catch (MalformedURLException e) {
				 //TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return listView;
		}
 
		@Override
		public int getCount() {
			return jaStringList.size();
		}
 
		@Override
		public Object getItem(int position) {
			return null;
		}
 
		@Override
		public long getItemId(int position) {
			return 0;
		}
	}
	
	public void playVideoAction(String path, String...control){
		
		
		if (external_player) {
			Intent myIntent = new Intent(Intent.ACTION_VIEW);
			Uri videoUri = Uri.parse(path);
			videoLocatorId= control[0];
	        myIntent.setDataAndType( videoUri, "application/x-mpegURL" );
	        myIntent.setPackage( "com.mxtech.videoplayer.ad" );
	        //startActivity( myIntent );
	        startActivityForResult(myIntent, PLAY_STREAMING_REQUEST);
	        
		} else {
		
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setClass(StreamingActivity.this, VideoPlayer.class);
			intent.putExtra("videoUri", path);
			intent.putExtra("live_streaming", live_streaming);
			intent.putExtra("videoLocatorId", control[0]);
			intent.putExtra("vgwIP", vgwHttpIP);
			intent.putExtra("vgwRecordingsPort", vgwRecordingsPort);
			intent.putExtra("vgwHttpPort", vgwHttpPort);
			startActivity(intent); 
		}


		
	}
	public void onResume () {
		bExternalPlayerPlaying = false;
		super.onResume();
	}
	public void onStop () {
		super.onStop();
	}
	
	//Felix: for recording
	  public class recordingTimerTask extends TimerTask
	  {
	    public void run()	    
	    {
	    	new GetRecordingResult().execute();
	    }
	  };
	  private void execToast(final Toast toast) {
          Toasttimer.schedule(new TimerTask() {

                  @Override
                  public void run() {
                	  int counter =1 ;
                	  int round = RECORDING_DURATION / 2000 ;
                	  while(true){  
                		  if (counter>round)
                				  break;
                		  counter++;
                          toast.show(); 
                          try {Thread.sleep(2000);
                          } catch (InterruptedException e) {
                              // TODO Auto-generated catch block
                              e.printStackTrace();
                          }
                	  }
                	  toast.cancel();    
                  }

          },2000);
  }
	
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
	       		 isRecordingComplete = true ;
	       		 Recording = false;
	       		recording_timer.purge();
	       		Toasttimer.purge();
	       		if (result.indexOf("status=1(Done)") >= 0) {
 	       			toast = Toast.makeText(getApplicationContext(),"Stop recording!!", Toast.LENGTH_LONG);
            		toast.show(); 
            		}
            	else {
	       			toast = Toast.makeText(getApplicationContext(),"Stop recording...", Toast.LENGTH_LONG);
            		toast.show(); 
            	}
            	return;
            }
    		try {
    			String[] content = result.split("<body>\n");
    			String[] array_delRecordingsResult = content[1].split(" Connection closed by foreign host");
    			json = new JSONObject(array_delRecordingsResult[0]);
	    			
    			json_result = json.getJSONObject(TAG_SCHEDULERECORDING);
    			if (json_result.getString(TAG_STATUS).equalsIgnoreCase("OK")){
			       		 isRecordingComplete = false ;
			       		 recording_timer.schedule(new recordingTimerTask(), RECORDING_DURATION);
    				
	    				toast = Toast.makeText(getApplicationContext(),"Recording!!。。。。", Toast.LENGTH_LONG);
	    				toast.show();
	    				execToast(toast);
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
        		request.setURI(new URI("http://"+vgwHttpIP+":"+vgwRecordingsPort+"/vldms/info/recordings"));
        	}
        	else if (Recording)
        	//if (Recording)
        		request.setURI(new URI("http://"+vgwHttpIP+":"+vgwHttpPort+"/startRecording.sh?locator="+videoLocatorId));
        	else
        		request.setURI(new URI("http://"+vgwHttpIP+":"+vgwHttpPort+"/deleteRecording.sh?recordingId=all"));
        		
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
            json = sb.toString();
        } catch (Exception e) {
            Log.e("Buffer Error", "Error converting result " + e.toString());
        }
      
			return json;
		}  
		
	}		
}
