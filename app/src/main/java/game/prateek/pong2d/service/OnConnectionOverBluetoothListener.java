package game.prateek.pong2d.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * Created by Home on 4/28/17.
 */

public interface OnConnectionOverBluetoothListener {

    public void connected(boolean hostingGame, BluetoothSocket socket);

    public void connectionFailed();

}
