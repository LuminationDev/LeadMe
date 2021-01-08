package com.lumination.leadme;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
class videoData{
    String ID;
    String thumbURL;
    String Title;
}
public class RecyclerAdaptor extends RecyclerView.Adapter<RecyclerAdaptor.ViewHolder> {

    private List<videoData> mData= new ArrayList<>();;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    RecyclerAdaptor(Context context, Iterator<SearchResult> data) {
        this.mInflater = LayoutInflater.from(context);
        while (data.hasNext()) {

            SearchResult singleVideo = data.next();
            ResourceId rId = singleVideo.getId();

            // Confirm that the result represents a video. Otherwise, the
            // item will not contain a video ID.
            if (rId.getKind().equals("youtube#video")) {
                Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();
                videoData VideoData = new videoData();
                VideoData.Title = singleVideo.getSnippet().getTitle();
                VideoData.ID = rId.getVideoId();
                VideoData.thumbURL = thumbnail.getUrl();
                mData.add(VideoData);
            }
        }

    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.url_recycler_adaptor, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String previewText = mData.get(position).Title;
        holder.myTextView.setText(previewText);
        Glide.with(holder.myImageView.getContext()).load(mData.get(position).thumbURL).into(holder.myImageView);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }



    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTextView;
        ImageView myImageView;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.url_preview_text);
            itemView.setOnClickListener(this);

            myImageView = itemView.findViewById(R.id.url_preview_image);
            myImageView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mData.get(id).ID;
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);

        void onPointerCaptureChanged(boolean hasCapture);
    }
}
