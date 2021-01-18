package kg.delletenebre.serialmanager2.communication;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import kg.delletenebre.serialmanager2.App;

public abstract class BaseCommunicatorImpl implements BaseCommunicator {
    private final CommunicatorType type;
    protected final Context context;
    protected final LocalBroadcastManager localBroadcastManager;

    public BaseCommunicatorImpl(Context context, CommunicatorType type) {
        this.context = context;
        this.type = type;
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    protected boolean isConnectionStateMessageUse() {
        return App.getInstance().getBooleanPreference("send_connection_state");
    }

    @NonNull
    public CommunicatorType getCommunicatorType() {
        return type;
    }
}
