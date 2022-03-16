package com.lumination.leadme.managers;

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

import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.adapters.LumiSpinnerAdapter;
import com.lumination.leadme.players.WithinEmbedPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class AppManager extends BaseAdapter {
    private final String TAG = "AppLauncher";

    private final LeadMeMain main;
    private final PackageManager pm;
    private FavouritesManager favouritesManager; //TODO shift to app manager
    private final WithinEmbedPlayer withinPlayer;

    private List<ApplicationInfo> appList;
    private ArrayList<String> appNameList;
    private final LayoutInflater inflater;
    private String defaultBrowserUrl = "";
    protected Drawable app_placeholder;

    public final String vrplayerPackage = "com.Edward.VRPlayer";
    public final String withinPackage = "com.shakingearthdigital.vrsecardboard";
    public final String youtubePackage = "com.google.android.youtube";

    public AppManager(LeadMeMain main) {
        this.main = main;
        favouritesManager = new FavouritesManager(main, null, FavouritesManager.FAVTYPE_APP, 4);
        withinPlayer = new WithinEmbedPlayer(main);
        app_placeholder = main.getDrawable(R.drawable.icon_unknown_browser);
        defaultBrowserUrl = main.getResources().getString(R.string.default_browser_url);
        inflater = LayoutInflater.from(main);
        pm = main.getPackageManager();
        appList = listApps();

        //set up lock spinner
        lockSpinner = main.getDialogManager().appPushDialogView.findViewById(R.id.push_spinner);
        withinLockSpinner = withinPlayer.getLockSpinner();
        String[] items = {"View only", "Free play"};
        Integer[] imgs = {R.drawable.controls_view, R.drawable.controls_play};

        LumiSpinnerAdapter adapter = new LumiSpinnerAdapter(main, R.layout.row_push_spinner, items, imgs);
        lockSpinner.setAdapter(adapter);
        lockSpinner.setSelection(1); //default to unlocked

        LumiSpinnerAdapter withinAdapter = new LumiSpinnerAdapter(main, R.layout.row_push_spinner, items, imgs);
        withinLockSpinner.setAdapter(withinAdapter);
    }

    public FavouritesManager getFavouritesManager() {
        return favouritesManager;
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
        appNameList = new ArrayList<String>();
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
                    appNameList.add(getAppName(ai.packageName) + "//" + ai.packageName);
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

    /**
     * Refresh the local app list in case something has been installed during the current session.
     */
    public List<String> refreshAppList() {
        listApps();
        return this.appNameList;
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
                launchLocalApp(packageName, appName, false, true, "false", null);
                break;

            case "VR Video":
                if (packageName.equals(main.getAppManager().withinPackage)) {
                    launchWithin(withinPlayer.foundURL, isStreaming, isVR,true);
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
     * Used by LEARNER to launch an app as requested by LEADER through the dispatch manager.
     * @param packageName A string representing the google play store package name.
     * @param appName A string representing the name of the application.
     * @param updateCurrentTask A boolean for if the peer needs to update their current task.
     * @param relaunch A boolean for if the application is being launched.
     * @param install A string representing if the application needs to be installed.
     * @param multipleInstall An array of strings holding the package names of applications to be installed.
     */
    public void launchLocalApp(String packageName, String appName, boolean updateCurrentTask, boolean relaunch, String install, String[] multipleInstall) {
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
            } else if (main.autoInstallApps && LeadMeMain.FLAG_UPDATES) {
                main.getLumiAppInstaller().autoInstall(packageName, appName, install, multipleInstall);
                return;

            }
            else {
                main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.APP_NOT_INSTALLED + ":" + appName + ":" + packageName + ":" + main.getNearbyManager().getID(), main.getNearbyManager().getSelectedPeerIDsOrAll());
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
        } else {
            main.getLumiAccessibilityConnector().withinManager.cleanUpVideo(); //reset video state
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

    private Spinner lockSpinner, withinLockSpinner;

    /**
     * Used by LEADER to launch an app on LEARNER devices.
     * @param packageName A string representing the google play store package name.
     * @param appName A string representing the name of the application.
     * @param guideToo A boolean determining if launching the app on the guide's device as well.
     * @param install A string representing if the application needs to be installed.
     * @param VRPlayer A boolean determining if the custom VR player is being launched.
     * @param peerSet A set of strings representing the learner devices that the function is being sent to.
     */
    public void launchApp(String packageName, String appName, boolean guideToo, String install, boolean VRPlayer, Set<String> peerSet) {
        if (guideToo) {
            //launch it locally
            launchLocalApp(packageName, appName, true, false, install, null);
        }

        //update lock status
        String lockTag = LeadMeMain.LOCK_TAG;
        if (lockSpinner.getSelectedItemPosition() == 1 && !VRPlayer) {
            //locked by default, unlocked if selected
            lockTag = LeadMeMain.UNLOCK_TAG;
        }

        //send launch request
        main.getDispatcher().requestRemoteAppOpen(LeadMeMain.APP_TAG, packageName, appName, lockTag, install, peerSet);
    }

    public WithinEmbedPlayer getWithinPlayer() {
        return withinPlayer;
    }

    public boolean isStreaming = false;
    public boolean isVR = true;
    public Uri withinURI = null;
    public static boolean videoInit = false;

    public boolean getIsWithinStreaming() {
        return isStreaming;
    }

    public boolean getIsWithinVRMode() {
        return isVR;
    }

    /**
     * Request that Within open with the supplied url video.
     * @param url A string representing the url of the selected video.
     * @param isStreaming A boolean determining if the video should be streamed of downloaded.
     * @param isVR A boolean determining if the experience is in VR or flat screen mode.
     * @param selectedOnly  A boolean determining if just the selected peers should launch the
     *                      application.
     */
    public void launchWithin(String url, boolean isStreaming, boolean isVR, boolean selectedOnly) {
        Log.d(TAG, "launchWithin: " + isStreaming + ", " + isVR + ", " + "Title: " + url);
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
        if(selectedOnly) {
            main.getDispatcher().requestRemoteWithinLaunch(LeadMeMain.APP_TAG, withinPackage, "Within VR", lockTag, url, isStreaming, isVR, main.getNearbyManager().getSelectedPeerIDsOrAll());
        }else{
            main.getDispatcher().requestRemoteWithinLaunch(LeadMeMain.APP_TAG, withinPackage, "Within VR", lockTag, url, isStreaming, isVR, main.getNearbyManager().getAllPeerIDs());
        }
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

        ImageView selectedIndicator = convertView.findViewById(R.id.selected_indicator);

        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.myTextView.setText(appName);
        viewHolder.myIcon.setContentDescription(appName); //for screen readers

        try {
            final Drawable appIcon = pm.getApplicationIcon(packageName);
            viewHolder.myIcon.setImageDrawable(appIcon);

            convertView.setOnClickListener(v -> {
                Log.i(TAG, "Launching " + appName + " from " + packageName + " " + withinPackage);

                if(main.getLumiAppInstaller().multiInstalling) { //selecting apps to install - first so Within can be selected
                    main.getLumiAppInstaller().selectToInstall(selectedIndicator, appName + "//" + packageName);
                } else if (packageName.equals(withinPackage)) {
                    Log.d(TAG, "getView: is a within package");
                    withinPlayer.showWithin(); //showGuideController();
                } else if(packageName.equals(vrplayerPackage)) {
                    main.getVrEmbedPlayer().showPlaybackPreview(appName);
                } else {
                    main.getDialogManager().showAppPushDialog(appName, appIcon, packageName);
                }
            });

            convertView.setLongClickable(true);
            convertView.setOnLongClickListener(v -> {
                favouritesManager.manageFavouritesEntry(packageName);
                return true; //true if event is consumed
            });

        } catch (Exception e) {
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
