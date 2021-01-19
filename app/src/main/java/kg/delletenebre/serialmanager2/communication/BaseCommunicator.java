package kg.delletenebre.serialmanager2.communication;

import androidx.annotation.NonNull;

public interface BaseCommunicator {
    void open();
    void close();
    void write(@NonNull String data);
    boolean isConnected();
    Integer getConnectionsCount();

    @NonNull
    CommunicatorType getCommunicatorType();

    default void openOrClose(boolean needOpen) {
        if((isConnected() && needOpen)
                || (!isConnected() && !needOpen)) {
            return;
        }

        if(needOpen) {
            open();
        } else {
            close();
        }
    }
}
