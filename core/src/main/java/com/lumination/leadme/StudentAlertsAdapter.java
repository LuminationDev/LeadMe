package com.lumination.leadme;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class StudentAlertsAdapter extends BaseAdapter {

    private final String TAG = "StudentAlerts";

    public ArrayList<ConnectedPeer> mData = new ArrayList<>();
    private LayoutInflater mInflater;
    private LeadMeMain main;
    private View list_view, no_alerts_view;

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

    StudentAlertsAdapter(LeadMeMain main, View list_view, View no_alerts_view) {
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
        ArrayList<ConnectedPeer> tmpPeers = new ArrayList<>();
        tmpPeers.addAll(mData);
        for (ConnectedPeer peer : tmpPeers) {
            peer.hideAlerts(true);
            if (!peer.hasWarning()) {
                mData.remove(peer);
            }

            if (peer.getStatus() == ConnectedPeer.STATUS_ERROR) {
                main.getConnectedLearnersAdapter().removeStudent(lastClickedID);
            }
        }
        //refresh main student list to reflect changes to warnings
        main.getConnectedLearnersAdapter().refresh();

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
        Iterator<ArrayList<ConnectedPeer>> iterator = AlertsByCategories.iterator();
        while(iterator.hasNext()){
            if(iterator.next().size()>0){
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
                    Iterator<ConnectedPeer> iterator0 = mData.iterator();
                    while (iterator0.hasNext()) {
                        ConnectedPeer peer = iterator0.next();
                        if (peer.getAlertsList().contains("DISCONNECTED FROM GUIDE")) {
                            AlertsByCategories.get(0).add(peer);
                        }
                    }
                    break;
                case 1:
                    Iterator<ConnectedPeer> iterator1 = mData.iterator();
                    while (iterator1.hasNext()) {
                        ConnectedPeer peer = iterator1.next();
                        if (peer.getAlertsList().contains("did not launch")) {
                            AlertsByCategories.get(1).add(peer);
                        }
                    }
                    break;
                case 2:
                    Iterator<ConnectedPeer> iterator2 = mData.iterator();
                    while (iterator2.hasNext()) {
                        ConnectedPeer peer = iterator2.next();
                        if (peer.getAlertsList().contains("Accessibility service disabled")) {
                            AlertsByCategories.get(2).add(peer);
                        }
                    }
                    break;
                case 3:
                    Iterator<ConnectedPeer> iterator3 = mData.iterator();
                    while (iterator3.hasNext()) {
                        ConnectedPeer peer = iterator3.next();
                        if (peer.getAlertsList().contains("Locking overlay disabled")) {
                            AlertsByCategories.get(3).add(peer);
                        }
                    }

                    break;
                case 4:
                    Iterator<ConnectedPeer> iterator4 = mData.iterator();
                    while (iterator4.hasNext()) {
                        ConnectedPeer peer = iterator4.next();
                        if (peer.getAlertsList().contains("Might be off task")) {
                            AlertsByCategories.get(4).add(peer);
                        }
                    }
                    break;
                case 5:
                    Iterator<ConnectedPeer> iterator5 = mData.iterator();
                    while (iterator5.hasNext()) {
                        ConnectedPeer peer = iterator5.next();
                        if (peer.getAlertsList().contains("Xray permission is disabled")) {
                            AlertsByCategories.get(5).add(peer);
                        }
                    }
                    break;
                case 6:
                    Iterator<ConnectedPeer> iterator6 = mData.iterator();
                    while (iterator6.hasNext()) {
                        ConnectedPeer peer = iterator6.next();
                        if (peer.getAlertsList().contains("Transfer permission is disabled")) {
                            AlertsByCategories.get(6).add(peer);
                        }
                    }
                    break;
                case 7:
                    Iterator<ConnectedPeer> iterator7 = mData.iterator();
                    while (iterator7.hasNext()) {
                        ConnectedPeer peer = iterator7.next();
                        if (peer.getAlertsList().contains("Auto installer permission is disabled")) {
                            AlertsByCategories.get(7).add(peer);
                        }
                    }
                    break;
                case 8:
                    Iterator<ConnectedPeer> iterator8 = mData.iterator();
                    while (iterator8.hasNext()) {
                        ConnectedPeer peer = iterator8.next();
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
    String Types[] = {"Disconnected",
            "Last App Didn't Launch",
            "Accessibility is Disabled",
            "Overlay is Disabled",
//            "No internet connection",
            "Student May Be Off Task",
            "Xray is Disabled",
            "Transfer is Disabled",
            "Installer is Disabled",
            "Uncategorised Warnings"};

    String Desc[] ={"Learners have disconnected from LeadMe.\n",
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
    String buttonTxt[] = {"Clear",
            "Re-push",
            "Launch",
            "Clear",
//            "Proceed Offline",
            "Re-push",
            "Clear",
            "Enable Transfer",
            "Enable Installer"};

    int button_icons[] = {R.drawable.icon_clear,R.drawable.icon_repush,R.drawable.ic_settings,R.drawable.icon_clear,R.drawable.icon_clear,R.drawable.icon_repush,R.drawable.icon_clear,R.drawable.ic_settings,R.drawable.ic_settings};
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //if (convertView == null) {
        convertView = mInflater.inflate(R.layout.row_student_alert, parent, false);
        //}

        TextView alertType = convertView.findViewById(R.id.list_alert_type);
        ImageView listIcon = convertView.findViewById(R.id.list_drop_icon);
        //ImageView infoIcon = convertView.findViewById(R.id.list_info_button);
        TextView alertList = convertView.findViewById(R.id.warning_list);
        TextView alertDesc = convertView.findViewById(R.id.warning_desc);
        Button alertBtn = convertView.findViewById(R.id.warning_button);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(alertList.getVisibility() == View.VISIBLE){
                    alertList.setVisibility(View.GONE);
                    alertDesc.setVisibility(View.GONE);
                    alertBtn.setVisibility(View.GONE);
                    listIcon.setImageResource(R.drawable.icon_list_right);
                }else{
                    alertList.setVisibility(View.VISIBLE);
                    alertDesc.setVisibility(View.VISIBLE);
                    alertBtn.setVisibility(View.VISIBLE);
                    listIcon.setImageResource(R.drawable.icon_list_down);
                }
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
                    alertDesc.setText(Desc[i]);
                    alertBtn.setText(buttonTxt[i]);
                    alertBtn.setCompoundDrawablesWithIntrinsicBounds(button_icons[i], 0, 0, 0);
                }
                count++;
            }
        }

        final int finalcount =index;
        alertBtn.setOnClickListener(new View.OnClickListener() {
            int position = finalcount;
            @Override
            public void onClick(View v) {
                if(alertBtn.getText().equals("Re-push")){
                    Log.d(TAG, "onClick: "+alertBtn);
                    ArrayList<String> ids = new ArrayList<>();
                    for(int i=0; i<AlertsByCategories.get(5).size();i++){
                        ids.add(String.valueOf(AlertsByCategories.get(5).get(i).getID()));
                    }
                    if(ids.size()>0) {
                        Set<String> selectedPeerIDs = new HashSet<>(ids);
                        main.getDispatcher().repushApp(selectedPeerIDs);
                    }
                }else if(alertBtn.getText().equals("Recall")){
                    main.getConnectedLearnersAdapter().selectAllPeers(false);
                    for(int i=0; i<AlertsByCategories.get(position).size();i++){
                        main.getConnectedLearnersAdapter().selectPeer(AlertsByCategories.get(position).get(i).getID(),true);
                    }
                    main.returnToAppFromMainAction(false);
                    main.getConnectedLearnersAdapter().selectAllPeers(false);
                }else if(alertBtn.getText().equals("Block")){
                    main.getConnectedLearnersAdapter().selectAllPeers(false);
                    for(int i=0; i<AlertsByCategories.get(position).size();i++){
                        main.getConnectedLearnersAdapter().selectPeer(AlertsByCategories.get(position).get(i).getID(),true);
                    }
                    main.blackoutFromMainAction();
                    main.getConnectedLearnersAdapter().selectAllPeers(false);
                }
//                else if(alertBtn.getText().equals("Clear")||alertBtn.getText().equals("Proceed Offline")){
//                    hideCurrentAlerts();
//                    for(int i=0; i<AlertsByCategories.get(position).size();i++){
//                        AlertsByCategories.get(position).get(i).hideAlerts(true);
//                    }
//                }
                else if(alertBtn.getText().equals("Launch")) {
                    ArrayList<String> ids = new ArrayList<>();
                    for (int i = 0; i < AlertsByCategories.get(2).size(); i++) {
                        ids.add(String.valueOf(AlertsByCategories.get(2).get(i).getID()));
                    }
                    if (ids.size() > 0) {
                        Set<String> selectedPeerIDs = new HashSet<>(ids);
                        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG,LeadMeMain.LAUNCH_ACCESS,selectedPeerIDs);
                    }
                }else if(alertBtn.getText().equals("Enable Transfer")) {
                    ArrayList<String> ids = new ArrayList<>();
                    for (int i = 0; i < AlertsByCategories.get(6).size(); i++) {
                        ids.add(String.valueOf(AlertsByCategories.get(6).get(i).getID()));
                    }

                    if (ids.size() > 0) {
                        Set<String> selectedPeerIDs = new HashSet<>(ids);
                        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.FILE_TRANSFER + ":"
                                + true, selectedPeerIDs);
                    }
                }else if(alertBtn.getText().equals("Enable Installer")) {
                    ArrayList<String> ids = new ArrayList<>();
                    for (int i = 0; i < AlertsByCategories.get(7).size(); i++) {
                        ids.add(String.valueOf(AlertsByCategories.get(7).get(i).getID()));
                    }

                    if (ids.size() > 0) {
                        Set<String> selectedPeerIDs = new HashSet<>(ids);
                        main.getDispatcher().sendActionToSelected(LeadMeMain.ACTION_TAG, LeadMeMain.AUTO_INSTALL + ":"
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