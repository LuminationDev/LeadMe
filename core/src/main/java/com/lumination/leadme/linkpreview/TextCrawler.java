package com.lumination.leadme.linkpreview;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class TextCrawler {

    private final String TAG = "TextCrawler";

    public static final int ALL = -1;
    public static final int NONE = -2;

    private final String HTTP_PROTOCOL = "http://";
    private final String HTTPS_PROTOCOL = "https://";

    private LinkPreviewCallback callback;
    private AsyncTask getCodeTask;

    private UrlExtractionStrategy urlExtractionStrategy;

    public TextCrawler() {
    }

    public void makePreview(LinkPreviewCallback callback, String url) {
        ImagePickingStrategy imagePickingStrategy = new DefaultImagePickingStrategy();
        makePreview(callback, url, imagePickingStrategy);
    }

    public void makePreview(LinkPreviewCallback callback, String url, ImagePickingStrategy imagePickingStrategy) {
        this.callback = callback;
        cancel();
        getCodeTask = createPreviewGenerator(imagePickingStrategy).execute(url);
    }

    protected GetCode createPreviewGenerator(ImagePickingStrategy imagePickingStrategy) {
        return new GetCode(imagePickingStrategy, urlExtractionStrategy);
    }

    public void cancel() {
        if (getCodeTask != null) {
            getCodeTask.cancel(true);
        }
    }

    /**
     * Get html code
     */
    class GetCode extends AsyncTask<String, Void, Void> {

        private final SourceContent sourceContent = new SourceContent();
        private final ImagePickingStrategy imagePickingStrategy;
        private final UrlExtractionStrategy urlExtractionStrategy;

        GetCode(ImagePickingStrategy imagePickingStrategy, UrlExtractionStrategy urlExtractionStrategy) {
            this.imagePickingStrategy = imagePickingStrategy;
            if (urlExtractionStrategy == null) {
                urlExtractionStrategy = new DefaultUrlExtractionStrategy();
            }
            this.urlExtractionStrategy = urlExtractionStrategy;
        }

        @Override
        protected void onPreExecute() {
            if (callback != null) {
                callback.onPre();
            }
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void result) {
            if (callback != null) {
                callback.onPos(sourceContent, isNull());
            }
            super.onPostExecute(result);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected Void doInBackground(String... params) {
            final List<String> urlStrings = urlExtractionStrategy.extractUrls(params[0]);

            //TODO work out where the leak is and reintroduce this
//            String url;
//            if (urlStrings != null && !urlStrings.isEmpty()) {
//                url = unshortenUrl(urlStrings.get(0));
//            } else {
//                url = "";
//            }

            String url = urlStrings.get(0);

            sourceContent.setFinalUrl(url);
            boolean wasPreviewGenerationSuccessful = false;
            if (!url.equals("")) {
                if (isImage(url) && !url.contains("dropbox")) {
                    setSourceContentForImage();
                    wasPreviewGenerationSuccessful = true;
                } else {
                    try {
                        Document doc = getDocument();
                        if (doc != null) {
                            sourceContent.setHtmlCode(extendedTrim(doc.toString()));
                            HashMap<String, String> metaTags = getMetaTags(sourceContent.getHtmlCode());
                            sourceContent.setMetaTags(metaTags);
                            sourceContent.setTitle(metaTags.get("title"));
                            sourceContent.setDescription(metaTags.get("description"));

                            if (sourceContent.getTitle().equals("")) {
                                String matchTitle = Regex.pregMatch(sourceContent.getHtmlCode(), Regex.TITLE_PATTERN, 2);

                                if (!matchTitle.equals(""))
                                    sourceContent.setTitle(htmlDecode(matchTitle));
                            }

                            if (sourceContent.getDescription().equals(""))
                                sourceContent.setDescription(crawlCode(sourceContent.getHtmlCode()));

                            sourceContent.setDescription(sourceContent.getDescription().replaceAll(Regex.SCRIPT_PATTERN, ""));

                            if (imagePickingStrategy.getImageQuantity() != NONE) {
                                List<String> images;
                                images = imagePickingStrategy.getImages(getCodeTask, doc, metaTags);
                                sourceContent.setImages(images);
                            }

                            wasPreviewGenerationSuccessful = true;

                        } else {
                            wasPreviewGenerationSuccessful = false;
                        }

                    } catch (Throwable t) {
                        t.printStackTrace();

                        if (t instanceof UnsupportedMimeTypeException) {
                            final String mimeType = ((UnsupportedMimeTypeException) t).getMimeType();
                            if (mimeType != null && mimeType.startsWith("image")) {
                                setSourceContentForImage();
                                wasPreviewGenerationSuccessful = true;
                            }
                        }
                    }
                }
                sourceContent.setSuccess(wasPreviewGenerationSuccessful);
            }

            String[] finalLinkSet = sourceContent.getFinalUrl().split("&");
            sourceContent.setUrl(finalLinkSet[0]);
            sourceContent.setDescription(stripTags(sourceContent.getDescription()));

            return null;
        }

        /**
         * Configures the sourceContent for an Image.
         */
        private void setSourceContentForImage() {
            sourceContent.getImages().add(sourceContent.getFinalUrl());
            sourceContent.setTitle("");
            sourceContent.setDescription("");
        }

        protected Document getDocument() throws IOException {
            Connection conn = null;
            Document doc = null;
            try {
                conn = Jsoup.connect(sourceContent.getFinalUrl()).userAgent("Mozilla");
                doc = conn.get();

                return doc;

            } catch (HttpStatusException e) {
                Log.e(TAG, "HTTP Error (probably offline): " + e.getStatusCode() + " - " + e.getMessage());
                //Log.e(TAG, "Conn: "+conn);
                //e.printStackTrace();
                return null;

            } catch (InterruptedIOException e) {
                Log.e(TAG, "IO Error (probably a timeout): " + e.bytesTransferred + ", " + e.getMessage());
                //Log.e(TAG, "Conn: "+conn);
                //e.printStackTrace();
                return null;

            } catch (Exception e) {
                Log.e(TAG, "OTHER Error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        /**
         * Verifies if the content could not be retrieved
         */
        public boolean isNull() {
            return !sourceContent.isSuccess() &&
                    extendedTrim(sourceContent.getHtmlCode()).equals("") &&
                    !isImage(sourceContent.getFinalUrl());
        }

    }

    /**
     * Gets content from a html tag
     */
    private String getTagContent(String tag, String content) {

        String pattern = "<" + tag + "(.*?)>(.*?)</" + tag + ">";
        String result = "", currentMatch = "";

        List<String> matches = Regex.pregMatchAll(content, pattern, 2);

        int matchesSize = matches.size();
        for (int i = 0; i < matchesSize; i++) {
            if (getCodeTask.isCancelled()) {
                break;
            }
            currentMatch = stripTags(matches.get(i));
            if (currentMatch.length() >= 120) {
                result = extendedTrim(currentMatch);
                break;
            }
        }

        if (result.equals("")) {
            String matchFinal = Regex.pregMatch(content, pattern, 2);
            result = extendedTrim(matchFinal);
        }

        result = result.replaceAll("&nbsp;", "");

        return htmlDecode(result);
    }

    /**
     * Transforms from html to normal string
     */
    private String htmlDecode(String content) {
        return Jsoup.parse(content).text();
    }

    /**
     * Crawls the code looking for relevant information
     */
    private String crawlCode(String content) {
        String resultSpan = getTagContent("span", content);
        String resultParagraph = getTagContent("p", content);
        String resultDiv = getTagContent("div", content);

        String result;

        if (resultParagraph.length() > resultSpan.length()
                && resultParagraph.length() >= resultDiv.length())
            result = resultParagraph;
        else if (resultParagraph.length() > resultSpan.length()
                && resultParagraph.length() < resultDiv.length())
            result = resultDiv;
        else
            result = resultParagraph;

        return htmlDecode(result);
    }

    /**
     * Returns the cannoncial url
     */
    private String cannonicalPage(String url) {

        String cannonical = "";
        if (url.startsWith(HTTP_PROTOCOL)) {
            url = url.substring(HTTP_PROTOCOL.length());
        } else if (url.startsWith(HTTPS_PROTOCOL)) {
            url = url.substring(HTTPS_PROTOCOL.length());
        }

        int urlLength = url.length();
        for (int i = 0; i < urlLength; i++) {
            if (getCodeTask.isCancelled()) {
                break;
            }
            if (url.charAt(i) != '/')
                cannonical += url.charAt(i);
            else
                break;
        }

        return cannonical;

    }

    /**
     * Strips the tags from an element
     */
    private String stripTags(String content) {
        return Jsoup.parse(content).text();
    }

    /**
     * Verifies if the url is an image
     */
    private boolean isImage(String url) {
        return url.matches(Regex.IMAGE_PATTERN);
    }

    /**
     * Returns meta tags from html code
     */
    private HashMap<String, String> getMetaTags(String content) {

        HashMap<String, String> metaTags = new HashMap<String, String>();
        metaTags.put("url", "");
        metaTags.put("title", "");
        metaTags.put("description", "");
        metaTags.put("image", "");

        List<String> matches = Regex.pregMatchAll(content,
                Regex.METATAG_PATTERN, 1);

        for (String match : matches) {
            if (getCodeTask.isCancelled()) {
                break;
            }
            final String lowerCase = match.toLowerCase();
            if (lowerCase.contains("property=\"og:url\"")
                    || lowerCase.contains("property='og:url'")
                    || lowerCase.contains("name=\"url\"")
                    || lowerCase.contains("name='url'"))
                updateMetaTag(metaTags, "url", separeMetaTagsContent(match));
            else if (lowerCase.contains("property=\"og:title\"")
                    || lowerCase.contains("property='og:title'")
                    || lowerCase.contains("name=\"title\"")
                    || lowerCase.contains("name='title'"))
                updateMetaTag(metaTags, "title", separeMetaTagsContent(match));
            else if (lowerCase
                    .contains("property=\"og:description\"")
                    || lowerCase
                    .contains("property='og:description'")
                    || lowerCase.contains("name=\"description\"")
                    || lowerCase.contains("name='description'"))
                updateMetaTag(metaTags, "description", separeMetaTagsContent(match));
            else if (lowerCase.contains("property=\"og:image\"")
                    || lowerCase.contains("property='og:image'")
                    || lowerCase.contains("name=\"image\"")
                    || lowerCase.contains("name='image'"))
                updateMetaTag(metaTags, "image", separeMetaTagsContent(match));
        }

        return metaTags;
    }

    private void updateMetaTag(HashMap<String, String> metaTags, String url, String value) {
        if (value != null && (value.length() > 0)) {
            metaTags.put(url, value);
        }
    }

    /**
     * Gets content from metatag
     */
    private String separeMetaTagsContent(String content) {
        String result = Regex.pregMatch(content, Regex.METATAG_CONTENT_PATTERN,
                1);
        return htmlDecode(result);
    }

    /**
     * Unshortens a short url
     */
    private String unshortenUrl(final String originURL) {
        if (!originURL.startsWith(HTTP_PROTOCOL) && !originURL.startsWith(HTTPS_PROTOCOL)) {
            return "";
        }

        HttpURLConnection urlConn = connectURL(originURL);
        urlConn.getHeaderFields();

        final URL finalUrl = urlConn.getURL();
        urlConn.disconnect(); //disconnect before connecting with new URL

        urlConn = connectURL(finalUrl);
        urlConn.getHeaderFields();

        final URL shortURL = urlConn.getURL();

        String finalResult = shortURL.toString();

        while (!shortURL.sameFile(finalUrl)) {
            boolean isEndlesslyRedirecting = false;
            if (shortURL.getHost().equals(finalUrl.getHost())) {
                if (shortURL.getPath().equals(finalUrl.getPath())) {
                    isEndlesslyRedirecting = true;
                }
            }
            if (isEndlesslyRedirecting) {
                break;
            } else {
                finalResult = unshortenUrl(shortURL.toString());
            }
        }

        return finalResult;
    }

    /**
     * Takes a valid url string and returns a URLConnection object for the url.
     */
    private HttpURLConnection connectURL(String strURL) {
        HttpURLConnection conn = null;
        try {
            URL inputURL = new URL(strURL);
            conn = connectURL(inputURL);
        } catch (MalformedURLException e) {
            System.out.println("Please input a valid URL");
            e.printStackTrace();
        }
        return conn;
    }

    /**
     * Takes a valid url and returns a URLConnection object for the url.
     */
    private HttpURLConnection connectURL(URL inputURL) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) inputURL.openConnection();
        } catch (IOException ioe) {

            System.out.println("Cannot connect to the URL");
            ioe.printStackTrace();
        }
        return conn;
    }

    /**
     * Removes extra spaces and trim the string
     */
    public static String extendedTrim(String content) {
        return content.replaceAll("\\s+", " ").replace("\n", " ")
                .replace("\r", " ").trim();
    }

}
