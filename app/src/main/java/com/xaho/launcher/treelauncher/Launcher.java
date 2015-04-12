package com.xaho.launcher.treelauncher;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ActionMenuView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Launcher extends Activity implements View.OnDragListener, View.OnLongClickListener {

    private ArrayList<Integer> selection = new ArrayList<>();
    private Node start = new Node();
    private Context context;
    private Resources resources;
    private LinearLayout rootll;
    private ImageView ivDelete, ivAdd, ivAddChild, ivAssign;
    PackageManager pmi;

    private boolean editing = false;
    int rowHeight = 50;//dp
    int columnWidth = 50;//dp
    int columns = 0;
    int rows = 0;

    LinearLayout.LayoutParams flhllpwgravity = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1
    );
    LinearLayout.LayoutParams flhllpwnogravity = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        initialize();
        testForSwipe();
        //showApps(null);
    }

    private void initialize() {
        context = getApplicationContext();
        resources = getResources();
        pmi = getPackageManager();
        rootll = (LinearLayout) findViewById(R.id.rootll);
        ivAdd = (ImageView) findViewById(R.id.ivAdd);
        ivAddChild = (ImageView) findViewById(R.id.ivAddChild);
        ivDelete = (ImageView) findViewById(R.id.ivDelete);
        ivAssign = (ImageView) findViewById(R.id.ivAssign);
        calculateMaxRowsAndColumns();
        populateRootLinearLayoutWithFrameLayouts();
        ImageView appsButton = (ImageView) findViewById(R.id.IVApps);
        appsButton.setOnLongClickListener(this);

        final PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> allApps = packageManager.getInstalledApplications(0);

        Collections.sort(allApps, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                return packageManager.getApplicationLabel(lhs).toString().compareTo(packageManager.getApplicationLabel(rhs).toString());
            }
        });

        List<ApplicationInfo> apps = new ArrayList<ApplicationInfo>();
        for (ApplicationInfo app : allApps) {
            Log.i("oLC", packageManager.getApplicationLabel(app).toString());
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                apps.add(app);
            }
        }
        Log.i("oLC", "Installed apps: " + apps.size());
    }

    private void calculateMaxRowsAndColumns() {
        Configuration configuration = this.getResources().getConfiguration();
        int screenWidthDp = configuration.screenWidthDp; //The current width of the available screen space, in dp units, corresponding to screen width resource qualifier.
        int screenHeightDp = configuration.screenHeightDp;
        rows = screenHeightDp / rowHeight;
        columns = screenWidthDp / columnWidth;
        rows -= 1; //leave room for apps button
        columns -= 1;
        Log.i("Width", "" + screenWidthDp);
        Log.i("Height", "" + screenHeightDp);
        Log.i("Columns", "" + columns);
        Log.i("Rows", "" + rows);
    }

    private void updateLayouts() {
        Log.i("uL", "Selection: " + selection.toString());
        LinearLayout lvl1 = (LinearLayout) rootll.getChildAt(rootll.getChildCount() - 2);
        for (int i = 0; i < columns; i++) {
            FrameLayout fl = (FrameLayout) lvl1.getChildAt(i);
            ImageView iv = (ImageView) fl.getChildAt(0);
            if (i < start.children.size()) {
                setLayoutShow(fl, iv, start.children.get(i).level);
            } else {
                setLayoutHide(fl, iv);
            }
        }
        Node rn = start;
        for (int y = 0; y < rows - 1; y++) {//iterate rows
            Log.i("uL", "i: " + y + " < selection: " + selection.size() + "?");
            if (y < selection.size()) {
                rn = rn.children.get(selection.get(y));
                LinearLayout ll = (LinearLayout) rootll.getChildAt(rows - 2 - y);
                for (int x = 0; x < ll.getChildCount(); x++) {//iterate columns
                    FrameLayout fl = (FrameLayout) ll.getChildAt(x);
                    ImageView iv = (ImageView) fl.getChildAt(0);
                    if (x < rn.children.size()) {
                        setLayoutShow(fl, iv, rn.children.get(x).level);
                    } else {
                        setLayoutHide(fl, iv);
                    }
                }
            } else {
                LinearLayout ll = (LinearLayout) rootll.getChildAt(rows - 2 - y);
                for (int h = 0; h < ll.getChildCount(); h++) {
                    FrameLayout fl = (FrameLayout) ll.getChildAt(h);
                    ImageView iv = (ImageView) fl.getChildAt(0);
                    setLayoutHide(fl, iv);
                }
            }
        }
    }

    private void setLayoutShow(FrameLayout fl, ImageView iv, ArrayList<Integer> level) {
        if (isSelected(level)) {
            iv.setImageDrawable(resources.getDrawable(R.drawable.selectednode));
        } else if (getNodeForLevel(level).app.packageName.equals("")) {
            iv.setImageDrawable(resources.getDrawable(R.drawable.notselectednode));
        } else {
            iv.setImageDrawable(getNodeForLevel(level).app.icon);
        }
        fl.setLayoutParams(flhllpwgravity);
    }

    private void setLayoutHide(FrameLayout fl, ImageView iv) {
        fl.setLayoutParams(flhllpwnogravity);
        iv.setImageDrawable(resources.getDrawable(R.drawable.emptynode));
    }

    private boolean isSelected(ArrayList<Integer> level) {
        boolean selected = true;
        if (level.size() > selection.size()) {
            selected = false;
        } else if (selection.size() == 0) {
            selected = false;
        } else {
            for (int i = 0; i < level.size(); i++) {
                if (level.get(i) != selection.get(i)) {
                    selected = false;
                }
            }
        }
        return selected;
    }

    private void testForSwipe() {
        addLevel(null);
        selection.add(0);//0
        //selection = (ArrayList)Arrays.asList(0);
        addShortcut(null);
        addShortcut(null);
        addShortcut(null);
        addLevel(null);
        selection.add(0);//0,0
        addLevel(null);
        selection.add(0);//0,0,0
        addLevel(null);
        selection.add(0);//0,0,0,0
        selection.remove(1);//0,0,0
        selection.remove(1);//0,0
        selection.remove(1);//0
        selection.set(0, 1);//1
        //selection = (ArrayList)Arrays.asList(1);
        addLevel(null);
        selection.add(0);//1,0
        //selection = (ArrayList)Arrays.asList(1,0);
        addShortcut(null);
        addLevel(null);
        selection.set(0, 2);//2.0
        selection.remove(1);//2
        //selection = (ArrayList)Arrays.asList(2);
        addLevel(null);
        selection.add(0);//2.0
        //selection = (ArrayList)Arrays.asList(2,0);
        addShortcut(null);
        addShortcut(null);
        addLevel(null);

        updateLayouts();
    }

    public void addShortcut(View v) {
        ArrayList<Integer> localcopy = new ArrayList<>();
        localcopy.addAll(selection);

        Node rn = new Node();
        Node parent = null;

        //shouldn't add to selected item, but to selected item's parent,
        //therefore, remove latest entry to end up at a level higher then selected.
        localcopy.remove(localcopy.size() - 1);
        //TODO: fix when adding to root, parent stays null
        while (localcopy.size() > 0) {
            if (parent == null)
                parent = start.children.get(localcopy.get(0));
            else
                parent = parent.children.get(localcopy.get(0));
            localcopy.remove(0);
        }
        if (parent != null && parent.children.size() < columns) {
            rn.level.addAll(selection);
            rn.level.remove(rn.level.size() - 1);
            rn.level.add(parent.children.size());
            parent.children.add(rn);
            Log.i("aS", "Added shortcut with level: " + rn.level.toString() + " to parent: " + parent.level.toString());
        } else if (start.children.size() < columns && parent == null) {
            rn.level.add(start.children.size());
            start.children.add(rn);
            Log.i("aS", "Added shortcut with level: " + rn.level.toString() + " to start: " + start.level.toString());
        } else {
            Log.i("aS", "Adding failed, probably too much children for screen width");
        }
        updateLayouts();
    }

    public void addLevel(View v) {
        if (selection.size() < rows) {
            ArrayList<Integer> localcopy = new ArrayList<>();
            localcopy.addAll(selection);

            //backend
            Node rn = new Node();
            Node parent = null;
            while (localcopy.size() > 0) {
                if (parent == null)
                    parent = start.children.get(localcopy.get(0));
                else
                    parent = parent.children.get(localcopy.get(0));
                localcopy.remove(0);
            }
            rn.level.addAll(selection);
            rn.level.add(0);
            //finally parent should be the select node which this Node has to be added to, or, its parent.
            if (parent == null)
                if (start.children.size() == 0) {
                    start.children.add(rn);
                    Log.i("aL", "Added new level with level: " + rn.level.toString() + " to start: " + start.level.toString());
                } else
                    Log.i("aL", "Select an endpoint before adding a new level");
            else if (parent.children.size() == 0) {
                parent.children.add(rn);
                Log.i("aL", "Added new level with level: " + rn.level.toString() + " to parent: " + parent.level.toString());
            }

            updateLayouts();
        } else
            Log.i("aL", "Maximum depth achieved");
    }

    private void populateRootLinearLayoutWithFrameLayouts() {
        LinearLayout.LayoutParams hlllp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        LinearLayout.LayoutParams flhllpwgravity = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        FrameLayout.LayoutParams ivfllp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL
        );
        for (int y = 0; y < rows; y++) {
            LinearLayout hll = new LinearLayout(context);
            hll.setLayoutParams(hlllp);
            hll.setTag(rootll.getChildCount() - 2);//First will be 0 as it already contains app button

            for (int x = 0; x < columns; x++) {
                ArrayList<Integer> level = new ArrayList<>();
                level.add(y);
                level.add(x);
                ImageView iv = new ImageView(context);
                iv.setTag(level);
                iv.setLayoutParams(ivfllp);
                iv.setImageDrawable(resources.getDrawable(R.drawable.notselectednode));
                /*iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Pair<Integer,Integer> pair = (Pair<Integer,Integer>)v.getTag();
                        Log.i("","Clicked on: "+ pair.first + "," + pair.second);
                    }
                });*/
                iv.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        ClipData cp = ClipData.newPlainText("Tag", v.getTag().toString());
                        View.DragShadowBuilder sb = new View.DragShadowBuilder(v);
                        ArrayList<Integer> level = (ArrayList<Integer>) v.getTag();
                        if (editing) {
                            //todo update selection to match touched item

                        } else {
                            selection.clear();
                            selection.add(level.get(1));
                        }
                        v.startDrag(cp, sb, v, 0);
                        updateLayouts();
                        return true;
                    }
                });
                iv.setOnDragListener(this);
                FrameLayout fl = new FrameLayout(context);
                fl.setLayoutParams(flhllpwgravity);
                fl.setTag(level);
                fl.addView(iv);
                hll.addView(fl);
            }
            rootll.addView(hll, 0);
        }
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        ArrayList<Integer> level = (ArrayList<Integer>) v.getTag();
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                //Log.i("oD", "Drag started on level " + level.get(0) + "");
                return true;
            case DragEvent.ACTION_DRAG_ENTERED:
                //set selection to this
                /*if (selection.size() == 0) {
                    selection.add(pair.second);
                }else */
                if (selection.size() == level.get(0) || selection.size() == 0) {
                    selection.add(level.get(1));
                    Log.i("oD", "Adding " + level.get(1) + " to " + selection.toString());
                } else {
                    Log.i("oD", "Setting selection[" + level.get(0) + "] to " + level.get(1) + ". Selection was: " + selection.toString());
                    selection.set(level.get(0), level.get(1));
                    Log.i("oD", "Setting is now: " + selection.toString());
                    while (selection.size() - 1 > level.get(0) && selection.size() > 1)
                        selection.remove(selection.size() - 1);
                }
                Log.i("oD", "Set selection to: " + selection.toString() + " after entering: " + v.getTag().toString());
                updateLayouts();
                break;
            case DragEvent.ACTION_DROP:
                //start app assigned to node with this selection
                Log.i("oD", "Start app with selection: " + selection.toString());
                if (!editing) {
                    updateLayouts();
                    Intent intent = null;
                    intent = pmi.getLaunchIntentForPackage(getNodeForSelection().app.packageName);
                    selection.clear();
                    if (intent != null){
                        startActivity(intent);
                    } else {
                        Log.i("oD","intent = null...");
                    }
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onLongClick(View v) {
        Log.i("oLC", v.getTag() + " clicked");
        if (v.getTag().toString().equals("apps")) {
            editing = true;
            v.setTag("editing");
            //v.setBackgroundColor(Color.RED);
            ivAdd.setVisibility(View.VISIBLE);
            ivAddChild.setVisibility(View.VISIBLE);
            ivDelete.setVisibility(View.VISIBLE);
            ivAssign.setVisibility(View.VISIBLE);
            Log.i("oLC", "Editing = true");

        } else {
            editing = false;
            v.setTag("apps");
            selection.clear();
            updateLayouts();
            //v.setBackgroundColor(Color.TRANSPARENT);
            ivAdd.setVisibility(View.GONE);
            ivAddChild.setVisibility(View.GONE);
            ivDelete.setVisibility(View.GONE);
            ivAssign.setVisibility(View.GONE);
            Log.i("oLC", "Editing = false, tag: " + v.getTag());
        }
        return true;
    }

    public void showApps(View v) {
        Intent i = new Intent(this, AppGridActivity.class);
        startActivity(i);

    }

    public void assignApp(View v) {
        Log.i("aA", "assing App");
        if (getNodeForSelection().children.size() > 0) {
            Toast.makeText(context, "Cannot assign app to folder.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK);
        Intent i = new Intent("action", Uri.parse("content://result_uri"), this, AppGridActivity.class);
        startActivityForResult(i, 1337);
        //startActivity(i);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("oAR", "Received result");
        if (requestCode == 1337) {
            if (resultCode == RESULT_OK) {
                Log.i("oAR", "Result_OK");
            } else if (data != null) {
                Log.i("oAR", "Result: " + resultCode + ", intent data: " + data.getAction());
                Node node = getNodeForSelection();
                node.app.packageName = data.getStringExtra("packageName");

                try {
                    node.app.icon = pmi.getApplicationIcon(data.getStringExtra("packageName"));

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                /*Intent intent = null;

                intent = pmi.getLaunchIntentForPackage(data.getAction());
                if (intent != null){
                    startActivity(intent);
                }*/
            }
        }
    }

    private Node getNodeForSelection() {
        return getNodeForLevel(selection);
    }

    private Node getNodeForLevel(ArrayList<Integer> level) {
        ArrayList<Integer> localcopy = new ArrayList<>();
        localcopy.addAll(level);
        Node node = start;
        if (localcopy.size() == 0)
            return node;
        else {
            while (localcopy.size() > 0) {
                node = node.children.get(localcopy.get(0));
                localcopy.remove(0);
            }
            return node;
        }
    }
}
