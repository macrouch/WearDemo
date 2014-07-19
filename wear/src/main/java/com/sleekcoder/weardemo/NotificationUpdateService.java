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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.MessageEvent;
import com.sleekcoder.weardemo.common.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.List;

public class NotificationUpdateService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<DataApi.DeleteDataItemsResult>, DataApi.DataListener {

    private static final String TAG = "NotificationUpdate";
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent) {
            String action = intent.getAction();
            if (Constants.ACTION_DISMISS.equals(action)) {
                int notificationId = intent.getIntExtra(Constants.KEY_NOTIFICATION_ID, -1);
                if (notificationId == Constants.BOTH_ID) {
                    dismissPhoneNotification(notificationId);
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void dismissPhoneNotification(int id) {
        final Uri dataItemUri =
                new Uri.Builder().scheme(WEAR_URI_SCHEME).path(Constants.BOTH_PATH).build();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Deleting Uri: " + dataItemUri.toString());
        }
        Wearable.DataApi.deleteDataItems(
                mGoogleApiClient, dataItemUri).setResultCallback(this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();

        for (DataEvent dataEvent : events) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED && dataEvent.getDataItem().getUri().getPath().equals(Constants.IMAGE_PATH)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataEvent.getDataItem());
                Asset image = dataMapItem.getDataMap().getAsset("image");
                Bitmap bitmap = loadBitMapFromAsset(image);
                buildWearableOnlyNotification("Image", "You took a photo!", Constants.BOTH_ID, false, bitmap);
            } else if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String content = dataMap.getString(Constants.KEY_CONTENT);
                String title = dataMap.getString(Constants.KEY_TITLE);
                if (Constants.WATCH_ONLY_PATH.equals(dataEvent.getDataItem().getUri().getPath())) {
                    buildWearableOnlyNotification(title, content, Constants.WATCH_ONLY_ID, false, null);
                } else if (Constants.BOTH_PATH.equals(dataEvent.getDataItem().getUri().getPath())) {
                    buildWearableOnlyNotification(title, content, Constants.BOTH_ID, true, null);
                }
            } else if (dataEvent.getType() == DataEvent.TYPE_DELETED) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "DataItem deleted: " + dataEvent.getDataItem().getUri().getPath());
                }
                if (Constants.BOTH_PATH.equals(dataEvent.getDataItem().getUri().getPath())) {
                    ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                            .cancel(Constants.BOTH_ID);
                }
            }
        }
    }

    public Bitmap loadBitMapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(mGoogleApiClient, asset)
                .await().getInputStream();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset");
        }

        return BitmapFactory.decodeStream(assetInputStream);
    }

    private void buildWearableOnlyNotification(String title, String content, int notificationId,
                                               boolean withDismissal, Bitmap bitmap) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setLargeIcon(bitmap);

        if (withDismissal) {
            Intent dismissIntent = new Intent(Constants.ACTION_DISMISS);
            dismissIntent.putExtra(Constants.KEY_NOTIFICATION_ID, Constants.BOTH_ID);
            PendingIntent pendingIntent = PendingIntent
                    .getService(this, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setDeleteIntent(pendingIntent);
        }

        NotificationCompat.BigTextStyle secondPageStyle = new NotificationCompat.BigTextStyle();
        secondPageStyle.setBigContentTitle("Page 2")
                .bigText("This is the second page of the notification");

        Notification secondPageNotification = new NotificationCompat.Builder(this)
                .setStyle(secondPageStyle)
                .build();

        Notification twoPageNotification = new NotificationCompat.WearableExtender()
                .addPage(secondPageNotification)
                .extend(builder)
                .build();

        NotificationManagerCompat.from(this).notify(notificationId, twoPageNotification);
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onResult(DataApi.DeleteDataItemsResult deleteDataItemsResult) {
        if (!deleteDataItemsResult.getStatus().isSuccess()) {
            Log.e(TAG, "dismissWearableNotification(): failed to delete DataItem");
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(Constants.START_WEAR_ACTIVITY_PATH)) {
            Intent startIntent = new Intent(this, WearActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
    }

}