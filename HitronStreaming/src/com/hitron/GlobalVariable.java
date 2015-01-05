package com.hitron;

import com.hitron.nsd.ChatConnection;
import com.hitron.nsd.NsdHelper;

import android.app.Application;

public class GlobalVariable extends Application {
    public NsdHelper mNsdHelper;
    public ChatConnection mConnection;
    public boolean bRemoteControl = false;
}
