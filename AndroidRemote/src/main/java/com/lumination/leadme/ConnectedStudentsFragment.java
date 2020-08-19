package com.lumination.leadme;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConnectedStudentsFragment extends Fragment {
    private View mContentView = null;
    private GridView gridView;
    private ConnectedStudentsAdapter mAdapter;
    private MainActivity main;

    private final String TAG = "ConnectedStudents";

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            mContentView = inflater.inflate(R.layout.follower_list, null);
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        gridView = mContentView.findViewById(R.id.studentListView);

        main = (MainActivity) getActivity();
        Context applicationContext = main.getApplicationContext();

        // specify an adapter
        mAdapter = new ConnectedStudentsAdapter(applicationContext, new ArrayList<LumiPeer>());
        gridView.setAdapter(mAdapter);

        return mContentView;
    }

    public boolean hasConnectedStudents() {
        return mAdapter.mData.size() > 0;
    }

    public void refresh() {
        mAdapter.notifyDataUpdate();
    }

    public String getSelectedPeerIDList() {
        String res = "";
        for (LumiPeer peer : mAdapter.mData) {
            if (peer.isSelected()) {
                res += "," + peer.getID();
            }
        }
        Log.d(TAG, "SELECTED = " + res);
        return res;
    }

    public void selectAllPeers(boolean select) {
        Log.d(TAG, "Got: " + mAdapter.mData.size() + " and " + select);
        for (LumiPeer peer : mAdapter.mData) {
            peer.setSelected(select);
        }
        refresh();
    }

    public boolean removeStudent(String id) {
        LumiPeer found = getMatchingPeer(id);
        if (found != null) {
            //remove old one so we keep the newest version
            mAdapter.mData.remove(found);
            refresh();
            return true;
        }
        refresh();
        return false;
    }

    public void addStudent(LumiPeer peer) {
        //does someone with this name already exist?
        LumiPeer found = getMatchingPeer(peer.getID());

        if (found != null) {
            //remove old one so we keep the newest version
            mAdapter.mData.remove(found);
        }

        //add the newest version
        mAdapter.mData.add(peer);
        refresh();

        Log.d(TAG, "Adding " + peer.getDisplayName() + " to my student list. Now: " + mAdapter.mData.size());
    }

//    private LumiPeer getMatchingPeerByName(String peerName) {
//        if(mAdapter == null || mAdapter.mData == null || mAdapter.mData.size() == 0){
//            return null;
//        }
//
//        for (LumiPeer peer : mAdapter.mData) {
//            if (peer != null && peer.getDisplayName().equals(peerName)) {
//                return peer;
//            }
//        }
//        return null;
//    }

    private LumiPeer getMatchingPeer(String peerID) {
        if (mAdapter == null || mAdapter.mData == null || mAdapter.mData.size() == 0) {
            return null;
        }

        for (LumiPeer peer : mAdapter.mData) {
            if (peer != null && peer.getID().equals(peerID)) {
                return peer;
            }
        }
        return null;
    }

    public void updateLockStatus(String name, boolean locked) {
        LumiPeer thisPeer = getMatchingPeer(name);
        if (thisPeer != null) {
            thisPeer.setLocked(locked);
            refresh();
        }
    }

    public void updateStatus(String name, String status) {
        LumiPeer thisPeer = getMatchingPeer(name);
        if (thisPeer != null) {
            thisPeer.setStatus(status);
            refresh();
        }

        //certain states should alert the guide - e.g. failure to load or attempt to install
        if (status.contains("fail") || status.contains("install")) {
            Toast warningToast = Toast.makeText(main.getApplicationContext(), "WARNING: Peer " + thisPeer.getDisplayName() + " status is " + status, Toast.LENGTH_LONG);
            warningToast.show();
        }
    }

    public void clearPeers() {
        mAdapter.mData.clear();
        refresh();
    }

    public void setStudentList(Collection<LumiPeer> peers) {
        Log.i(TAG, "Updating student list: " + peers.size());
        mAdapter.mData.clear();
        mAdapter.mData.addAll(peers);
        refresh();
    }

    public class ConnectedStudentsAdapter extends BaseAdapter {
        private ArrayList<LumiPeer> mData = new ArrayList<>();
        private LayoutInflater mInflater;

        // data is passed into the constructor
        ConnectedStudentsAdapter(Context context, List<LumiPeer> data) {
            this.mInflater = LayoutInflater.from(context);
            mData.addAll(data);
        }

        public void notifyDataUpdate() {
            Log.i(TAG, "Updating! " + mData.size());
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

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_follower, parent, false);
            }

            TextView studentName = convertView.findViewById(R.id.student_name);
            TextView studentStatus = convertView.findViewById(R.id.student_status);
            ImageView studentLocked = convertView.findViewById(R.id.student_lock);
            ImageView studentIcon = convertView.findViewById(R.id.student_icon);

            final LumiPeer peer = mData.get(position);
            if (peer != null) {
                studentName.setText(peer.getDisplayName());
                studentStatus.setText(Html.fromHtml(peer.getStatus(), Html.FROM_HTML_MODE_LEGACY));

                if (peer.isSelected()) {
                    studentIcon.setImageResource(R.drawable.student_selectedface);
                } else {
                    studentIcon.setImageResource(R.drawable.student_placeholder);
                }

                if (peer.isLocked()) {
                    studentLocked.setImageResource(R.mipmap.lock_icon);
                } else {
                    studentLocked.setImageResource(R.mipmap.unlock_icon);
                }


                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.i(TAG, "Toggled on VIEW");
                        peer.toggleSelected();
                        notifyDataUpdate();
                    }
                });
            }
            return convertView;
        }

    }
}


