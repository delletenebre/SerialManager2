package kg.delletenebre.serialmanager2.communication.impl;

import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import kg.delletenebre.serialmanager2.App;
import kg.delletenebre.serialmanager2.communication.BaseCommunicatorImpl;
import kg.delletenebre.serialmanager2.communication.CommunicatorType;

public class SerialCommunicator extends BaseCommunicatorImpl {

    private FileDescriptor fileDescriptor;
    private CommunicationThread mCommunicationThread;

    public SerialCommunicator(Context context) {
        super(context, CommunicatorType.SERIAL);
    }

    @Override
    public void open() {
        if (mCommunicationThread != null) {
            close();
        }
        Pair<String, Integer> settings = getPathAndBaudRate();
        fileDescriptor = openSerial(settings.first, settings.second);
        if (fileDescriptor != null) {
            App.log("Serial Connection-Connected to serial: " + settings.first + " baudRate: " + settings.second);
            localBroadcastManager.sendBroadcast(
                    new Intent(App.LOCAL_ACTION_CONNECTION_ESTABLISHED)
                            .putExtra("type", "serial")
                            .putExtra("name", settings.first));

            if (isConnectionStateMessageUse()) {
                write(App.ACTION_CONNECTION_ESTABLISHED);
            }
            mCommunicationThread = new CommunicationThread();
            mCommunicationThread.start();
        } else {
            localBroadcastManager.sendBroadcast(
                    new Intent(App.LOCAL_ACTION_CONNECTION_FAILED)
                            .putExtra("type", "serial")
                            .putExtra("name", settings.first));
            App.logError("Serial port open fail-File descriptor null. Path: " + settings.first + " baudRate: " + settings.second);
        }
    }

    @Override
    public void close() {
        if (isConnectionStateMessageUse()) {
            write(App.ACTION_CONNECTION_LOST);
        }

        if (mCommunicationThread != null) {
            mCommunicationThread.cancel();
            localBroadcastManager.sendBroadcast(
                    new Intent(App.LOCAL_ACTION_CONNECTION_CLOSED)
                            .putExtra("type", "serial")
                            .putExtra("name", "dev/tty*"));
        }
        if(fileDescriptor != null) {
            closeSerial();
            fileDescriptor = null;
        }
    }

    @Override
    public void write(@NonNull String data) {
        if (mCommunicationThread != null) {
            mCommunicationThread.write(data.getBytes());
        }
    }

    @Override
    public boolean isConnected() {
        return fileDescriptor != null;
    }

    @Override
    public Integer getConnectionsCount() {
        return isConnected() ? 1 : 0;
    }

    private class CommunicationThread extends Thread {
        private InputStream mInputStream;
        private OutputStream mOutputStream;

        CommunicationThread() {
            mInputStream = new FileInputStream(fileDescriptor);
            mOutputStream = new FileOutputStream(fileDescriptor);
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
//                        Thread.sleep(1);
//                    }

                    int data = mInputStream.read();
                    if (data > -1) {
                        if (data == App.CR || data == App.LF) {
                            if (sb.length() > 0) {
                                localBroadcastManager.sendBroadcast(
                                        new Intent(App.LOCAL_ACTION_COMMAND_RECEIVED)
                                                .putExtra("from", "serial")
                                                .putExtra("command", sb.toString()));
                                Thread.sleep(10);
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

            synchronized (SerialCommunicator.this) {
                mCommunicationThread = null;
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        void write(byte[] buffer) {
            App.log("SerialPort: trying to write: " + new String(buffer, StandardCharsets.UTF_8));
            try {
                if (mOutputStream != null) {
                    mOutputStream.write(buffer);
                }
            } catch (IOException e) {
                App.logError("SerialPort: Exception during write: " + e.getLocalizedMessage());
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

    private Pair<String, Integer> getPathAndBaudRate() {
        return Pair.create(
                App.getInstance().getStringPreference("serial_path"),
                App.getInstance().getIntPreference("serial_baud_rate"));
    }

    static {
        System.loadLibrary("serial-port");
    }

    private native FileDescriptor openSerial(String path, int baudRate);

    private native void closeSerial();
}
