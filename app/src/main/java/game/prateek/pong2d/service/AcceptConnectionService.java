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
 * A service that creates a server socket and listens to incoming bluetooth connection requests.
 */

public class AcceptConnectionService extends Thread {

    private String TAG = AcceptConnectionService.class.getSimpleName();
    private OnConnectionOverBluetoothListener mListener;
    private BluetoothServerSocket mServerSocket;
    private boolean listening = true;

    public AcceptConnectionService(String name, OnConnectionOverBluetoothListener listener) {
        setName(name);
        mListener = listener;
        try {
            //Gets the server socket of the device's bluetooth port
            mServerSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("Pong2d", MultiplayerActivity.APP_UUID);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }


    public void run() {

        BluetoothSocket socket = null;

        while(listening) {
            try {
                //Listening to incoming connections
                socket = mServerSocket.accept();
                if (socket != null) {
                    //If connection is accepted, then call a callback to ConnectionOverBluetooth listener.
                    mListener.connected(true, socket);
                } else {
                    mListener.connectionFailed();
                    //break;
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public void cancel() {
        if (mServerSocket != null) {
            try {
                listening = false;
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
}
