package com.example.abood.youtubedownloader;

import android.annotation.SuppressLint;
import android.os.Message;
import android.util.Log;

import com.termux.terminal.ByteQueue;
import com.termux.terminal.JNI;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Handler;


public class ThreadUtil {


    private static final int MSG_NEW_INPUT = 1;
    private static final int MSG_PROCESS_EXITED = 4;


    @SuppressLint("HandlerLeak")
    public android.os.Handler mMainThreadHandler = new android.os.Handler() {
        final byte[] mReceiveBuffer = new byte[4 * 1024];

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_NEW_INPUT) {
                int bytesRead = mProcessToTerminalIOQueue.read(mReceiveBuffer, false);
                if (bytesRead > 0) {
                    Log.i("ThreadUtil", "byte is greater then zero ");
                    append(mReceiveBuffer,bytesRead);

                }
            } else if (msg.what == MSG_PROCESS_EXITED) {
                int exitCode = (Integer) msg.obj;

                String exitDescription = "\r\n[Process completed";
                if (exitCode > 0) {
                    // Non-zero process exit.
                    exitDescription += " (code " + exitCode + ")";
                } else if (exitCode < 0) {
                    // Negated signal.
                    exitDescription += " (signal " + (-exitCode) + ")";
                }
                exitDescription += " - press Enter]";

                byte[] bytesToWrite = exitDescription.getBytes(StandardCharsets.UTF_8);
            }
        }
    };


    private FileDescriptor terminalFileDescriptorWrapped;
    private int mShellPid;
    public final ByteQueue mProcessToTerminalIOQueue = new ByteQueue(4096);
    final ByteQueue mTerminalToProcessIOQueue = new ByteQueue(4096);


    public ThreadUtil(FileDescriptor terminalFileDescriptorWrapped, int mShellPid) {

        this.terminalFileDescriptorWrapped = terminalFileDescriptorWrapped;
        this.mShellPid = mShellPid;
        readThread();
        writeThread();
        waitThread();
    }


    private void writeThread() {
        new Thread("TermSessionOutputWriter[pid=" + mShellPid + "]") {
            @Override
            public void run() {

                final byte[] buffer = new byte[4096];
                try (FileOutputStream termOut = new FileOutputStream(terminalFileDescriptorWrapped)) {
                    while (true) {
                        int bytesToWrite = mTerminalToProcessIOQueue.read(buffer, true);
                        if (bytesToWrite == -1) return;
                        termOut.write(buffer, 0, bytesToWrite);
                    }
                } catch (IOException e) {
                    // Ignore.
                }
            }
        }.start();


    }


    public void readThread() {
        new Thread("TermSessionInputReader[pid=" + mShellPid + "]") {
            @Override
            public void run() {
                try (InputStream termIn = new FileInputStream(terminalFileDescriptorWrapped)) {
                    final byte[] buffer = new byte[4096];
                    while (true) {
                        int read = termIn.read(buffer);
                        if (read == -1) return;
                        if (!mProcessToTerminalIOQueue.write(buffer, 0, read)) return;
                        mMainThreadHandler.sendEmptyMessage(MSG_NEW_INPUT);
                    }
                } catch (Exception e) {
                    // Ignore, just shutting down.
                }
            }
        }.start();
    }


    private void waitThread() {
        new Thread("TermSessionWaiter[pid=" + mShellPid + "]") {
            @Override
            public void run() {
                int processExitCode = JNI.waitFor(mShellPid);
                mMainThreadHandler.sendMessage(mMainThreadHandler.obtainMessage(MSG_PROCESS_EXITED, processExitCode));
            }
        }.start();


    }


    public final void write(String data) {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        write(bytes, 0, bytes.length);
    }

    private void write(byte[] bytes, int i, int length) {
        mTerminalToProcessIOQueue.write(bytes,i,length);
    }

    public void append(byte[] buffer, int length) {
        for (int i = 0; i < length; i++){

        }

        Log.i("ThreadUtil",new String(buffer));

    }
}


