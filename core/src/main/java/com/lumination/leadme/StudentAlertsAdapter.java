package com.lumination.leadme;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
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
    6. Unspecified
     */
    private ArrayList<ArrayList<ConnectedPeer>> AlertsByCategories = new ArrayList<>();

    StudentAlertsAdapter(LeadMeMain main, View list_view, View no_alerts_view) {
        this.main = main;
        this.mInflater = LayoutInflater.from(main);
        this.list_view = list_view;
        this.no_alerts_view = no_alerts_view;
        for(int i=0; i<7; i++){
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
        for(int i=0; i<7; i++){
            AlertsByCategories.add(new ArrayList<>());
        }
        for(int position=0; position<7; position++) {
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
                        if (peer.getAlertsList().contains("Locking overlay disabled")) {
                            AlertsByCategories.get(4).add(peer);
                        }
                    }
                    break;
                case 5:
                    Iterator<ConnectedPeer> iterator5 = mData.iterator();
                    while (iterator5.hasNext()) {
                        ConnectedPeer peer = iterator5.next();
                        if (peer.getAlertsList().contains("Might be off task")) {
                            AlertsByCategories.get(5).add(peer);
                        }
                    }
                    break;
                case 6:
                    Iterator<ConnectedPeer> iterator6 = mData.iterator();
                    while (iterator6.hasNext()) {
                        ConnectedPeer peer = iterator6.next();
                        if (peer.getAlertsList().contains("Unspecified warning")) {
                            AlertsByCategories.get(6).add(peer);
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
    String Types[] = {"Disconnected","Last App Didn't Launch","Accessibility is Disabled","Overlay is Disabled","No internet connection","Student May Be Off Task","Uncategorised Warnings"};
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //if (convertView == null) {
        convertView = mInflater.inflate(R.layout.row_student_alert, parent, false);
        //}

        TextView alertType = convertView.findViewById(R.id.list_alert_type);
        ImageView listIcon = convertView.findViewById(R.id.list_drop_icon);
        ImageView infoIcon = convertView.findViewById(R.id.list_info_button);
        TextView alertList = convertView.findViewById(R.id.warning_list);
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(alertList.getVisibility() == View.VISIBLE){
                    alertList.setVisibility(View.GONE);
                    listIcon.setImageResource(R.drawable.icon_list_right);
                }else{
                    alertList.setVisibility(View.VISIBLE);
                    listIcon.setImageResource(R.drawable.icon_list_down);
                }
            }
        });

        Log.d(TAG, "getView: "+position);
        String nameList="";
        int index = -1;
        int count =0;
        for(int i=0; i<AlertsByCategories.size(); i++) {
            if(AlertsByCategories.get(i).size()!=0){
                if(count==position) {
                    index=i;
                    int numStud=0;
                    for (int j = 0; j < AlertsByCategories.get(i).size(); j++) {
                        Log.d(TAG, "AlertsByCategories "+i);
                        nameList += "• " + AlertsByCategories.get(i).get(j).getDisplayName() + "\n";
                        numStud++;
                    }
                    alertType.setText(Types[i]+" ("+numStud+")");
                }
                count++;
            }
        }
        final int finalcount =index;
        infoIcon.setOnClickListener(new View.OnClickListener() {
            int counter=finalcount;
            @Override
            public void onClick(View v) {
//                Toast toast=Toast.makeText(main.getApplicationContext(),"Working",Toast.LENGTH_LONG);
//                toast.setMargin(50,50);
//                toast.show();
                buildAndShowError(counter);

            }
        });
        Log.d(TAG, "getView: "+nameList);
        alertList.setText(nameList);
        //refresh();
        return convertView;
    }
    public void buildAndShowError(int errorPos){
        View OffTask = View.inflate(main, R.layout.e__error_dialog, null);
        Spinner lockSpinner;
        String[]  lockSpinnerItems = new String[0];
        lockSpinner = (Spinner) OffTask.findViewById(R.id.push_spinner);
        Button errorBtn = OffTask.findViewById(R.id.push_btn);
        Button backBtn = OffTask.findViewById(R.id.back_btn);
        TextView errorText = OffTask.findViewById(R.id.error_description);
        TextView errorTitle = OffTask.findViewById(R.id.error_dialog_title);
        lockSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemSelected: "+parent.getItemAtPosition(position));
                parent.getItemAtPosition(position);
                //TextView textV = v.findViewById(R.id.spinner_item);
                String text = (String) parent.getItemAtPosition(position);
                if(text.contains(" ")&&!text.contains("Proceed")){
                    errorBtn.setText(text.substring(0, text.indexOf(" ")));
                }else {
                    errorBtn.setText(text);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        Integer[] push_imgs=new Integer[0];
        switch(errorPos){
            case 0:
                lockSpinnerItems = new String[1];
                lockSpinnerItems[0] = "Clear";
                push_imgs = new Integer[]{R.drawable.icon_clear};
                errorTitle.setText("Disconnected");
                errorText.setText("Learners have disconnected from LeadMe.\n" +
                        "\n" +
                        "Have learners login again to reconnect.");
                break;
            case 1:
                lockSpinnerItems = new String[1];
                lockSpinnerItems[0] = "Re-push Last Task";
                push_imgs = new Integer[]{R.drawable.icon_repush};
                errorTitle.setText("App not installed");
                errorText.setText("The application that was pushed does not exist on learner devices.\n" +
                        "\n" +
                        "Make sure app is downloaded and installed on learner devices, then re-push task.");
                break;
            case 2:
                lockSpinnerItems = new String[1];
                lockSpinnerItems[0] = "Clear";
                push_imgs = new Integer[]{R.drawable.icon_clear};
                errorTitle.setText("Accessibility Disabled");
                errorText.setText("Learners have no enabled accessibility for LeadMe.\n" +
                        "\n" +
                        "Have learners open their device settings, open accessibility, and under services, make sure Lumination LeadMe is switched on.");
                break;
            case 3:
                lockSpinnerItems = new String[1];
                lockSpinnerItems[0] = "Clear";
                push_imgs = new Integer[]{R.drawable.icon_clear};
                errorTitle.setText("Overlay Disabled");
                errorText.setText("Learner have not enabled screen overlay for LeadMe.\n" +
                        "\n" +
                        "Have learners open their device settings, search 'display over other apps', locate LeadMe and toggle 'ON'.\n" +
                        "\n");
                break;
            case 4:
                lockSpinnerItems = new String[2];
                lockSpinnerItems[0] = "Proceed Offline";
                lockSpinnerItems[1] = "Re-push Last Task";
                push_imgs = new Integer[]{R.drawable.icon_clear};
                errorTitle.setText("Offline");
                errorText.setText("Task may have not launched successfully as learners are not connected to the internet.\n" +
                        "\n" +
                        "Have learners connect to wifi and re-push task for full internet access, or proceed with offline functionality.");
                break;
            case 5:
                lockSpinnerItems = new String[3];
                lockSpinnerItems[0] = "Re-push Last Task";
                lockSpinnerItems[1] = "Recall To LeadMe";
                lockSpinnerItems[2] = "Block";
                push_imgs = new Integer[]{R.drawable.icon_repush, R.drawable.arrow_icon,R.drawable.controls_block};
                errorTitle.setText("Might be off task");
                errorText.setText("Learners have exited the current task and may be using the wrong application.\n\n Select re-push to return all to the current task, or select an option from the dropdown.");
                break;
            case 6:
                lockSpinnerItems = new String[1];
                lockSpinnerItems[0] = "Clear";
                push_imgs = new Integer[]{R.drawable.icon_clear};
                errorTitle.setText("Uncategorised Error");
                errorText.setText("To be completely honest, I'm not really sure how we got here.\n" +
                        "\n"+
                        "You should probably try restarting the student device");
                break;
        }

        SpinnerAdapter push_adapter = new SpinnerAdapter(main, R.layout.row_push_spinner, lockSpinnerItems, push_imgs);
        lockSpinner.setAdapter(push_adapter);
        lockSpinner.setSelection(0); //default to locked
        AlertDialog errors = new AlertDialog.Builder(main)
                .setView(OffTask)
                .create();
        errors.show();
        errorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(errorBtn.getText().equals("Re-push")){
                    Log.d(TAG, "onClick: "+errorPos);
                    ArrayList<String> ids = new ArrayList<>();
                    for(int i=0; i<AlertsByCategories.get(5).size();i++){
                        ids.add(String.valueOf(AlertsByCategories.get(5).get(i).getID()));
                    }
                    if(ids.size()>0) {
                        Set<String> selectedPeerIDs = new HashSet<>(ids);
                        main.getDispatcher().repushApp(selectedPeerIDs);
                    }
                }else if(errorBtn.getText().equals("Recall")){
                    main.getConnectedLearnersAdapter().selectAllPeers(false);
                    for(int i=0; i<AlertsByCategories.get(errorPos).size();i++){
                        main.getConnectedLearnersAdapter().selectPeer(AlertsByCategories.get(errorPos).get(i).getID(),true);
                    }
                    main.returnToAppFromMainAction(false);
                    main.getConnectedLearnersAdapter().selectAllPeers(false);
                }else if(errorBtn.getText().equals("Block")){
                    main.getConnectedLearnersAdapter().selectAllPeers(false);
                    for(int i=0; i<AlertsByCategories.get(errorPos).size();i++){
                        main.getConnectedLearnersAdapter().selectPeer(AlertsByCategories.get(errorPos).get(i).getID(),true);
                    }
                    main.blackoutFromMainAction();
                    main.getConnectedLearnersAdapter().selectAllPeers(false);
                }else if(errorBtn.getText().equals("Clear")||errorBtn.getText().equals("Proceed Offline")){
                    for(int i=0; i<AlertsByCategories.get(errorPos).size();i++){
                        AlertsByCategories.get(errorPos).get(i).hideAlerts(true);
                    }
                }
                errors.hide();
            }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errors.hide();
            }
        });

    }
}