package game.prateek.pong2d.service;

import android.app.IntentService;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;

import game.prateek.pong2d.MultiplayerActivity;

/**
 * Created by Home on 4/28/17.
 */

public class ConnectService extends Thread {

    private final String TAG = ConnectService.class.getSimpleName();
    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
    private OnConnectionOverBluetoothListener mListener;

    public ConnectService(String name, BluetoothDevice device, OnConnectionOverBluetoothListener listener) {
        super(name);
        mDevice = device;
        try {
            mSocket = device.createRfcommSocketToServiceRecord(MultiplayerActivity.APP_UUID);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        mListener = listener;
    }

    public void run() {

        try {
            mSocket.connect();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            try {
                mSocket.close();
            } catch (IOException e1) {
                Log.e(TAG, e.getMessage());
            }
            mListener.connectionFailed();
        }
        mListener.connected(false,mSocket);
    }

    public void cancel() {
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
}
