package com.lumination.leadme.linkpreview;


import java.util.List;

/**
 * Provides the means for extracting URL(s) from text.
 */
public interface UrlExtractionStrategy {
    List<String> extractUrls(String textPassedToTextCrawler);
}
