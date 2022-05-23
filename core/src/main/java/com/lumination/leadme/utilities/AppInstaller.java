package com.lumination.leadme.utilities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.lumination.leadme.managers.AppManager;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.managers.DispatchManager;
import com.lumination.leadme.managers.NearbyPeersManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AppInstaller {
    private static final String TAG = "LumiAppInstaller";

    private final LeadMeMain main;
    private final AppManager appManager;

    private final View multiAppManager;
    private View installDialogView;
    public AlertDialog installDialog;
    private boolean installDialogShowing;

    public List<ImageView> selectedIcons = new ArrayList<ImageView>();
    public List<String> peersToInstall = new ArrayList<String>(); //keep track of who needs to install
    public List<String> appsToManage = new ArrayList<String>(); //keep track of apps to install or uninstall
    public List<String> peerApplications = new ArrayList<String>(); //list of applications on the currently view peers device

    //TODO populate the whitelist with applications
    public List<String> whiteList = new ArrayList<String>(); //applications that cannot be uninstalled

    Button manageSelected;
    Button clearAllSelected;
    Button manageInstallBtn;
    TextView manageInstallText;

    public Boolean multiInstalling = false; //track if the user is installing multiple apps
    public String lastAppName;
    public String lastPackageName;
    private String action;
    private int upTo;

    public AppInstaller(LeadMeMain main) {
        Log.d(TAG, "LumiAccessibilityConnector: ");
        this.main = main;
        this.multiAppManager = main.multiAppManager;
        this.appManager = main.getAppManager();
        this.upTo = 0;

        //set the button to clear and manage the selected apps
        manageSelected = multiAppManager.findViewById(R.id.manage_selected_btn);
        clearAllSelected = multiAppManager.findViewById(R.id.clear_all_btn);
        clearAllSelected.setOnClickListener((View.OnClickListener) view -> {
            resetAppSelection();
        });

        //get the dialog view ready for popups
        setupInstallerDialog();
    }


    private void setupInstallerDialog() {
        installDialogView = View.inflate(main, R.layout.e__confirm_installation, null);
        manageInstallText = installDialogView.findViewById(R.id.install_comment);
        manageInstallBtn = installDialogView.findViewById(R.id.install_btn);

        //cancel the push
        Button backBtn = installDialogView.findViewById(R.id.back_btn);
        backBtn.setOnClickListener(v -> {
            installDialog.dismiss();
            installDialogShowing = false;
        });
    }

    //TODO move dialog to dialog manager
    private void showDialog() {
        installDialog = new AlertDialog.Builder(main)
                .setView(installDialogView)
                .create();
        installDialog.setOnDismissListener(dialog -> {
            main.hideSystemUI();
            setupInstallerDialog(); //reset for next time
        });
        installDialogShowing = true;
        installDialog.show();
    }


    //********************
    //INSTALLING SECTION
    //********************

    // Called from AppManager determines if multi installing or singular install
    public void autoInstall(String packageName, String appName, String install, String[] multipleInstall) {
        if(install.equals("false") && multipleInstall == null) { //send back that the app is not installed, wait for confirmation
            DispatchManager.sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.APP_NOT_INSTALLED + ":" + appName + ":" + packageName + ":" + NearbyPeersManager.getID(), NearbyPeersManager.getSelectedPeerIDsOrAll());
            return;
        }

        prepareToInstall(packageName, appName);

        if(multipleInstall != null) { //if installing multiple apps
            multiInstalling = true;

            for (String app : multipleInstall) {
                String[] split = app.split("//"); //remove the appName from the list
                addToAppQue(split[1], appsToManage);
            }
        } else { //installing just one
            addToAppQue(packageName, appsToManage);
        }

        LeadMeMain.installingApps = true; //true for installing, false for uninstalling
        //start installing the first app
        DispatchManager.sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.AUTO_INSTALL_ATTEMPT + appName + ":" + NearbyPeersManager.getID(), NearbyPeersManager.getSelectedPeerIDsOrAll());
        main.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appsToManage.get(0))));
    }

    public void prepareToInstall(String packageName, String appName) {
        lastAppName = appName;
        lastPackageName = packageName;
        LeadMeMain.managingAutoInstaller = true;
        Log.d(TAG, "PREPARING TO INSTALL: " + lastAppName);
    }

    //hide the listview, show the scrollview
    public void resetAfterUninstall() {
        ScrollView appScroll = ((ScrollView) multiAppManager.findViewById(R.id.app_scroll_view));
        appScroll.setVisibility(View.VISIBLE);
        ListView listView = (ListView) multiAppManager.findViewById(R.id.application_list);
        listView.setVisibility(View.GONE);
    }

    public void showMultiInstaller(ViewGroup.LayoutParams layoutParams) {
        resetAfterUninstall();

        //add icons to multilaunch - do it here so we can populate with peers apps for the uninstaller
        GridView multiAppGrid = ((GridView) multiAppManager.findViewById(R.id.app_list_grid));
        multiAppGrid.setAdapter(this.appManager);
        multiAppGrid.setLayoutParams(layoutParams);

        //set the action for the process
        action = main.getResources().getString(R.string.install);

        //give the manage select button a function to perform
        manageSelected.setText(action);
        //set here as the app_manager is reused for deleting and the listener changes
        manageSelected.setOnClickListener((View.OnClickListener) view -> {
            //send action to install everything within appsToInstall
            //run the modified applicationsToInstallWarning() for confirm buttons
            applicationsToInstallWarning(null, null, true);
        });

        //eventually add a drop down?
        main.showMultiAppInstallerScreen();
        ((ScrollView) main.appLauncherScreen.findViewById(R.id.app_scroll_view)).scrollTo(0, 0);
        //set the title of the xml
        TextView title = (TextView) multiAppManager.findViewById(R.id.App_title_top);
        title.setText(R.string.application_installer);

        multiInstalling = true;
    }

    public void install(AccessibilityEvent event) {
        if(lastAppName == null) {
            return;
        }

        AccessibilityNodeInfo nodeInfo = event.getSource();
        String pname = (String) event.getPackageName();

        Log.d(TAG, pname);
        Log.i(TAG, "ACC::onAccessibilityEvent: nodeInfo=" + nodeInfo);

        if (nodeInfo == null) {
            return;
        }

        List<AccessibilityNodeInfo> accNodes = nodeInfo.findAccessibilityNodeInfosByText("ACCEPT");
        List<AccessibilityNodeInfo> playcheck1 = nodeInfo.findAccessibilityNodeInfosByText("needs access to");
        List<AccessibilityNodeInfo> updateNodes = nodeInfo.findAccessibilityNodeInfosByText("Update");
        List<AccessibilityNodeInfo> ErrorNodes = nodeInfo.findAccessibilityNodeInfosByText("Your device isn't compatible with this version");
        if(playcheck1.size()>0 && accNodes.size()>0){
            accNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        List<AccessibilityNodeInfo> installNodes = nodeInfo.findAccessibilityNodeInfosByText("Install");
        List<AccessibilityNodeInfo> uninstallNodes = nodeInfo.findAccessibilityNodeInfosByText("Uninstall");

        if (uninstallNodes.size()>0 || ErrorNodes.size()>0) {
            upTo++; //add to begin with as the first is used as the trigger event

            if (LeadMeMain.installingApps && upTo < appsToManage.size()) {
                Log.d(TAG, "Launch new page dammit: "+ upTo);
                Intent AppL = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appsToManage.get(upTo)));
                AppL.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                main.startActivity(AppL);
            }

            if(upTo >= appsToManage.size()) {
                //if installing multiple apps return to leadme after completion
                if (multiInstalling) { //TODO change this as you can install 1 app using the multi app installer
                    LeadMeMain.managingAutoInstaller = false; //reset before going back so it doesn't launch the last app
                } //if not open the newly installed application

                //app have been installed, reset for next time
                Log.i(TAG, "Installing complete.");
                appsToManage = new ArrayList<String>();
                upTo = 0;

                //refresh the application list
                main.getAppManager().refreshAppList();

                //relaunches the app or recalls to leadme if multiple installed
                if (LeadMeMain.managingAutoInstaller) {
                    main.getAppManager().launchLocalApp(lastPackageName, "INSTALLED", true, true, "false", null);
                } else {
                    //just need to refresh the downloading icon
                    DispatchManager.sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.LAUNCH_SUCCESS + "INSTALLED" + ":" + NearbyPeersManager.getID() + ":" + "LeadMe", NearbyPeersManager.getAllPeerIDs());
                    multiInstalling = false;
                    main.recallToLeadMe();
                }
            }
        }

        for (AccessibilityNodeInfo node : installNodes) {
            if(pname.equals("com.android.vending") && uninstallNodes.size()<1) {
                Log.i(TAG, "ACC::onAccessibilityEvent: install " + node);
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }

        if(installNodes.size()==0 && updateNodes.size()>0){
            for (AccessibilityNodeInfo node : updateNodes) {
                if (pname.equals("com.android.vending") && uninstallNodes.size() < 1) {
                    Log.i(TAG, "ACC::onAccessibilityEvent: install " + node);
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }
    }

    //ask if the teacher wants to install applications
    @SuppressLint("SetTextI18n")
    public void applicationsToInstallWarning(String appToInstallName, String packageToInstallName, Boolean multi) {
        //if multiple devices need to install just update the install text after the first one
        if(peersToInstall.size() > 1) {
            manageInstallText.setText(peersToInstall.size() + " devices currently do not have the application " + appToInstallName + " installed.");
        } else { //open the dialog on the first response
            //enabled incase a leader has previously tried to install 0 apps
            manageInstallBtn.setVisibility(View.VISIBLE);

            //if installing multiple apps and the auto installer toggle is on
            if(multi && LeadMeMain.autoInstallApps) {
                //if no applications have been selected to install
                if(appsToManage.size() == 0) {
                    manageInstallText.setText("No applications have been selected.");
                    manageInstallBtn.setVisibility(View.GONE);
                } else {
                    //ask if they want to install
                    manageInstallText.setText("Are you sure you want to install these applications on all devices.");
                    manageInstallBtn.setText(R.string.confirm);
                    manageInstallBtn.setOnClickListener(v -> {
                        DispatchManager.sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.MULTI_INSTALL + ":"
                                + LeadMeMain.autoInstallApps + ":" + appsToManage, NearbyPeersManager.getSelectedPeerIDsOrAll());

                        installDialog.dismiss();
                        installDialogShowing = false;

                        //go back to home page
                        main.exitCurrentView();
                        resetAppSelection();
                        multiInstalling = false;
                    });
                }

            } else if (LeadMeMain.autoInstallApps) { //check if the option has been enabled and install a singular app
                //set the action for the process
                action = main.getResources().getString(R.string.install);

                manageInstallText.setText(peersToInstall.size() + " device currently do not have the application " + appToInstallName + " installed.");
                manageInstallBtn.setText(action);
                manageInstallBtn.setOnClickListener(v -> {
                    //double check that they want to install
                    reconfirmInstallApplications(appToInstallName, packageToInstallName);
                });

            } else { //if the auto installing app toggle is unchecked
                turnOnAutoInstaller(appToInstallName, packageToInstallName, multi, true);
            }

            if(!installDialogShowing) {
                Log.e("SHOWING", String.valueOf(installDialogShowing));
                showDialog();
            }
        }
    }

    //double check confirmation for installing
    public void reconfirmInstallApplications(String appToInstallName, String packageToInstallName) {
        Log.e(TAG, "GETTING HERE?");
        ImageView installIcon = installDialogView.findViewById(R.id.install_icon);
        installIcon.setImageResource(R.drawable.alert_warning);

        TextView installText = installDialogView.findViewById(R.id.install_comment);
        installText.setText(R.string.time_warning);

        //reset the button values for a confirmation this time
        Button installBtn = installDialogView.findViewById(R.id.install_btn);
        installBtn.setText(R.string.confirm);
        installBtn.setOnClickListener(v -> {
            installDialog.dismiss();
            installDialogShowing = false;
            //push application again this time with install set to true and only to the peersToInstall list
            appManager.launchApp(packageToInstallName, appToInstallName, false, "true", false, new HashSet<>(peersToInstall));
            resetAppSelection();
        });

        if(!installDialogShowing) {
            showDialog();
        }
    }

    //TODO dialog is not working correctly
    //turn on the auto installer - triggers the shared preferences to save and peer devices to swap
    public void turnOnAutoInstaller(String appToInstallName, String packageToInstallName, Boolean multi, Boolean installing) {
        if(multi) {
            manageInstallText.setText("Auto installer is currently set to off." +
                    "\nWould you like to turn on the Auto Installer?");
        } else {
            manageInstallText.setText(peersToInstall.size() + " device(s) currently does not have the application " + appToInstallName + " installed." +
                    "\nWould you like to turn on the Auto Installer?");
        }

        manageInstallBtn.setText(R.string.turn_on);
        manageInstallBtn.setOnClickListener(v -> {
            //Dismiss the popup and reset the dialog for the next dialog
//            installDialog.dismiss();
//            main.dialogShowing = false;

            //change the shared preferences and send a message to the students to update the auto install
            main.autoToggle.setChecked(true);

            //wait while the peers update the auto settings
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //start the confirmation process again
            if(installing) {
                applicationsToInstallWarning(appToInstallName, packageToInstallName, multi);
            } else {
                applicationsToUninstallWarning();
            }
        });

        showDialog();
    }


    //********************
    //UNINSTALLING SECTION
    //********************

    //hide the scrollview, show the listview
    public void populateUninstall() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(main, R.layout.row_peer_application, R.id.textView, peerApplications);

        ListView listView = (ListView) multiAppManager.findViewById(R.id.application_list);
        listView.setVisibility(View.VISIBLE);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //get the list items package name
                String selectedItem = (String) parent.getItemAtPosition(position);
                String[] split = selectedItem.split("//");

                //TODO not currently working properly
                //ConstraintLayout textView = (ConstraintLayout) listView.getAdapter().getView(position, null, listView);

                //detect if it is already in the list
                //if not add package to appsToManage list
                if(appsToManage.contains(split[1])) {
                    appsToManage.remove(split[1]);
                    //textView.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    appsToManage.add(split[1]);
                    //highlight the listview item - placeholder colour for now
                    //textView.setBackgroundColor(Color.CYAN);
                }

                updateManageCount(); //update the count on the uninstall button
            }
        });
    }

    //load the d__app_manager_list populated with the apps from a peers device
    public void showUninstallScreen() {
        ScrollView appScroll = ((ScrollView) multiAppManager.findViewById(R.id.app_scroll_view));
        appScroll.setVisibility(View.GONE);

        action = main.getResources().getString(R.string.uninstall);

        main.showMultiAppInstallerScreen();

        manageSelected.setOnClickListener((View.OnClickListener) view -> {
            //check if autoInstaller is on & warn guide about uninstalling the applications
            applicationsToUninstallWarning();
        });

        ((ScrollView) main.appLauncherScreen.findViewById(R.id.app_scroll_view)).scrollTo(0, 0);
        TextView title = (TextView) multiAppManager.findViewById(R.id.App_title_top);
        title.setText(R.string.application_uninstaller); //set the title of the xml
        manageSelected.setText(action); //set the button text to install
    }

    //TODO currently only running one at a time
    //run on the peers device after receiving the app list
    public void runUninstaller() {
        //either have to auto click the okay button or wait for a set time?
        for(int i=0; i<appsToManage.size(); i++) {
            Log.i(TAG, "Uninstalling package: " + appsToManage.get(upTo));
            Intent Uninstall = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + appsToManage.get(upTo)));
            Uninstall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            upTo++;
            main.startActivity(Uninstall);
        }

        //reset peer for next time
        upTo = 0;
        appsToManage = new ArrayList<String>();
    }

    //unhighlight selected applications and clear lists
    public void resetAppSelection() {
        //clear the app & peer list and update the count
        appsToManage = new ArrayList<String>();
        peersToInstall = new ArrayList<String>();

        if(action.equals("install")) {
            for(ImageView icon : selectedIcons) {
                icon.setVisibility(View.INVISIBLE);
            }
        } else {
            //clear all selected uninstall apps colours
            Log.e(TAG, "Clearing uninstall highlights: " + action);
        }

        updateManageCount();
    }

    public void applicationsToUninstallWarning() {
        if(!LeadMeMain.autoInstallApps) {
            turnOnAutoInstaller(null, null, true, false);
        } else {
            //if no applications have been selected to install
            if (appsToManage.size() == 0) {
                manageInstallText.setText("No applications have been selected.");
                manageInstallBtn.setVisibility(View.GONE);
            } else {
                //ask if they want to uninstall
                manageInstallText.setText("Are you sure you want to uninstall these applications on the selected device.");
                manageInstallBtn.setText(R.string.confirm);
                manageInstallBtn.setVisibility(View.VISIBLE);
                manageInstallBtn.setOnClickListener(v -> {
                    //send action to install everything within appsToInstall
                    DispatchManager.sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.AUTO_UNINSTALL + ":"
                            + LeadMeMain.autoInstallApps + ":" + appsToManage, NearbyPeersManager.getSelectedPeerIDsOrAll());

                    installDialog.dismiss();
                    installDialogShowing = false;

                    //go back to home page
                    main.exitCurrentView();

                    //reset for next time
                    peerApplications = new ArrayList<>();
                    resetAppSelection();
                });
            }

            if(!installDialogShowing) {
                showDialog();
            }
        }
    }


    //********************
    //HELPER FUNCTIONS
    //********************

    //add a selected application to the install/uninstall que
    public void addToAppQue(String packageName, List<String> que) {
        que.add(packageName);
    }

    //updates the count next to the install/uninstaller button
    private void updateManageCount() {
        //update the selected button text
        if(appsToManage.size() > 0) {
            manageSelected.setText(action + " [" + appsToManage.size() + "]");
        } else {
            manageSelected.setText(action);
        }
    }

    //highlights app icons which have been selected
    public void selectToInstall(ImageView icon, String application) {
        if(appsToManage.contains(application)) {
            appsToManage.remove(application);
            selectedIcons.remove(icon);
            icon.setVisibility(View.INVISIBLE);
        } else {
            appsToManage.add(application);
            selectedIcons.add(icon);
            icon.setVisibility(View.VISIBLE);
        }
        updateManageCount();
    }
}
