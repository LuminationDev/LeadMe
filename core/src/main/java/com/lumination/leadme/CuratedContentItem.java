package com.lumination.leadme;

// todo - still not sold on using an enum here, might be painful with mapping into the view and more data validation than it's worth - will see 11/03/22
enum CuratedContentType {
    YOUTUBE,
    WITHIN,
    LINK
}

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
