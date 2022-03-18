package com.lumination.leadme.models;

public class CuratedContentItem {
    public String title;
    public CuratedContentType type;
    public String link;
    public String description;
    public String years;
    public String subject;
    public int id;

    public CuratedContentItem(int id, String title, CuratedContentType type, String link, String description, String years, String subject)
    {
        this.id = id;
        this.title = title;
        this.type = type;
        this.link = link;
        this.description = description;
        this.years = years;
        this.subject = subject;
    }
}
