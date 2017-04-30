package game.prateek.pong2d.service;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;

import game.prateek.pong2d.MultiplayerActivity;

/**
 * Created by Prateek Gupta on 4/28/17.
 */

public class AcceptConnectionService extends Thread {

    private String TAG = AcceptConnectionService.class.getSimpleName();
    private OnConnectionOverBluetoothListener mListener;
    private BluetoothServerSocket mServerSocket;

    public AcceptConnectionService(String name, OnConnectionOverBluetoothListener listener) {
        setName(name);
        mListener = listener;
        try {
            mServerSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("Pong2d", MultiplayerActivity.APP_UUID);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }


    public void run() {

        BluetoothSocket socket = null;

        while(true) {
            try {
                socket = mServerSocket.accept();
                if (socket != null) {
                    mListener.connected(true, socket);
                } else {
                    mListener.connectionFailed();
                    break;
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public void cancel() {
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
}
