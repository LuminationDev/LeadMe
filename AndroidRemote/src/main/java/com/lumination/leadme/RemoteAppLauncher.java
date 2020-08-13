package com.lumination.leadme;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class RemoteAppLauncher extends Fragment {
    private View mContentView = null;
    private GridView gridView;
    private ListAdapter mAdapter;
    private PackageManager pm;
    private List<ApplicationInfo> appList;
    private View.OnClickListener onClickListener;

    private final String TAG = "AppLauncher";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            mContentView = inflater.inflate(R.layout.app_list, null);
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        gridView = mContentView.findViewById(R.id.appListView);

        // use a linear layout manager
        Context applicationContext = getActivity().getApplicationContext();
        pm = getActivity().getPackageManager();

        // specify an adapter
        mAdapter = new AppListAdapter(applicationContext, listApps(), pm);
        gridView.setAdapter(mAdapter);

        return mContentView;
    }

    public List<ApplicationInfo> listApps() {
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
                if (!nameList.contains(ai.packageName) && !ai.packageName.equals(getActivity().getPackageName())) {
                    appList.add(ai);
                    nameList.add(ai.packageName); //to prevent duplicates
                }
            }

            //appList = installedApplications;
            return appList;
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return null;
    }

    public String lastApp = "";
    public void launchLocalApp(MainActivity main, String packageName, String appName) {
        //launch it locally
        Intent intent = main.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            // Bring user to the market or let them choose an app?

            //prepare to install, which includes temporarily turning off
            //overlay to allow capture of accessibility events
            if (main.autoInstallApps) {
                main.getRemoteDispatchService().prepareToInstall(packageName, appName);

                //launch Play Store page
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + packageName));//,  "application/vnd.android.package-archive");
                main.startActivity(intent);

                main.getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.AUTO_INSTALL_ATTEMPT + appName + ":" + main.nearbyManager.getName());

                Toast toast = Toast.makeText(main, "Attempting to install \'" + appName + "\', please wait...", Toast.LENGTH_SHORT);
                toast.show();
            } else {
                main.getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.AUTO_INSTALL_FAILED + appName + ":" + main.nearbyManager.getName());

                Toast toast = Toast.makeText(main, "Sorry, the app \'" + appName + "\' doesn't exist on this device!", Toast.LENGTH_SHORT);
                toast.show();
            }


            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        main.startActivity(intent);
        lastApp = packageName;
        main.getRemoteDispatchService().sendAction(MainActivity.ACTION_TAG, MainActivity.LAUNCH_SUCCESS + appName + ":" + main.nearbyManager.getName());
    }

    public void launchApp(MainActivity main, String packageName, String appName) {
        //launch it locally
        launchLocalApp(main, packageName, appName);

        //request to launch it on all peers
        ((MainActivity) getActivity()).getRemoteDispatchService().requestRemoteAppOpen(MainActivity.APP_TAG, packageName, appName);
    }


    private class AppListAdapter extends BaseAdapter {
        private List<ApplicationInfo> mData;
        private LayoutInflater mInflater;
        private PackageManager mPm;

        // data is passed into the constructor
        AppListAdapter(Context context, final List<ApplicationInfo> data, PackageManager pm) {
            this.mInflater = LayoutInflater.from(context);
            this.mData = data;
            this.mPm = pm;
        }

        @Override
        public int getCount() {
            if (mData != null) {
                return mData.size();
            } else {
                return 0;
            }
        }

        // convenience method for getting data at click position
        public ApplicationInfo getItem(int id) {
            return mData.get(id);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final String packageName = mData.get(position).packageName;
            final String appName = mPm.getApplicationLabel(mData.get(position)).toString();

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_apps, parent, false);
                final ViewHolder viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            }

            final ViewHolder viewHolder = (ViewHolder)convertView.getTag();
            viewHolder.myTextView.setText(appName);

            try {
                Drawable appIcon = mPm.getApplicationIcon(packageName);
                viewHolder.myIcon.setImageDrawable(appIcon);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "Launching " + appName + " from " + packageName);
                    launchApp(((MainActivity) getContext()), packageName, appName);
                }
            });

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

}


