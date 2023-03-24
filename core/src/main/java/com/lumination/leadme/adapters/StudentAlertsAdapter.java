package com.lumination.leadme.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.lumination.leadme.connections.ConnectedPeer;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.R;
import com.lumination.leadme.controller.Controller;
import com.lumination.leadme.managers.DispatchManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class needs updating, has not had a major modification since V1 and still uses hard coded
 * values for alerts and descriptions.
 * Not high on the priority list but be aware adding extra alerts is ... painful.
 */
public class StudentAlertsAdapter extends BaseAdapter {

    private final String TAG = "StudentAlerts";

    public ArrayList<ConnectedPeer> mData = new ArrayList<>();
    private final LayoutInflater mInflater;
    private final LeadMeMain main;
    private final View list_view, no_alerts_view;

    /* Alerts By Categories
    0. Disconnected
    1. Last App didn't Launch
    2. Accessibility Disabled
    3. Overlay Disabled
    4. No internet
    5. Off Task
    6. Transfer disabled
    7. Auto installer disabled
    8. Unspecified
     */
    private ArrayList<ArrayList<ConnectedPeer>> AlertsByCategories = new ArrayList<>();

    public StudentAlertsAdapter(LeadMeMain main, View list_view, View no_alerts_view) {
        this.main = main;
        this.mInflater = LayoutInflater.from(main);
        this.list_view = list_view;
        this.no_alerts_view = no_alerts_view;
        for(int i=0; i<9; i++){
            AlertsByCategories.add(new ArrayList<>());
        }
    }

    public void setData(List<ConnectedPeer> data) {
        mData.clear();
        if (!data.isEmpty()) {
            mData.addAll(data);
        }
        refresh();
    }

    public void hideCurrentAlerts() {
        //can't remove items from a list while iterating over it,
        //so create a temp list for iterating and make changes to the real list
        ArrayList<ConnectedPeer> tmpPeers = new ArrayList<>(mData);
        for (ConnectedPeer peer : tmpPeers) {
            peer.hideAlerts(true);
            if (!peer.hasWarning()) {
                mData.remove(peer);
            }

            if (peer.getStatus() == ConnectedPeer.STATUS_ERROR) {
                Controller.getInstance().getConnectedLearnersAdapter().removeStudent(lastClickedID);
            }
        }
        //refresh main student list to reflect changes to warnings
        Controller.getInstance().getConnectedLearnersAdapter().refresh();

        //refresh alerts list for same reason
        refresh();
    }

    public void refresh() {
        if (mData.size() == 0) {
            no_alerts_view.setVisibility(View.VISIBLE);
            list_view.setVisibility(View.GONE);
        } else {
            no_alerts_view.setVisibility(View.GONE);
            list_view.setVisibility(View.VISIBLE);
        }
        main.updateLastOffTask();
        notifyDataSetChanged();
        super.notifyDataSetChanged();
    }

    public int countArray(){
        int count=0;
        for (ArrayList<ConnectedPeer> alertsByCategory : AlertsByCategories) {
            if (alertsByCategory.size() > 0) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getCount() {
        AlertsByCategories = new ArrayList<>();
        for(int i=0; i<9; i++){
            AlertsByCategories.add(new ArrayList<>());
        }

        for(int position=0; position<9; position++) {
            switch (position) {
                case 0:
                    for (ConnectedPeer peer : mData) {
                        if (peer.getAlertsList().contains("DISCONNECTED FROM GUIDE")) {
                            AlertsByCategories.get(0).add(peer);
                        }
                    }
                    break;

                case 1:
                    for (ConnectedPeer peer : mData) {
                        if (peer.getAlertsList().contains("did not launch")) {
                            AlertsByCategories.get(1).add(peer);
                        }
                    }
                    break;

                case 2:
                    for (ConnectedPeer peer : mData) {
                        if (peer.getAlertsList().contains("Accessibility service disabled")) {
                            AlertsByCategories.get(2).add(peer);
                        }
                    }
                    break;

                case 3:
                    for (ConnectedPeer peer : mData) {
                        if (peer.getAlertsList().contains("Locking overlay disabled")) {
                            AlertsByCategories.get(3).add(peer);
                        }
                    }
                    break;

                case 4:
                    for (ConnectedPeer peer : mData) {
                        if (peer.getAlertsList().contains("Might be off task")) {
                            AlertsByCategories.get(4).add(peer);
                        }
                    }
                    break;

                case 5:
                    for (ConnectedPeer peer : mData) {
                        if (peer.getAlertsList().contains("Xray permission is disabled")) {
                            AlertsByCategories.get(5).add(peer);
                        }
                    }
                    break;

                case 6:
                    for (ConnectedPeer peer : mData) {
                        if (peer.getAlertsList().contains("Transfer permission is disabled")) {
                            AlertsByCategories.get(6).add(peer);
                        }
                    }
                    break;

                case 7:
                    for (ConnectedPeer peer : mData) {
                        if (peer.getAlertsList().contains("Auto installer permission is disabled")) {
                            AlertsByCategories.get(7).add(peer);
                        }
                    }
                    break;

                case 8:
                    for (ConnectedPeer peer : mData) {
                        if (peer.getAlertsList().contains("Unspecified warning")) {
                            AlertsByCategories.get(8).add(peer);
                        }
                    }
                    break;

                default:
                    break;
            }
        }
        return countArray();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private String lastClickedID = "";
    ArrayList<View> nullViews = new ArrayList<>();
    String[] Types = {"Disconnected",
            "Last App Didn't Launch",
            "Accessibility is Disabled",
            "Overlay is Disabled",
//            "No internet connection",
            "Student May Be Off Task",
            "Xray is Disabled",
            "Transfer is Disabled",
            "Installer is Disabled",
            "Uncategorised Warnings"};

    String[] Desc ={"Learners have disconnected from LeadMe.\n",
            "The application that was pushed does not exist on learner devices.\n",
            "Learners have no enabled accessibility for LeadMe.\n",
            "Learner has not enabled screen overlay for LeadMe.\n",
//            "Task may have not launched successfully as learners are not connected to the internet.\n",
            "Learners have exited the current task and may be using the wrong application.\n",
            "The learner has not accepted the Xray permission. The prompt will be displayed again if you attempt to Xray them.\n",
            "The learner has not accepted the Transfer permission. The prompt will be displayed again if .....\n",
            "The learner has not accepted the Auto Installer permission. The prompt will be displayed again if .....\n",
            "To be completely honest, I'm not really sure how we got here.\n"
    };
    String[] buttonTxt = {"Clear",
            "Clear", //Install - add later?
            "Launch",
            "Clear",
//            "Proceed Offline",
            "Re-push",
            "Enable Xray",
            "Enable Transfer",
            "Enable Installer"};

    int[] button_icons = {R.drawable.icon_clear,R.drawable.icon_repush,R.drawable.ic_settings,R.drawable.icon_clear,R.drawable.icon_clear,R.drawable.icon_repush,R.drawable.icon_clear,R.drawable.ic_settings,R.drawable.ic_settings};
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //if (convertView == null) {
        convertView = mInflater.inflate(R.layout.row_student_alert, parent, false);
        //}

        TextView alertType = convertView.findViewById(R.id.list_alert_type);
        ImageView listIcon = convertView.findViewById(R.id.list_drop_icon);
        TextView alertList = convertView.findViewById(R.id.warning_list);
        TextView alertDesc = convertView.findViewById(R.id.warning_desc);
        Button alertBtn = convertView.findViewById(R.id.warning_button);

        convertView.setOnClickListener(v -> {
            if(alertList.getVisibility() == View.VISIBLE){
                alertList.setVisibility(View.GONE);
                alertDesc.setVisibility(View.GONE);
                alertBtn.setVisibility(View.GONE);
                listIcon.setImageResource(R.drawable.icon_list_right);
            } else {
                alertList.setVisibility(View.VISIBLE);
                alertDesc.setVisibility(View.VISIBLE);
                alertBtn.setVisibility(View.VISIBLE);
                listIcon.setImageResource(R.drawable.icon_list_down);
            }
        });

        Log.d(TAG, "getView: " + position);
        String nameList="Affected Students:\n";
        int index = -1;
        int count = 0;

        for(int i=0; i<AlertsByCategories.size(); i++) {
            if(AlertsByCategories.get(i).size() != 0){
                if(count == position) {
                    index = i;
                    int numStud = 0;
                    for (int j = 0; j < AlertsByCategories.get(i).size(); j++) {
                        Log.d(TAG, "AlertsByCategories "+i);
                        nameList += "• " + AlertsByCategories.get(i).get(j).getDisplayName() + "\n";
                        numStud++;
                    }

                    alertType.setText(Types[i]+" ("+numStud+")");

                    if(i == 1) {
                        alertDesc.setText(Desc[i] + "\nApplication: " + DispatchManager.appNameRepush + "\n");
                    } else {
                        alertDesc.setText(Desc[i]);
                    }

                    alertBtn.setText(buttonTxt[i]);
                    alertBtn.setCompoundDrawablesWithIntrinsicBounds(button_icons[i], 0, 0, 0);
                }
                count++;
            }
        }

        final int finalcount =index;
        alertBtn.setOnClickListener(new View.OnClickListener() {
            final int position = finalcount;
            @Override
            public void onClick(View v) {
                if(alertBtn.getText().equals("Re-push")) {
                    Log.d(TAG, "onClick: " + alertBtn);
                    ArrayList<String> ids = new ArrayList<>();
                    for (int i = 0; i < AlertsByCategories.get(5).size(); i++) {
                        ids.add(String.valueOf(AlertsByCategories.get(5).get(i).getID()));
                    }
                    if (ids.size() > 0) {
                        Set<String> selectedPeerIDs = new HashSet<>(ids);
                        DispatchManager.repushApp(selectedPeerIDs);
                    }
                } else if(alertBtn.getText().equals("Install")){
                    //Implement auto installer here? - probably not a good idea though
                    for(int i=0; i<AlertsByCategories.get(position).size();i++){
                        AlertsByCategories.get(position).get(i).hideAlerts(true);
                    }
                } else if(alertBtn.getText().equals("Recall")){
                    Controller.getInstance().getConnectedLearnersAdapter().selectAllPeers(false);
                    for(int i=0; i<AlertsByCategories.get(position).size();i++){
                        Controller.getInstance().getConnectedLearnersAdapter().selectPeer(AlertsByCategories.get(position).get(i).getID(),true);
                    }
                    main.returnToAppFromMainAction(false);
                    Controller.getInstance().getConnectedLearnersAdapter().selectAllPeers(false);
                } else if(alertBtn.getText().equals("Block")){
                    Controller.getInstance().getConnectedLearnersAdapter().selectAllPeers(false);
                    for(int i=0; i<AlertsByCategories.get(position).size();i++){
                        Controller.getInstance().getConnectedLearnersAdapter().selectPeer(AlertsByCategories.get(position).get(i).getID(),true);
                    }
                    main.blackoutFromMainAction();
                    Controller.getInstance().getConnectedLearnersAdapter().selectAllPeers(false);
                }
                else if(alertBtn.getText().equals("Clear")){
                    hideCurrentAlerts();
                    for(int i=0; i<AlertsByCategories.get(position).size();i++){
                        AlertsByCategories.get(position).get(i).hideAlerts(true);
                    }
                } else if(alertBtn.getText().equals("Launch")) {
                    ArrayList<String> ids = new ArrayList<>();
                    for (int i = 0; i < AlertsByCategories.get(2).size(); i++) {
                        ids.add(String.valueOf(AlertsByCategories.get(2).get(i).getID()));
                    }
                    if (ids.size() > 0) {
                        Set<String> selectedPeerIDs = new HashSet<>(ids);
                        DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.LAUNCH_ACCESS,selectedPeerIDs);
                    }
                } else if(alertBtn.getText().equals("Enable Transfer")) {
                    ArrayList<String> ids = new ArrayList<>();
                    for (int i = 0; i < AlertsByCategories.get(6).size(); i++) {
                        ids.add(String.valueOf(AlertsByCategories.get(6).get(i).getID()));
                    }

                    if (ids.size() > 0) {
                        Set<String> selectedPeerIDs = new HashSet<>(ids);
                        DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.FILE_TRANSFER + ":"
                                + true, selectedPeerIDs);
                    }
                } else if(alertBtn.getText().equals("Enable Installer")) {
                    ArrayList<String> ids = new ArrayList<>();
                    for (int i = 0; i < AlertsByCategories.get(7).size(); i++) {
                        ids.add(String.valueOf(AlertsByCategories.get(7).get(i).getID()));
                    }

                    if (ids.size() > 0) {
                        Set<String> selectedPeerIDs = new HashSet<>(ids);
                        DispatchManager.sendActionToSelected(Controller.ACTION_TAG, Controller.AUTO_INSTALL + ":"
                                + true, selectedPeerIDs);
                    }
                }
            }
        });

        Log.d(TAG, "getView: "+nameList);
        alertList.setText(nameList);
        //refresh();
        return convertView;
    }
}
