package kg.delletenebre.serialmanager2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;


class BluetoothConnection {

    // Current connection state
    private enum BluetoothState {
        NONE,       // we're doing nothing
        LISTEN,     // now listening for incoming connections
        CONNECTING, // now initiating an outgoing connection
        CONNECTED   // now connected to a remote device
    }

    private BluetoothAdapter mBluetoothAdapter;
    private UUID CONNECTION_UUID;
    private BluetoothState mState;

    private CommunicationThread mCommunicationThread;


    // Listener for Bluetooth Status & Connection
    private OnDataReceivedListener mDataReceivedListener = null;
    private ConnectionListener mConnectionListener = null;


    BluetoothConnection() {
        CONNECTION_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = BluetoothState.NONE;
    }

    /**
     * Set the current state of the connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(BluetoothState state) {
        App.log("BluetoothService: setState() " + mState + " -> " + state);

        mState = state;
    }

    /**
     * Return the current connection state.
     */
    public synchronized BluetoothState getState() {
        return mState;
    }
    boolean isConnected() {
        return mState == BluetoothState.CONNECTED;
    }


    private synchronized void autoConnectTo(BluetoothDevice device) {
        App.log("BluetoothService: auto connecting to: " + device);

        // Cancel any thread attempting to make a connection
        if (mCommunicationThread != null) {
            mCommunicationThread.cancel();
            mCommunicationThread = null;
        }

        if (isEnabled()) {
            // Start the thread to connect with the given device
            mCommunicationThread = new CommunicationThread(device);
            mCommunicationThread.start();
        }
    }
    synchronized void autoConnectTo(String deviceAddress) {
        if (isEnabled() && BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);

            if (device != null) {
                autoConnectTo(device);
            }
        }
    }


    /**
     * Stop all threads
     */
    synchronized void stop() {
        if (mCommunicationThread != null) {
            mCommunicationThread.cancel();
            mCommunicationThread = null;
        }

        setState(BluetoothState.NONE);

        setConnectionListener(null);
        setOnDataReceivedListener(null);

        mBluetoothAdapter = null;
        mState = null;
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see CommunicationThread#write(byte[])
     */
    private void write(byte[] out) {
        // Create temporary object
        CommunicationThread r;

        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != BluetoothState.CONNECTED) {
                return;
            }
            r = mCommunicationThread;
        }

        // Perform the write unsynchronized
        r.write(out);
    }


    private class CommunicationThread extends Thread {
        private BluetoothSocket mBluetoothSocket;
        private BluetoothDevice mBluetoothDevice;
        private InputStream mInputStream;
        private OutputStream mOutputStream;

        CommunicationThread(BluetoothDevice device) {
            mBluetoothDevice = device;

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
//            try {
//                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(CONNECTION_UUID);
//            } catch (IOException e) {
//                App.logStatus("createRfcommSocketToServiceRecord",  "FAIL");
//            }
        }

        public void run() {
            setState(BluetoothState.CONNECTING);

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            boolean connected = false;
            while (!connected) {
                if (isInterrupted()) {
                    break;
                }
                // Make a connection to the BluetoothSocket
                try {
                    // Get a BluetoothSocket for a connection with the given BluetoothDevice
                    mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(CONNECTION_UUID);
                    mBluetoothSocket.connect();
                    connected = mBluetoothSocket.isConnected();
                } catch (Exception e) {
                    try {
                        if (mBluetoothSocket != null) {
                            mBluetoothSocket.close();
                        }
                        synchronized (this) {
                            sleep(100);
                        }
                    } catch (Exception e1) {
                        // nothing
                    }
                }
            }

            if (connected) {
                try {
                    mInputStream = mBluetoothSocket.getInputStream();
                    mOutputStream = mBluetoothSocket.getOutputStream();
                } catch (IOException e) {
                    App.logError("temp sockets not created");
                    cancel();
                    return;
                }

                setState(BluetoothState.CONNECTED);

                if (mConnectionListener != null) {
                    mConnectionListener.onDeviceConnected(mBluetoothSocket.getRemoteDevice().getName(),
                            mBluetoothSocket.getRemoteDevice().getAddress());
                }

                StringBuilder sb = new StringBuilder();

                // Keep listening to the InputStream while connected
                while (!isInterrupted() && mState == BluetoothState.CONNECTED) {
                    try {
                        int data = mInputStream.read();
                        if (data > -1) {
                            if (data == 0x0D || data == 0x0A) {
                                if (sb.length() > 0) {
                                    if (mDataReceivedListener != null) {
                                        mDataReceivedListener.onDataReceived(sb.toString());
                                    }

                                    sb = new StringBuilder();
                                }
                            } else {
                                sb.append((char) data);
                            }
                        }
                    } catch (IOException e) {
                        setState(BluetoothState.NONE);
                        if (mConnectionListener != null) {
                            mConnectionListener.onDeviceConnectionFailed();
                        }
                        break;
                    }
                }

                cancel();
            }

            synchronized (BluetoothConnection.this) {
                mCommunicationThread = null;
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        void write(byte[] buffer) {
            App.log("BluetoothService: trying to write: " + new String(buffer, Charset.forName("UTF-8")));
            try {
                if (mOutputStream != null) {
                    mOutputStream.write(buffer);
                }
            } catch (IOException e) {
                App.logError("BluetoothService: Exception during write: " + e.getLocalizedMessage());
            }
        }

        void cancel() {
            try {
                if (mInputStream != null) {
                    mInputStream.close();
                    mInputStream = null;
                }
                if (mOutputStream != null) {
                    mOutputStream.close();
                    mOutputStream = null;
                }
            } catch (IOException e) {
                App.logStatus("inputStream.close() or outputStream.close()", "FAIL");
            }

            try {
                if (mBluetoothSocket != null) {
                    mBluetoothSocket.close();
                    mBluetoothSocket = null;
                }
            } catch (IOException e) {
                App.logStatus("BluetoothService: Connect socket close", "FAIL");
            }

            setState(BluetoothState.NONE);
            mBluetoothDevice = null;
            interrupt();
        }
    }




    interface OnDataReceivedListener {
        void onDataReceived(String message);
    }

    interface ConnectionListener {
        void onDeviceConnected(String name, String address);
        void onDeviceConnectionFailed();
    }


    void setOnDataReceivedListener (OnDataReceivedListener listener) {
        mDataReceivedListener = listener;
    }

    void setConnectionListener (ConnectionListener listener) {
        mConnectionListener = listener;
    }



    public boolean isEnabled() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }
    BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public void write(String data, boolean CRLF) {
        if (mState == BluetoothState.CONNECTED) {
            if (CRLF) {
                data += "\r\n";
            }

            write(data.getBytes());
        }
    }

}
