package kg.delletenebre.serialmanager2;


import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

class SerialPort {
    static {
        System.loadLibrary("serial-port");
    }
    private native FileDescriptor open(String path, int baudRate);
    private native void close();

    private FileDescriptor mFileDescriptor;
    private CommunicationThread mCommunicationThread;
    private OnDataReceivedListener mDataReceivedListener = null;

    SerialPort(String path, int baudRate) {
        mFileDescriptor = open(path, baudRate);

        if (mFileDescriptor == null) {
            App.logError("Serial port: native open() returns null");
        } else {
            mCommunicationThread = new CommunicationThread();
            mCommunicationThread.start();
        }
    }

    public void write(byte[] out) {
        if (mCommunicationThread != null) {
            mCommunicationThread.write(out);
        }
    }

    void closePort() {
        if (mCommunicationThread != null) {
            mCommunicationThread.cancel();
            close();
        }
    }

    private class CommunicationThread extends Thread {
        private InputStream mInputStream;
        private OutputStream mOutputStream;

        CommunicationThread() {
            mInputStream = new BufferedInputStream(new FileInputStream(mFileDescriptor));
            mOutputStream = new FileOutputStream(mFileDescriptor);
        }

        public void run() {

            StringBuilder sb = new StringBuilder();

            while (!isInterrupted()) {
                try {
                    int data = mInputStream.read();
                    if (data > -1) {
                        if (data == App.CR || data == App.LF) {
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

    interface OnDataReceivedListener {
        void onDataReceived(String message);
    }

    void setOnDataReceivedListener (OnDataReceivedListener listener) {
        mDataReceivedListener = listener;
    }
}
