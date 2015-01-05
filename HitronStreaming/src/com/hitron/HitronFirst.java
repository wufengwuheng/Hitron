package com.hitron;

import java.util.Timer;
import java.util.TimerTask;

import com.hitron.streaming.R;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.LinearLayout;

public class HitronFirst extends Activity {
	Timer countdown_timer = new Timer();
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.hitronfirst);
        LinearLayout linearlayout = (LinearLayout) findViewById(R.id.myLinearLayout);
        Resources res = this.getResources();
        Drawable drawable = res.getDrawable(R.drawable.hitronfirst);
        linearlayout.setBackground(drawable);
        countdown_timer.schedule(new countdownTimerTask(), 3000);
	}	
	
	  public class countdownTimerTask extends TimerTask
	  {
	    public void run()	    
	    {
	    	finish();
	    }
	  };
	

}
