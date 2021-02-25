package com.lumination.leadme;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class StudentAlertsAdapter extends BaseAdapter {

    private final String TAG = "StudentAlerts";

    public ArrayList<ConnectedPeer> mData = new ArrayList<>();
    private LayoutInflater mInflater;
    private LeadMeMain main;
    private View list_view, no_alerts_view;

    StudentAlertsAdapter(LeadMeMain main, View list_view, View no_alerts_view) {
        this.main = main;
        this.mInflater = LayoutInflater.from(main);
        this.list_view = list_view;
        this.no_alerts_view = no_alerts_view;
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
        notifyDataSetChanged();
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (mData != null) {
            return mData.size();
        } else {
            return 0;
        }
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //if (convertView == null) {
        convertView = mInflater.inflate(R.layout.row_student_alert, parent, false);
        //}

        TextView studentName = convertView.findViewById(R.id.list_student_name);
        ImageView maxAlertIcon = convertView.findViewById(R.id.max_alert_icon);
        TextView alertList = convertView.findViewById(R.id.warning_list);

        final ConnectedPeer peer = mData.get(position);
        if (peer != null) {
            studentName.setText(peer.getDisplayName());
            if (peer.getStatus() == ConnectedPeer.STATUS_ERROR) {
                maxAlertIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.alert_error, null));
            } else {
                maxAlertIcon.setImageDrawable(main.getResources().getDrawable(R.drawable.alert_warning, null));
            }

            alertList.setText(peer.getAlertsList());
        }
        //refresh();
        return convertView;
    }


}