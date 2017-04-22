package de.rhuber.homedash;

import android.content.Context;
import android.content.Intent;
import android.app.Application;

public class WallPanel extends Application {

    private static Application sApplication;

    @SuppressWarnings("FieldCanBeLocal")
    private Intent wallPanelService;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
        wallPanelService = new Intent(this, WallPanelService.class);
        startService(wallPanelService);
    }

    public static Application getApplication() {
        return sApplication;
    }

    public static Context getContext() {
        return getApplication().getApplicationContext();
    }
}
