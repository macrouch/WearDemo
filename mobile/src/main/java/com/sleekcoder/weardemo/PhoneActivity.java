package com.sleekcoder.weardemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.sleekcoder.weardemo.R;
import com.sleekcoder.weardemo.common.Constants;

import java.io.ByteArrayOutputStream;

public class PhoneActivity extends Activity {

    private static final String TAG = "PhoneActivity";
    GoogleApiClient mGoogleApiClient;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.notificationButton:
                Intent i = new Intent(getApplicationContext(), NotificationActivity.class);
                startActivity(i);
                break;
            case R.id.messageButton:
                openWearActivity();
                break;
            case R.id.dataItemButton:
                dispatchTakePictureIntent();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        mGoogleApiClient = new GoogleApiClient.Builder(this.getApplicationContext())
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    public void openWearActivity() {
        if(mGoogleApiClient.isConnected()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                    for(Node node : nodes.getNodes()) {
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                                mGoogleApiClient, node.getId(), Constants.START_WEAR_ACTIVITY_PATH, null
                        ).await();
                        if(!result.getStatus().isSuccess()){
                            Log.e(TAG, "error");
                        } else {
                            Log.i(TAG, "success!! sent to: " + node.getDisplayName());
                        }
                    }
                }
            }).start();

        } else {
            Log.e(TAG, "not connected");
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            Asset asset = createAssetFromBitmap(imageBitmap);
            PutDataMapRequest dataMap = PutDataMapRequest.create(Constants.IMAGE_PATH);
            dataMap.getDataMap().putAsset("image", asset);
            PutDataRequest request = dataMap.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request);
        }
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }
}
