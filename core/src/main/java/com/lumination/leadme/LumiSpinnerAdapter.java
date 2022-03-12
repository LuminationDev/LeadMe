package com.lumination.leadme;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class LumiSpinnerAdapter extends ArrayAdapter<String> {

    private Context ctx;
    private String[] contentArray;
    private Integer[] imageArray;

    public LumiSpinnerAdapter(Context context, int resource, String[] objects, Integer[] imageArray) {
        super(context, R.layout.row_push_spinner, R.id.spinner_item, objects);
        this.ctx = context;
        this.contentArray = objects;
        this.imageArray = imageArray;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.row_push_spinner, parent, false);

        TextView textView = (TextView) row.findViewWithTag("spinner_text");
        textView.setText(contentArray[position]);

        ImageView imageView = (ImageView) row.findViewWithTag("spinner_img");
        imageView.setImageResource(imageArray[position]);

        return row;
    }
}
