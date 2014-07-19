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

import static com.google.android.gms.wearable.PutDataRequest.WEAR_URI_SCHEME;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.sleekcoder.weardemo.common.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;


public class DismissListener extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<DataApi.DeleteDataItemsResult> {

    private static final String TAG = "DismissListener";
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_DELETED) {
                if (Constants.BOTH_PATH.equals(dataEvent.getDataItem().getUri().getPath())) {
                    NotificationManagerCompat.from(this).cancel(Constants.BOTH_ID);
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent) {
            String action = intent.getAction();
            if (Constants.ACTION_DISMISS.equals(action)) {
                int notificationId = intent.getIntExtra(Constants.KEY_NOTIFICATION_ID, -1);
                if (notificationId == Constants.BOTH_ID) {
                    dismissWearableNotification(notificationId);
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }


    private void dismissWearableNotification(final int id) {
        mGoogleApiClient.connect();
        final Uri dataItemUri =
                new Uri.Builder().scheme(WEAR_URI_SCHEME).path(Constants.BOTH_PATH).build();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Deleting Uri: " + dataItemUri.toString());
        }
        Wearable.DataApi.deleteDataItems(
                mGoogleApiClient, dataItemUri).setResultCallback(this);
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Failed to connect to the Google API client");
    }

    @Override
    public void onResult(DataApi.DeleteDataItemsResult deleteDataItemsResult) {
        if (!deleteDataItemsResult.getStatus().isSuccess()) {
            Log.e(TAG, "dismissWearableNotification(): failed to delete DataItem");
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(Constants.START_PHONE_ACTIVITY_PATH)) {
            Intent startIntent = new Intent(this, PhoneActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
    }
}
