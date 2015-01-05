package com.hitron;

import java.io.BufferedReader;
import java.io.InputStream;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import com.hitron.streaming.MainActivity;

public class FirstActivity extends Activity {
	InputStream instream;
	BufferedReader buffreader;
	String credentials = "";
	String authorization = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		Intent intent = new Intent(); 
	    intent.setClass(FirstActivity.this, HitronFirst.class);
        startActivityForResult(intent,0); 
	}
    public void onActivityResult(int requestCode, int resultCode, Intent data) {  
	    Intent intent = new Intent(); 
	    intent.setClass(FirstActivity.this, MainActivity.class);
	    startActivity(intent);
	    finish();
    }  	
}
