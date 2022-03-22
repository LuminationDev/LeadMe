package com.lumination.leadme.models;

public class CuratedContentItem {
    public String title;
    public CuratedContentType type;
    public String link;
    public String description;
    public String years;
    public String subject;
    public String topics;
    public String img_url;
    public int id;

    public CuratedContentItem(int id, String title, CuratedContentType type, String link, String description, String years, String subject, String topics)
    {
        this.id = id;
        this.title = title;
        this.type = type;
        this.link = link;
        this.description = description;
        this.years = years;
        this.subject = subject;
        this.topics = topics;
    }
}
