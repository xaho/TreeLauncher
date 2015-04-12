package com.xaho.launcher.treelauncher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppGridActivity extends Activity implements AdapterView.OnItemClickListener {
    private PackageManager manager;
    private List<App> apps;
    private GridView list;
    private Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("AGAlifecycle","onCreate");
        setContentView(R.layout.activity_app_grid);

        loadApps();
        loadListView();
        addClickListener();

        intent = getIntent();
        Log.i("oC",intent.toString());
    }

    private void addClickListener() {
        list.setOnItemClickListener(this);
    }

    private void loadListView() {
        list = (GridView)findViewById(R.id.apps_list);

        ArrayAdapter<App> adapter = new ArrayAdapter<App>(this,
                R.layout.list_item,
                apps) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(convertView == null){
                    convertView = getLayoutInflater().inflate(R.layout.list_item, null);
                }

                ImageView appIcon = (ImageView)convertView.findViewById(R.id.item_app_icon);
                appIcon.setImageDrawable(apps.get(position).icon);

                TextView appName = (TextView)convertView.findViewById(R.id.item_app_name);
                appName.setText(apps.get(position).name);
                convertView.setTag(apps.get(position).packageName);
                return convertView;
            }
        };

        list.setAdapter(adapter);
    }

    private void loadApps() {
        manager = getPackageManager();
        apps = new ArrayList<App>();

        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> availableActivities = manager.queryIntentActivities(i, 0);
        for(ResolveInfo ri:availableActivities){
            App app = new App();
            Log.i("AGA","lA: " + ri.activityInfo.packageName);

            app.packageName = ri.activityInfo.packageName;
            app.name = ri.loadLabel(manager);
            app.icon = ri.activityInfo.loadIcon(manager);
            apps.add(app);
        }
        Collections.sort(apps);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i("AGA", "oC: " + parent.toString() + ", " + view.toString() + ", " + position + ", " + id);
        Log.i("AGA", "oC: " + apps.get(position).packageName);
        Intent result = new Intent();
        result.putExtra("packageName",apps.get(position).packageName);
        if (intent.getAction() == "action") {
            setResult(1337,result
//                    new Intent(
//                            apps.get(position).packageName,
//                            Uri.parse("content://result_uri")
//                    )
            );
            finish();
        }
        else {
            final Intent intent = getPackageManager().getLaunchIntentForPackage(apps.get(position).packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("AGAlifecycle","onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("AGAlifecycle","onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("AGAlifecycle","onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("AGAlifecycle","onDestroy");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("AGAlifecycle","onRestart");
    }
}
