package de.rhuber.homedash;

import android.content.Intent;
import android.app.Application;

public class HomeDash extends Application {

    @SuppressWarnings("FieldCanBeLocal")
    private Intent homeDashService;

    @Override
    public void onCreate() {
        super.onCreate();
        homeDashService = new Intent(this, HomeDashService.class);
        startService(homeDashService);
    }
}
