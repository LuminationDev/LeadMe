package com.lumination.leadme;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Control the list of leaders available for connection by learners. This is controlled either by
 * the Nsd Services a learner has resolved or by the retrieveLeaders call to the Firestore. Each
 * leader element contains a teachers username and IP address.
 */
public class LeaderSelectAdapter extends BaseAdapter {
    private static final String TAG = "LeaderSelect";
    private final LayoutInflater inflater;
    private final ArrayList<ConnectedPeer> leader_list = new ArrayList<>();
    private final LeadMeMain main;

    public LeaderSelectAdapter(LeadMeMain main) {
        this.main = main;
        inflater = (LayoutInflater.from(main));
        Log.d(TAG, "Created adapter!!");
    }

    public synchronized void setLeaderList(ArrayList<ConnectedPeer> leaders) {
        Log.d(TAG, "Setting leader list to: " + leaders);
        leader_list.clear();
        leader_list.addAll(leaders);
        if (leader_list.isEmpty()) {
            main.showLeaderWaitMsg(true);
        }
        notifyDataSetChanged();
    }

    public synchronized void addLeader(ConnectedPeer leader) {
        ArrayList<ConnectedPeer> peersForRemoval = new ArrayList<>();
        for (ConnectedPeer peer : leader_list) {
            if (peer.getID().equals(leader.getID())) {
                Log.d(TAG, "This one already exists: " + leader.getDisplayName() + " : " + leader.getID());
                peersForRemoval.add(peer);
            }
        }

        if (!peersForRemoval.isEmpty()) {
            leader_list.removeAll(peersForRemoval);
        }

        //this leader is not in the list, so add it
        Log.d(TAG, "Adding " + leader.getDisplayName());
        leader_list.add(leader);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return leader_list.size();
    }

    @Override
    public Object getItem(int position) {
        return leader_list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            //root must be null or it will crash
            convertView = inflater.inflate(R.layout.row_leader, null);
        }
        final TextView textView = convertView.findViewById(R.id.leader_name);
        textView.setText(leader_list.get(position).getDisplayName());

        convertView.setOnClickListener(v -> {
            Log.d(TAG, "Clicked view: " + textView.getText());
            main.getNearbyManager().setSelectedLeader(leader_list.get(position));
            main.showLoginDialog();
        });

        return convertView;
    }
}
