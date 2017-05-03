package game.prateek.pong2d.utils;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by Prateek Gupta on 4/28/17.
 */

public class SocketUtil {

    private static BluetoothSocket socket;
    private static ObjectInputStream input;
    private static ObjectOutputStream output;
    private static final String TAG = SocketUtil.class.getSimpleName();

    public static synchronized void setSocket(BluetoothSocket soc){
        socket = soc;
    }

    public static BluetoothSocket getSocket(){ return socket;}

    public static void closeSocket(){
        if(socket != null){
            try {
                input.close();
                output.close();
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public static ObjectInputStream getInput() {
        return input;
    }

    public static void setInput(ObjectInputStream in) {
        SocketUtil.input = in;
    }

    public static ObjectOutputStream getOutput() {
        return output;
    }

    public static void setOutput(ObjectOutputStream out) {
        SocketUtil.output = out;
    }
}
