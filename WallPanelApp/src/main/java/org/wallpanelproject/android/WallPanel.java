package org.wallpanelproject.android;

import android.content.Context;
import android.content.Intent;
import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(
        excludeMatchingSharedPreferencesKeys = {".*password"},
        excludeMatchingSettingsKeys = {".*password"},
        formUri = "https://collector.tracepot.com/0cf0b155",
        mode = ReportingInteractionMode.DIALOG,
        resToastText = R.string.crash_toast_text, // optional, displayed as soon as the crash occurs, before collecting data which can take a few seconds
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info, //optional. default is a warning sign
        resDialogTitle = R.string.crash_dialog_title, // optional. default is your application name
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. When defined, adds a user text field input with this text resource as a label
        resDialogOkToast = R.string.crash_dialog_ok_toast, // optional. displays a Toast message when the user accepts to send a report.
        resDialogTheme = R.style.Theme_AppCompat_Dialog //optional. default is Theme.Dialog
)
public class WallPanel extends Application {

    @SuppressWarnings("FieldCanBeLocal")
    private Intent wallPanelService;

    @Override
    public void onCreate() {
        super.onCreate();
        wallPanelService = new Intent(this, WallPanelService.class);
        startService(wallPanelService);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ACRA.init(this);
    }
}
