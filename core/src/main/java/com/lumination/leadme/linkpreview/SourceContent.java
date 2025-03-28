package com.lumination.leadme.linkpreview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SourceContent {

    private boolean success = false;
    private String htmlCode = "";
    private String title = "";
    private String description = "";
    private String url = "";
    private String finalUrl = "";
    private HashMap<String, String> metaTags = new HashMap<String, String>();

    private List<String> images = new ArrayList<String>();

    public SourceContent() {
    }

    /**
     * @return the success
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @param success the success to set
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * @return the htmlCode
     */
    public String getHtmlCode() {
        return htmlCode;
    }

    /**
     * @param htmlCode the htmlCode to set
     */
    public void setHtmlCode(String htmlCode) {
        this.htmlCode = htmlCode;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the finalUrl
     */
    public String getFinalUrl() {
        return finalUrl;
    }

    /**
     * @param finalUrl the finalUrl to set
     */
    public void setFinalUrl(String finalUrl) {
        this.finalUrl = finalUrl;
    }

    /**
     * @param metaTags the metaTags to set
     */
    public void setMetaTags(HashMap<String, String> metaTags) {
        this.metaTags = metaTags;
    }

    /**
     * @return the images
     */
    public List<String> getImages() {
        return images;
    }

    /**
     * @param images the images to set
     */
    public void setImages(List<String> images) {
        this.images = images;
    }

}
