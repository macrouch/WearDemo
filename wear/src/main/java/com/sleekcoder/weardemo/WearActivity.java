package com.sleekcoder.weardemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.sleekcoder.weardemo.common.Constants;

public class WearActivity extends Activity {

    private static final String TAG = "WearActivity";
    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear);

        mGoogleApiClient = new GoogleApiClient.Builder(this.getApplicationContext())
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    public void openApp(View view) {
        if(mGoogleApiClient.isConnected()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                    for(Node node : nodes.getNodes()) {
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                                mGoogleApiClient, node.getId(), Constants.START_PHONE_ACTIVITY_PATH, null
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
}
