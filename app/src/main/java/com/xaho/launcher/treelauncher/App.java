package com.xaho.launcher.treelauncher;

import android.graphics.drawable.Drawable;

/**
 * Created by Xaho on 1-4-2015.
 */
public class App implements Comparable<App>{
    CharSequence name;
    Drawable icon;
    String packageName;

    @Override
    public int compareTo(App another) {
        return this.name.toString().compareTo(another.name.toString());
    }
}
