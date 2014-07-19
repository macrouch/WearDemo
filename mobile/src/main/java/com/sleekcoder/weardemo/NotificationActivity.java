/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sleekcoder.weardemo;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.View;

import com.sleekcoder.weardemo.common.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.DateFormat;
import java.util.Date;


public class NotificationActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "NotificationActivity";
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private void buildLocalOnlyNotification(String title, String content, int notificationId,
                                            boolean withDismissal) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(title)
                .setContentText(content)
                .setLocalOnly(true)
                .setSmallIcon(R.drawable.ic_launcher);

        if (withDismissal) {
            Intent dismissIntent = new Intent(Constants.ACTION_DISMISS);
            dismissIntent.putExtra(Constants.KEY_NOTIFICATION_ID, Constants.BOTH_ID);
            PendingIntent pendingIntent = PendingIntent
                    .getService(this, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setDeleteIntent(pendingIntent);
        }
        NotificationManagerCompat.from(this).notify(notificationId, builder.build());
    }

    private void buildWearableOnlyNotification(String title, String content, String path) {
        if (mGoogleApiClient.isConnected()) {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
            putDataMapRequest.getDataMap().putString(Constants.KEY_CONTENT, content);
            putDataMapRequest.getDataMap().putString(Constants.KEY_TITLE, title);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "buildWatchOnlyNotification(): Failed to set the data, "
                                        + "status: " + dataItemResult.getStatus().getStatusCode());
                            }
                        }
                    });
        } else {
            Log.e(TAG, "buildWearableOnlyNotification(): no Google API Client connection");
        }
    }

    private void buildMirroredNotifications(String phoneTitle, String watchTitle, String content) {
        if (mGoogleApiClient.isConnected()) {
            buildWearableOnlyNotification(watchTitle, content, Constants.BOTH_PATH);
            buildLocalOnlyNotification(phoneTitle, content, Constants.BOTH_ID, true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Failed to connect to Google API Client");
    }

    private String now() {
        DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(this);
        return dateFormat.format(new Date());
    }

    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.phone_only:
                buildLocalOnlyNotification(getString(R.string.phone_only), now(),
                        Constants.PHONE_ONLY_ID, false);
                break;
            case R.id.wear_only:
                buildWearableOnlyNotification(getString(R.string.wear_only), now(),
                        Constants.WATCH_ONLY_PATH);
                break;
            case R.id.different_notifications:
                buildMirroredNotifications(getString(R.string.phone_both), getString(R.string.watch_both), now());
                break;
        }
    }
}
