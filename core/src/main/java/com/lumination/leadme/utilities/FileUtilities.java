package com.lumination.leadme.utilities;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;

import com.lumination.leadme.LeadMeMain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * A class for picking files from the storage of a device.
 * NOTE: Some Xiamoi devices need to have MUI Optimizations turned on within the developer options
 * in order to return a file to onActivityResult, otherwise result will always be 0.
 * If MUI Optmizations is not shown, Click the reset to default values in the Autofill section a
 * few times and the option will appear.
 *
 * Note: Xiaomi phones do not delete files sometimes, it will stay hidden in memory and if trying
 * to search for it, it will say it exists however it cannot be accessed. Need to install 'File by Google'
 * in order to fully remove the file.
 */
public class FileUtilities {

    private static final String TAG = "FileUtils";

    private static Uri contentUri = null;

    /**
     * Browse the phones files - reuse this part for selecting files for viewing or selection for
     * transfer.
     * @param RequestType An integer representing the type of request, used when returning a
     *                    result.
     */
    public static void browseFiles(LeadMeMain main, int RequestType) {
        Intent chooseFileIntent = new Intent();

        chooseFileIntent.setType("*/*");
        chooseFileIntent.setAction(Intent.ACTION_GET_CONTENT);

        // Only return URIs that can be opened with ContentResolver
        chooseFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFileIntent = Intent.createChooser(chooseFileIntent, "Choose a file");

        main.startActivityForResult(chooseFileIntent, RequestType);
    }

    /**
     * Search the local storage, can only search one 'block' at a time.
     * @param main A reference to the main activity.
     * @param fileName A string representing the file to search for.
     * @return A Uri of the file, if null the file doesn't exist.
     */
    public static Uri searchStorage(LeadMeMain main, String fileName) {
        Uri foundUri;

        boolean isMovie = fileName.toLowerCase().contains(".mp4")
                || fileName.toLowerCase().contains(".avi")
                || fileName.toLowerCase().contains(".mkv")
                || fileName.toLowerCase().contains(".webm")
                || fileName.toLowerCase().contains(".mov");

        boolean isPicture = fileName.toLowerCase().contains(".jpg")
                || fileName.toLowerCase().contains(".jpeg")
                || fileName.toLowerCase().contains(".png");

        boolean isDocument = fileName.toLowerCase().contains(".pdf");

        if (isPicture) {
            foundUri = FileUtilities.getPhotoFileByName(main, fileName);
        } else if (isMovie) {
            foundUri = FileUtilities.getVideoFileByName(main, fileName);
        } else if (isDocument) {
            foundUri = FileUtilities.getDocumentFileByName(main, fileName);
        } else {
            return null;
        }

        return foundUri;
    }

    private static Uri getPhotoFileByName(LeadMeMain main, String fileName) {
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String selection = MediaStore.Images.Media.DISPLAY_NAME + "= ?";
        String column = MediaStore.Images.ImageColumns._ID;

        Log.d(TAG, "Searching for a photo");

        return getFileByName(uri, selection, column, main, fileName);
    }

    private static Uri getVideoFileByName(LeadMeMain main, String fileName) {
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String selection = MediaStore.Video.Media.DISPLAY_NAME + "= ?";
        String column = MediaStore.Video.VideoColumns._ID;

        Log.d(TAG, "Searching for a video");

        return getFileByName(uri, selection, column, main, fileName);
    }

    private static Uri getDocumentFileByName(LeadMeMain main, String fileName) {
        Uri uri = MediaStore.Files.getContentUri("external");

        String selection = MediaStore.Files.FileColumns.DISPLAY_NAME + "= ?";
        String column = MediaStore.Files.FileColumns._ID;

        Log.d(TAG, "Searching for a document");

        return getFileByName(uri, selection, column, main, fileName);
    }

    /**
     * Search for a file within the MediaStore by file name. Using the supplied file name.
     * @param main A reference to the LeadMe main activity.
     * @param fileName A string representing the name of the file to be searched for.
     * @return A Uri of the file that has been found.
     */
    private static Uri getFileByName(Uri uri, String selection, String column, LeadMeMain main, String fileName) {
        Uri foundUri = null;

        ContentResolver cr = main.getContentResolver();

        String[] selectionArguments = {fileName};

        Cursor cursor = cr.query(uri, null, selection, selectionArguments, null);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            foundUri = ContentUris.withAppendedId(uri, cursor.getInt(cursor.getColumnIndex(column)));

            cursor.close();
        }

        return foundUri;
    }

    /**
     * Search for a file on older android versions. Currently searches through all the storage
     * directories, if supplied with Environment.getExternalDirectories().
     * @param directory A String representing the root directory of where to start the search.
     * @param name A string representing the name of the file being searched for.
     */
    public static File findFile(File directory, String name) {
        File[] children = directory.listFiles();

        for(File child : children) {
            if(child.isDirectory()) {
                File found = findFile(child, name);
                if(found != null) {
                    return found;
                }
            } else {
                if(name.equals(child.getName())) {
                    return child;
                }
            }
        }

        return null;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {
        // Check here to KITKAT or new version
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        String selection = null;
        String[] selectionArgs = null;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");

                String fullPath = getPathFromExtSD(split);
                if (fullPath != "") {
                    return fullPath;
                } else {
                    return null;
                }
            }

            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    final String id;
                    Cursor cursor = null;
                    try {
                        cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            String fileName = cursor.getString(0);
                            String path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                            if (!TextUtils.isEmpty(path)) {
                                return path;
                            }
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                    id = DocumentsContract.getDocumentId(uri);
                    if (!TextUtils.isEmpty(id)) {
                        if (id.startsWith("raw:")) {
                            return id.replaceFirst("raw:", "");
                        }
                        String[] contentUriPrefixesToTry = new String[] {
                                "content://downloads/public_downloads",
                                "content://downloads/my_downloads"
                        };
                        for (String contentUriPrefix : contentUriPrefixesToTry) {
                            try {
                                final Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));

                                // final Uri contentUri = ContentUris.withAppendedId(
                                //        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                                return getDataColumn(context, contentUri, null, null);
                            } catch (NumberFormatException e) {
                                // In Android 8 and Android P the id is not a number
                                return uri.getPath().replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
                            }
                        }
                    }
                } else {
                    final String id = DocumentsContract.getDocumentId(uri);
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                    try {
                        contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (contentUri != null) {
                        return getDataColumn(context, contentUri, null, null);
                    }
                }
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;

                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            } else if (isGoogleDriveUri(uri)) {
                return getDriveFilePath(uri, context);
            }
        }


        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }

            if (isGoogleDriveUri(uri)) {
                return getDriveFilePath(uri, context);
            }
            if( Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
                // return getFilePathFromURI(context,uri);
                return getMediaFilePathForN(uri, context);
                // return getRealPathFromURI(context,uri);
            } else {
                return getDataColumn(context, uri, null, null);
            }
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Check if a file exists on device
     *
     * @param filePath The absolute file path
     */
    private static boolean fileExists(String filePath) {
        File file = new File(filePath);

        return file.exists();
    }


    /**
     * Get full file path from external storage
     *
     * @param pathData The storage type and the relative path
     */
    private static String getPathFromExtSD(String[] pathData) {
        final String type = pathData[0];
        final String relativePath = "/" + pathData[1];
        String fullPath = "";

        // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
        // something like "71F8-2C0A", some kind of unique id per storage
        // don't know any API that can get the root path of that storage based on its id.
        //
        // so no "primary" type, but let the check here for other devices
        if ("primary".equalsIgnoreCase(type)) {
            fullPath = Environment.getExternalStorageDirectory() + relativePath;
            if (fileExists(fullPath)) {
                return fullPath;
            }
        }

        // Environment.isExternalStorageRemovable() is `true` for external and internal storage
        // so we cannot relay on it.
        //
        // instead, for each possible path, check if file exists
        // we'll start with secondary storage as this could be our (physically) removable sd card
        fullPath = System.getenv("SECONDARY_STORAGE") + relativePath;
        if (fileExists(fullPath)) {
            return fullPath;
        }

        fullPath = System.getenv("EXTERNAL_STORAGE") + relativePath;
        if (fileExists(fullPath)) {
            return fullPath;
        }

        return fullPath;
    }

    private static String getDriveFilePath(Uri uri, Context context) {
        Uri returnUri = uri;
        ContentResolver contentResolver = context.getContentResolver();
        Cursor returnCursor = contentResolver.query(returnUri, null, null, null, null);

        // Get the column indexes of the data in the Cursor,
        // move to the first row in the Cursor, get the data,
        // and display it.

        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        File file = new File(context.getCacheDir(), name);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            int maxBufferSize = 1 * 1024 * 1024;
            int bytesAvailable = inputStream.available();

            // int bufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);

            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
//            Log.e("File Size", "Size " + file.length());
            inputStream.close();
            outputStream.close();
            Log.e("File Path", "Path " + file.getPath());
//            Log.e("File Size", "Size " + file.length());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return file.getPath();
    }

    private static String getMediaFilePathForN(Uri uri, Context context) {
        Uri returnUri = uri;
        Cursor returnCursor = context.getContentResolver().query(returnUri, null, null, null, null);

        // Get the column indexes of the data in the Cursor,
        // move to the first row in the Cursor, get the data,
        // and display it.

        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        File file = new File(context.getFilesDir(), name);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            int maxBufferSize = 1 * 1024 * 1024;
            int bytesAvailable = inputStream.available();

            //int bufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);

            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
//            Log.e("File Size", "Size " + file.length());
            inputStream.close();
            outputStream.close();
            Log.e("File Path", "Path " + file.getPath());
//            Log.e("File Size", "Size " + file.length());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return file.getPath();
    }


    private static String getDataColumn(Context context, Uri uri,
                                        String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            Log.e(TAG, "Cursor exists: " + cursor.moveToFirst());
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @param uri - The Uri to check.
     * @return - Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri - The Uri to check.
     * @return - Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri - The Uri to check.
     * @return - Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri - The Uri to check.
     * @return - Whether the Uri authority is Google Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Drive.
     */
    private static boolean isGoogleDriveUri(Uri uri) {
        return "com.google.android.apps.docs.storage".equals(uri.getAuthority()) //
                || "com.google.android.apps.docs.storage.legacy".equals(uri.getAuthority());
    }
}