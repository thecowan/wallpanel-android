/*
 * Copyright (c) 2019 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.thanksmister.iot.wallpanel.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.thanksmister.iot.wallpanel.R;
import com.thanksmister.iot.wallpanel.ui.activities.WelcomeActivity;

public class NotificationUtils extends ContextWrapper {
    
    private static int NOTIFICATION_ID = 1138;
    public static final String ANDROID_CHANNEL_ID = "com.thanksmister.iot.wallpanel.ANDROID";
    public static String ANDROID_CHANNEL_NAME;
    private NotificationManager notificationManager;
    private final PendingIntent pendingIntent;
    private final Intent notificationIntent;
    private final Resources resources;

    public NotificationUtils(Context context, Resources resources) {
        super(context);
        this.resources = resources;
        ANDROID_CHANNEL_NAME = getString(R.string.text_android_channel_name);
        notificationIntent = new Intent(context, WelcomeActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
        pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels();
        }
    }
    
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannels() {
        String description = getString(R.string.text_android_channel_description);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(ANDROID_CHANNEL_ID, ANDROID_CHANNEL_NAME, importance);
        mChannel.setDescription(description);
        getManager().createNotificationChannel(mChannel);
    }

    private NotificationManager getManager() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

    public Notification createNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder nb = getAndroidChannelNotification(title, message);
            return nb.build();
            //getManager().notify(NOTIFICATION_ID, nb.build());
        } else {
            NotificationCompat.Builder nb = getAndroidNotification(title, message);
            // This ensures that navigating backward from the Activity leads out of your app to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
            stackBuilder.addParentStack(WelcomeActivity.class);
            stackBuilder.addNextIntent(notificationIntent);
            nb.setContentIntent(pendingIntent);
            return nb.build();
            //getManager().notify(NOTIFICATION_ID, nb.build());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getAndroidChannelNotification(String title, String body) {
        final int color = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary);
        Notification.Builder builder =  new Notification.Builder(getApplicationContext(), ANDROID_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setColor(color)
                .setOngoing(true)
                .setLocalOnly(true)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                .setSmallIcon(R.drawable.ic_dashboard_white)
                .setAutoCancel(false);
        
        builder.setContentIntent(pendingIntent);
        return builder;
    }
    
    public NotificationCompat.Builder getAndroidNotification(String title, String body) {
        final int color = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary);
        return new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_dashboard_white)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                .setContentTitle(title)
                .setContentText(body)
                .setOngoing(false)
                .setLocalOnly(true)
                .setColor(color)
                .setAutoCancel(false);
    }

    public void clearNotification() {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }
}