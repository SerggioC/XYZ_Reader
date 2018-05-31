package com.example.xyzreader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.xyzreader.data.UpdaterService;

public class DataBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
            boolean isRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
            broadcastReady.onReady(isRefreshing);
        }
    }

    public DataBroadcastReceiver(BroadcastReady broadcastReady) {
        this.broadcastReady = broadcastReady;
    }

    private BroadcastReady broadcastReady;

    public interface BroadcastReady {
        void onReady(boolean isRefreshing);
    }

}
