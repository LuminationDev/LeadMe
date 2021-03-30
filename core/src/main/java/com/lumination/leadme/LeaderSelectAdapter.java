package com.lumination.leadme;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;


public class LeaderSelectAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final ArrayList<ConnectedPeer> leader_list = new ArrayList<>();
    private final LeadMeMain main;
    private static final String TAG = "LeaderSelect";

    public LeaderSelectAdapter(LeadMeMain main) {
        this.main = main;
        inflater = (LayoutInflater.from(main));
//        leader_list.add(new ConnectedPeer("Sam Smith", "1234"));
//        leader_list.add(new ConnectedPeer("Anna Apple", "5678"));
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
        for (ConnectedPeer peer : leader_list) {
            if (peer.getID().equals(leader.getID())) {
                Log.d(TAG, "This one already exists " + leader);
                leader_list.remove(peer); //remove this so we don't have duplicates
            }
        }

        //this leader is not in the list, so add it
        Log.d(TAG, "Adding " + leader);
        leader_list.add(leader);
        notifyDataSetChanged();
    }

    public synchronized void removeLeader(String leaderID) {
        Log.d(TAG, "Removing leader " + leaderID + " from " + leader_list);
        ArrayList<ConnectedPeer> peersToRemove = new ArrayList<>();
        for (ConnectedPeer peer : leader_list) {
            Log.d(TAG, "Comparing: " + peer.getID() + " vs " + leaderID);
            if (peer.getID().equals(leaderID)) {
                peersToRemove.add(peer);
            }
        }
        for (ConnectedPeer removalPeer : peersToRemove) {
            removeLeader(removalPeer);
        }
        notifyDataSetChanged();
    }

    public synchronized void removeLeader(ConnectedPeer leader) {
        leader_list.remove(leader);
        if (leader_list.isEmpty()) {
            main.showLeaderWaitMsg(true);
        }
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
