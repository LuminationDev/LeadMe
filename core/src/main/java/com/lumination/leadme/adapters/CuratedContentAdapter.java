package com.lumination.leadme.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.lumination.leadme.R;
import com.lumination.leadme.models.CuratedContentItem;
import com.lumination.leadme.LeadMeMain;
import com.lumination.leadme.databinding.RowCuratedContentItemBinding;

import java.util.ArrayList;

public class CuratedContentAdapter extends BaseAdapter {

    private final String TAG = "CuratedContentAdapter";

    public ArrayList<CuratedContentItem> curatedContentList = new ArrayList<>();
    private LayoutInflater mInflater;
    private LeadMeMain main;
    private View list_view;

    CuratedContentAdapter(LeadMeMain main, View list_view) {
        this.main = main;
        this.mInflater = LayoutInflater.from(main);
        this.list_view = list_view;
    }

    @Override
    public int getCount() {
        return curatedContentList != null ? curatedContentList.size() : 0;
    }

    @Override
    public CuratedContentItem getItem(int position) {
        return curatedContentList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return curatedContentList.get(position).id;
    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        View result = view;
        CuratedContentItem item = getItem(position);
        RowCuratedContentItemBinding binding;
        if (result == null) {
            if (mInflater == null) {
                mInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }
            binding = RowCuratedContentItemBinding.inflate(mInflater, parent, false);
            result = binding.getRoot();
            result.setTag(binding);
        } else {
            binding = (RowCuratedContentItemBinding) result.getTag();
        }

        LinearLayout listItem = result.findViewById(R.id.curated_content_list_item);
        if (position % 2 == 0) {
            listItem.setBackgroundResource(R.drawable.bg_light_grey);
        } else {
            listItem.setBackgroundResource(R.drawable.bg_white_unrounded);
        }

        binding.setCuratedContentItem(item);
        CheckBox fav = result.findViewById(R.id.fav_checkbox_curated_content);
        CuratedContentItem curatedContentItem = item;

        boolean isInFavourites = CuratedContentManager.isInFavourites(curatedContentItem.link, curatedContentItem.type);
        fav.setOnCheckedChangeListener(null);
        fav.setChecked(isInFavourites);
        fav.setOnCheckedChangeListener((button, checked) -> CuratedContentManager.addToFavourites(curatedContentItem.link, curatedContentItem.title, curatedContentItem.type, checked));

        ImageView imageView = result.findViewById(R.id.img_view);
        if (curatedContentItem.img_url != null) {
            UrlImageViewHelper.setUrlDrawable(imageView, curatedContentItem.img_url);
        }

        ImageView curatedContentTypeIcon = result.findViewById(R.id.curated_content_type_icon);
        switch (curatedContentItem.type) {
            case WITHIN:
                curatedContentTypeIcon.setBackground(main.getResources().getDrawable(R.drawable.search_within, null));
                break;
            case YOUTUBE:
                curatedContentTypeIcon.setBackground(main.getResources().getDrawable(R.drawable.core_yt_icon, null));
                break;
            case LINK:
                curatedContentTypeIcon.setBackground(main.getResources().getDrawable(R.drawable.task_website_icon, null));
                break;
        }

        return result;
    }
}