package com.lumination.leadme;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppManager extends BaseAdapter {
    private LeadMeMain main;
    private PackageManager pm;
    private List<ApplicationInfo> appList;
    private final LayoutInflater inflater;
    private String defaultBrowserUrl = "";
    protected Drawable app_placeholder;
    private WithinEmbedPlayer withinPlayer;

    private final String TAG = "AppLauncher";

    public final String withinPackage = "com.shakingearthdigital.vrsecardboard";
    public final String youtubePackage = "com.google.android.youtube";

    public AppManager(LeadMeMain main) {
        this.main = main;
        withinPlayer = new WithinEmbedPlayer(main);
        app_placeholder = main.getDrawable(R.drawable.icon_unknown_browser);
        defaultBrowserUrl = main.getResources().getString(R.string.default_browser_url);
        inflater = LayoutInflater.from(main);
        pm = main.getPackageManager();
        appList = listApps();

        //set up lock spinner
        lockSpinner = main.appPushDialogView.findViewById(R.id.push_spinner);
        withinLockSpinner = withinPlayer.getLockSpinner();
        String[] items = {"View only", "Free play"};
        Integer[] imgs = {R.drawable.controls_view, R.drawable.controls_play};

        LumiSpinnerAdapter adapter = new LumiSpinnerAdapter(main, R.layout.row_push_spinner, items, imgs);
        lockSpinner.setAdapter(adapter);
        lockSpinner.setSelection(1); //default to unlocked

        LumiSpinnerAdapter withinAdapter = new LumiSpinnerAdapter(main, R.layout.row_push_spinner, items, imgs);
        withinLockSpinner.setAdapter(withinAdapter);
    }


    public Drawable getAppIcon(String packageName) {
        try {
            return pm.getApplicationIcon(packageName);

        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Couldn't get icon for " + packageName);
            return app_placeholder; //new R.drawable.student_placeholder;
        }
    }

    public String getAppName(String packageName) {
        try {
            return pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString();

        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Couldn't get name for " + packageName);
            return null; //new R.drawable.student_placeholder;
        }
    }

    public class ApplicationInfoComparator implements Comparator<ApplicationInfo> {
        @Override
        public int compare(ApplicationInfo t1, ApplicationInfo t2) {
            String t1Name = pm.getApplicationLabel(t1).toString();
            String t2Name = pm.getApplicationLabel(t2).toString();
            return t1Name.compareToIgnoreCase(t2Name);
        }
    }

    private List<ApplicationInfo> listApps() {
        appList = new ArrayList<>();
        ArrayList<String> nameList = new ArrayList<>();
        try {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
            for (ResolveInfo info : resolveInfos) {
                ApplicationInfo ai = info.activityInfo.applicationInfo;
                //sometimes these don't automatically have a name - insert it now if needed
                if (ai.name == null) {
                    ai.name = pm.getApplicationLabel(ai).toString();
                }

                //check to avoid double ups and avoid adding this app to the list
                if (!nameList.contains(ai.packageName) && !ai.packageName.equals(main.getPackageName())) {
                    appList.add(ai);
                    nameList.add(ai.packageName); //to prevent duplicates
                }
            }
            //sort alphabetically by name
            Collections.sort(appList, new ApplicationInfoComparator());
            return appList;
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return null;
    }

    public String lastApp = "";

    public void relaunchLast() {
        relaunchLast(main.currentTaskPackageName, main.currentTaskName, main.currentTaskType, main.currentTaskURL, main.currentTaskURLTitle);
    }


    /**
     * used by LEARNER to relaunch the last app requested by the LEADER
     **/
    public void relaunchLast(String packageName, String appName, String taskType, String url, String urlTitle) {
        //launch it locally
        Log.w(TAG, "Relaunching: " + taskType + ", " + url + ", " + packageName);
        switch (taskType) {
            case "Application":
                launchLocalApp(packageName, appName, false, true);
                break;
            case "VR Video":
                if (packageName.equals(main.getAppManager().withinPackage)) {
                    launchWithin(withinPlayer.foundURL, isStreaming, isVR);
                    break;
                }
            case "YouTube":
                main.getWebManager().launchYouTube(url, urlTitle, false, main.getWebManager().getYouTubeEmbedPlayer().isVROn());
                break;
            case "Website":
                main.getWebManager().launchWebsite(url, urlTitle, false);
                break;
        }
    }

    /**
     * used by LEARNER to launch an app as requested by LEADER
     **/
    public void launchLocalApp(String packageName, String appName, boolean updateCurrentTask, boolean relaunch) {
        //check overlay status and alert leader if there's an issue
        main.verifyOverlay();

        //launch it locally
        String actualAppPackage = packageName;
        Intent intent = main.getPackageManager().getLaunchIntentForPackage(packageName);


        if (intent == null) {

            if (packageName.toLowerCase().contains("browser")) {
                //find default browser instead
                intent = main.getWebManager().getBrowserIntent(defaultBrowserUrl);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                actualAppPackage = intent.getPackage(); //store the ACTUAL app that is getting launched, not just what was requested
                Log.d(TAG, "Attempting to launch default browser instead: " + packageName + " // " + actualAppPackage + " // " + defaultBrowserUrl);

                //prepare to install, which includes temporarily turning off
                //overlay to allow capture of accessibility events
            } else if (main.autoInstallApps) {
                autoInstall(packageName, appName);
                return;

            } else {
                main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.AUTO_INSTALL_FAILED + appName + ":" + main.getNearbyManager().getID(), main.getNearbyManager().getSelectedPeerIDsOrAll());
                Toast toast = Toast.makeText(main, "Sorry, the app '" + appName + "' doesn't exist on this device!", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
        }

        if (packageName.equals(withinPackage)) {
            intent = new Intent(Intent.ACTION_VIEW);
            //intent = new Intent(Intent.ACTION_VIEW, withinURI);
            intent.setData(withinURI);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setComponent(new ComponentName(withinPackage, withinPackage + ".activities.DeeplinkStartupActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            //intent.setPackage(packageName);
        }

        if (actualAppPackage.equals(withinPackage)) {
            main.activityManager.killBackgroundProcesses(withinPackage);
        }

        if (actualAppPackage.equals(youtubePackage)) {
            main.activityManager.killBackgroundProcesses(youtubePackage);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        main.startActivity(intent);
        lastApp = packageName;

        if (updateCurrentTask) {
            if (relaunch) {
                main.updateFollowerCurrentTask(actualAppPackage, appName, main.currentTaskType, main.currentTaskURL, main.currentTaskURLTitle);
            } else {
                main.updateFollowerCurrentTask(actualAppPackage, appName, "Application", "", "");
            }
        }
        Log.w(TAG, "Launching: " + appName + ", " + actualAppPackage + " " + withinPackage + ", " + withinURI);


        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + appName + ":" + main.getNearbyManager().getID() + ":" + actualAppPackage, main.getNearbyManager().getAllPeerIDs());
    }

    protected void autoInstall(String packageName, String appName) {
        main.getLumiAccessibilityConnector().prepareToInstall(packageName, appName);

        //launch Play Store page
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + packageName));//,  "application/vnd.android.package-archive");
        main.startActivity(intent);
        main.getAppManager().lastApp = intent.getPackage();

        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.AUTO_INSTALL_ATTEMPT + appName + ":" + main.getNearbyManager().getID(), main.getNearbyManager().getSelectedPeerIDsOrAll());
        Toast toast = Toast.makeText(main, "Attempting to install '" + appName + "', please wait...", Toast.LENGTH_SHORT);
        toast.show();
        return;

    }

    private Spinner lockSpinner, withinLockSpinner;

    /**
     * used by LEADER to launch an app on LEARNER devices
     **/
    public void launchApp(String packageName, String appName, boolean guideToo) {
        if (guideToo) {
            //launch it locally
            launchLocalApp(packageName, appName, true, false);
        }

        //update lock status
        String lockTag = LeadMeMain.LOCK_TAG;
        if (lockSpinner.getSelectedItemPosition() == 1) {
            //locked by default, unlocked if selected
            lockTag = LeadMeMain.UNLOCK_TAG;
        }

        //send launch request
        main.getDispatcher().requestRemoteAppOpen(LeadMeMain.APP_TAG, packageName, appName, lockTag, main.getNearbyManager().getSelectedPeerIDsOrAll());
    }

    public WithinEmbedPlayer getWithinPlayer() {
        return withinPlayer;
    }

    boolean isStreaming = false;
    boolean isVR = true;
    Uri withinURI = null;
    boolean videoInit = false;

    public boolean getIsWithinStreaming() {
        return isStreaming;
    }

    public boolean getIsWithinVRMode() {
        return isVR;
    }

    public void launchWithin(String url, boolean isStreaming, boolean isVR) {
        Log.d(TAG, "launchWithin: " + isStreaming + ", " + isVR);
        this.isStreaming = isStreaming;
        this.isVR = isVR;
        videoInit = false; //reset
        //update lock status
        String lockTag = LeadMeMain.LOCK_TAG;
        if (withinLockSpinner.getSelectedItemPosition() == 1) {
            //locked by default, unlocked if selected
            lockTag = LeadMeMain.UNLOCK_TAG;
        }
        //send launch request
        main.getDispatcher().requestRemoteWithinLaunch(LeadMeMain.APP_TAG, withinPackage, "Within VR", lockTag, url, isStreaming, isVR, main.getNearbyManager().getSelectedPeerIDsOrAll());
    }


    @Override
    public int getCount() {
        if (appList != null) {
            return appList.size();
        } else {
            return 0;
        }
    }

    // convenience method for getting data at click position
    public ApplicationInfo getItem(int id) {
        return appList.get(id);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final String packageName = appList.get(position).packageName;
        final String appName = pm.getApplicationLabel(appList.get(position)).toString();

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_apps, parent, false);
            final ViewHolder viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.myTextView.setText(appName);
        viewHolder.myIcon.setContentDescription(appName); //for screen readers

        try {
            final Drawable appIcon = pm.getApplicationIcon(packageName);
            viewHolder.myIcon.setImageDrawable(appIcon);

            convertView.setOnClickListener(v -> {
                Log.i(TAG, "Launching " + appName + " from " + packageName + " " + withinPackage);
                if (packageName.equals(withinPackage)) {
                    Log.d(TAG, "getView: is a within package");
                    withinPlayer.showWithin(); //showGuideController();
                } else {
                    main.showAppPushDialog(appName, appIcon, packageName);
                }
            });

            convertView.setLongClickable(true);
            convertView.setOnLongClickListener(v -> {
                main.getFavouritesManager().manageFavouritesEntry(packageName);
                return true; //true if event is consumed
            });

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return convertView;
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder {
        final TextView myTextView;
        final ImageView myIcon;

        ViewHolder(View itemView) {
            myTextView = itemView.findViewById(R.id.app_name);
            myIcon = itemView.findViewById(R.id.app_icon);
        }
    }

}



