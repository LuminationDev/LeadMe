package com.lumination.leadme;

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
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AppManager extends BaseAdapter {
    private LeadMeMain main;
    private PackageManager pm;
    private List<ApplicationInfo> appList;
    private LayoutInflater inflater;
    private String defaultBrowserUrl = "";

    private final String TAG = "AppLauncher";

    public AppManager(LeadMeMain main) {
        this.main = main;
        app_placeholder = main.getDrawable(R.drawable.icon_unknown_app);
        defaultBrowserUrl = main.getResources().getString(R.string.default_browser_url);
        inflater = LayoutInflater.from(main);
        pm = main.getPackageManager();
        appList = listApps();
    }

    protected Drawable app_placeholder;

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
            return appList;
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return null;
    }

    public String lastApp = "";

    /**
     * used by LEARNER to relaunch the last app requested by the LEADER
     **/
    public void relaunchLast(String packageName, String appName, String taskType, String url) {
        //launch it locally
        switch (taskType) {
            case "Application":
                launchLocalApp(packageName, appName, false);
                break;
            case "YouTube":
                main.getWebManager().launchYouTube(url, false);
                break;
            case "Website":
                main.getWebManager().launchWebsite(url, false);
                break;
        }
    }

    /**
     * used by LEARNER to launch an app as requested by LEADER
     **/
    public void launchLocalApp(String packageName, String appName, boolean updateCurrentTask) {
        //launch it locally
        Intent intent = main.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {

            if (packageName.toLowerCase().contains("browser")) {
                //find default browser instead
                intent = main.getWebManager().getBrowserIntent(defaultBrowserUrl);
                Log.d(TAG, "Attempting to launch default browser instead: " + intent);

                //prepare to install, which includes temporarily turning off
                //overlay to allow capture of accessibility events
            } else if (main.autoInstallApps) {
                autoInstall(packageName, appName);
            } else {
                main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.AUTO_INSTALL_FAILED + appName + ":" + main.getNearbyManager().getID(), main.getNearbyManager().getSelectedPeerIDs());
                Toast toast = Toast.makeText(main, "Sorry, the app \'" + appName + "\' doesn't exist on this device!", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        main.startActivity(intent);
        lastApp = packageName;
        if (updateCurrentTask) {
            main.updateFollowerCurrentTask(packageName, appName, "Application", "");
        }
        main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + appName + ":" + main.getNearbyManager().getID() + ":" + packageName, main.getNearbyManager().getAllPeerIDs());
    }

    protected void autoInstall(String packageName, String appName) {
        main.getRemoteDispatchService().prepareToInstall(packageName, appName);

        //launch Play Store page
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + packageName));//,  "application/vnd.android.package-archive");
        main.startActivity(intent);

        main.getRemoteDispatchService().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.AUTO_INSTALL_ATTEMPT + appName + ":" + main.getNearbyManager().getID(), main.getNearbyManager().getSelectedPeerIDs());
        Toast toast = Toast.makeText(main, "Attempting to install \'" + appName + "\', please wait...", Toast.LENGTH_SHORT);
        toast.show();
        return;

    }

    /**
     * used by LEADER to launch an app on LEARNER devices
     **/
    public void launchApp(String packageName, String appName, boolean guideToo) {
        if (guideToo) {
            //launch it locally
            launchLocalApp(packageName, appName, true);
        }


        if (!main.getConnectedLearnersAdapter().someoneIsSelected()) {
            //if no-one is selected, request to launch it on all peers
            main.getRemoteDispatchService().requestRemoteAppOpen(LeadMeMain.APP_TAG, packageName, appName, main.getNearbyManager().getAllPeerIDs());
        } else {
            //if someone is selected, request to launch only on selected peers
            main.getRemoteDispatchService().requestRemoteAppOpen(LeadMeMain.APP_TAG, packageName, appName, main.getNearbyManager().getSelectedPeerIDs());
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

        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.myTextView.setText(appName);

        try {
            final Drawable appIcon = pm.getApplicationIcon(packageName);
            viewHolder.myIcon.setImageDrawable(appIcon);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "Launching " + appName + " from " + packageName);
                    main.showAppPushDialog(appName, appIcon, packageName);
                }
            });

            convertView.setLongClickable(true);
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    main.getFavouritesManager().manageFavouritesEntry(packageName);
                    return true; //true if event is consumed
                }
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



