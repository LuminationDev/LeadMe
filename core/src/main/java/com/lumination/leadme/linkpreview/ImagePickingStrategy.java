package com.lumination.leadme.linkpreview;

import android.os.AsyncTask;

import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.List;

/**
 * A strategy for how to select the images to return.
 */
public interface ImagePickingStrategy {

    int getImageQuantity();

    List<String> getImages(AsyncTask asyncTask, Document doc, HashMap<String, String> metaTags);
}
