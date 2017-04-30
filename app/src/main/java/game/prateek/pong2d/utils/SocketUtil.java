package game.prateek.pong2d.utils;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

/**
 * Created by Prateek Gupta on 4/28/17.
 */

public class SocketUtil {

    private static BluetoothSocket socket;
    private static final String TAG = SocketUtil.class.getSimpleName();

    public static synchronized void setSocket(BluetoothSocket soc){
        socket = soc;
    }

    public static BluetoothSocket getSocket(){ return socket;}

    public static void closeSocket(){
        if(socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
}
