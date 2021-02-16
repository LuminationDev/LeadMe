package com.lumination.leadme;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.UpdateFrom;


public class AutoUpdater {
    /* Auto app installation through javiersantos library
     * TODO - to be removed for App Store version
     */

    private final static String TAG = "AutoUpdater";
    LeadMeMain main;
    AppUpdater appUpdater;

    public AutoUpdater(LeadMeMain main) {
        this.main = main;
        initialiseAutoUpdater();
    }

    protected void initialiseAutoUpdater() {
        appUpdater = new AppUpdater(main)
                .setDisplay(com.github.javiersantos.appupdater.enums.Display.DIALOG)
                .setUpdateFrom(UpdateFrom.GOOGLE_PLAY) //to stay in sync with Google Play Store
                .setUpdateFrom(UpdateFrom.XML) //for manual updates
                // points to an XML file hosted on a public github, the XML file then contains the download link to check for a new update
                //.setUpdateXML("https://raw.githubusercontent.com/jlundlumination/WebFiles/master/Update.XML")
                .setUpdateXML("https://raw.githubusercontent.com/LuminationDev/public/main/Update.XML")
                .setTitleOnUpdateAvailable("Update available")
                .setContentOnUpdateAvailable("For the best experience and improved functionality, please download the latest version of the LeadMe Learning app")
                .setContentOnUpdateNotAvailable("You've got the latest version of the LeadMe Learning app")
                .setButtonUpdate("Update now")
                .setButtonDismiss("Maybe later")
                .setButtonDoNotShowAgain("Don't ask again") //need to test if only stops showing for that update
                .setCancelable(true);
    }

    protected void startUpdateChecker() {
        appUpdater.start();
    }

    protected void stopUpdateChecker() {
        appUpdater.stop();
    }

    protected void showUpdateDialog() {
        appUpdater.showAppUpdated(true);
    }
}
