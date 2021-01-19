package kg.delletenebre.serialmanager2.communication.impl.old;


import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import kg.delletenebre.serialmanager2.App;

public class SerialPort {
    static {
        System.loadLibrary("serial-port");
    }

    private native FileDescriptor open(String path, int baudRate);

    private native void close();

    private final FileDescriptor mFileDescriptor;
    private final LocalBroadcastManager localBroadcastManager;
    private final String path;
    private final int baudRate;

    private CommunicationThread mCommunicationThread;

    public SerialPort(String path, int baudRate, Context context) {
        this.path = path;
        this.baudRate = baudRate;

        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        mFileDescriptor = open(this.path, this.baudRate);
        if (mFileDescriptor != null) {
            App.log("Serial Connection - Connected to serial: " + this.path + " baudRate: " + this.baudRate);
            localBroadcastManager.sendBroadcast(
                    new Intent(App.LOCAL_ACTION_CONNECTION_ESTABLISHED)
                            .putExtra("type", "serial")
                            .putExtra("name", this.path));

            if (App.getInstance().getBooleanPreference("send_connection_state")) {
                write((App.ACTION_CONNECTION_ESTABLISHED + "\n").getBytes());
            }

            mCommunicationThread = new CommunicationThread();
            mCommunicationThread.start();
        } else {
            localBroadcastManager.sendBroadcast(
                    new Intent(App.LOCAL_ACTION_CONNECTION_FAILED)
                            .putExtra("type", "serial")
                            .putExtra("name", path));
            App.logError("Serial port open fail-File descriptor null. Path: " + path + " baudRate: " + baudRate);
        }
    }

    public void write(byte[] out) {
        if (mCommunicationThread != null) {
            mCommunicationThread.write(out);
        }
    }

    public void closePort() {
        if (mCommunicationThread != null) {
            mCommunicationThread.cancel();
            close();
            localBroadcastManager.sendBroadcast(
                    new Intent(App.LOCAL_ACTION_CONNECTION_CLOSED)
                            .putExtra("type", "serial")
                            .putExtra("name", path + " (" + baudRate + ")"));
        }
    }

    public boolean isConnected() {
        return mFileDescriptor != null;
    }

    private class CommunicationThread extends Thread {
        private InputStream mInputStream;
        private OutputStream mOutputStream;

        CommunicationThread() {
            mInputStream = new FileInputStream(mFileDescriptor);
            mOutputStream = new FileOutputStream(mFileDescriptor);
        }

        public void run() {
            StringBuilder sb = new StringBuilder();
            while (!isInterrupted()) {
                try {
                    if (mInputStream == null) {
                        continue;
                    }

//                    int sizeOfData = mInputStream.available();
//                    if (sizeOfData > 0) {
//                        byte[] data = new byte[sizeOfData];
//                        mInputStream.read(data);
//                        String strData = new String(data, StandardCharsets.UTF_8);
//                        localBroadcastManager.sendBroadcast(
//                                new Intent(App.LOCAL_ACTION_COMMAND_RECEIVED)
//                                        .putExtra("from", "serial")
//                                        .putExtra("command", strData));
//                        Thread.sleep(20);
//                    }

                    int data = mInputStream.read();
                    if (data > -1) {
                        if (data == App.CR || data == App.LF) {
                            if (sb.length() > 0) {
                                localBroadcastManager.sendBroadcast(
                                        new Intent(App.LOCAL_ACTION_COMMAND_RECEIVED)
                                                .putExtra("from", "serial")
                                                .putExtra("command", sb.toString()));
                                Thread.sleep(20);
                                sb = new StringBuilder();
                            }
                        } else {
                            sb.append((char) data);
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    break;
                }
            }

            cancel();

            synchronized (SerialPort.this) {
                mCommunicationThread = null;
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        void write(byte[] buffer) {
            App.log("SerialPort: trying to write: " + new String(buffer, Charset.forName("UTF-8")));
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

            interrupt();
        }
    }
}
