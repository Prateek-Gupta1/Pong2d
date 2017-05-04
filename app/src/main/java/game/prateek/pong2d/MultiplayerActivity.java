/**
 * The activity presents users with the choices of Hosting a Game or connecting to one.
 * In either case users are asked to turn ON the Bluetooth first, failing to do so exits the activity.
 * The Host device first makes the device discoverable and starts a service to accept any incoming connection.
 * The client device scans for nearby devices and the user is presented with  available devices. When the user selects
 * a device, an attempt is made to establish the connection. Once the connection is set, pong activity starts.
 * On host side, once it accepts an incoming connection it starts the pong activity.
 */
package game.prateek.pong2d;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.UUID;

import game.prateek.pong2d.game.GameThread;
import game.prateek.pong2d.service.AcceptConnectionService;
import game.prateek.pong2d.service.ConnectService;
import game.prateek.pong2d.service.OnConnectionOverBluetoothListener;
import game.prateek.pong2d.utils.SocketUtil;
import game.prateek.pong2d.view.PongTable;

public class MultiplayerActivity extends AppCompatActivity implements OnConnectionOverBluetoothListener {

    BluetoothAdapter mBluetoothAdapter;
    ProgressDialog mDialog;
    AcceptConnectionService mAcceptConnService;
    ConnectService mConnectService;

    private static final int REQUEST_ENABLE_BT = 0;
    private static final int ENSURE_ENABLE_DISCOVERABLE = 1;
    //Unique bluetooth ID
    public static final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //If bluetooth is not supported by the device, then exit the activity
        if(mBluetoothAdapter == null){
            Toast.makeText(this, "Bluetooth not supported in your device", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        RadioGroup rgMultiplayer = (RadioGroup) findViewById(R.id.rgMultiplayer);
            rgMultiplayer.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                    switch (checkedId) {
                        //If user wants to be client.
                        case R.id.rbConnect:
                            //If device is already scanning, then cancel it.
                            if(mBluetoothAdapter.isDiscovering()){
                                mBluetoothAdapter.cancelDiscovery();
                            }
                            //workaround for device API fragmentation
                            int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
                            ActivityCompat.requestPermissions(MultiplayerActivity.this,
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                            //Start scanning nearby devices
                            mBluetoothAdapter.startDiscovery();
                            //Register broadcast receiver to receive info about nearby device
                            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                            registerReceiver(mDeviceListReceiver, filter);
                            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                            registerReceiver(mDeviceListReceiver, filter);
                            //Shows a dialog with list of available devices
                            showNearbyDevicesToPair();
                            break;
                        //If user wants to be host
                        case R.id.rbHost:
                            //Make sure device is discoverable for 300 seconds
                            ensureDiscoverable();
                            break;
                    }
                }
            });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_ENABLE_BT:
                if(resultCode == Activity.RESULT_CANCELED){
                    Toast.makeText(this, "Bluetooth must be enabled", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case ENSURE_ENABLE_DISCOVERABLE:
                if(resultCode == Activity.RESULT_CANCELED){
                    Toast.makeText(this, "Bluetooth must be made discoverable", Toast.LENGTH_LONG).show();
                    finish();
                }else{
                    //If user agrees to make device discoverable then start accept connection service
                    mDialog = new ProgressDialog(this);
                    mDialog.setTitle("Waiting for someone to join");
                    mDialog.setCancelable(false);
                    mAcceptConnService = new AcceptConnectionService("Pong2d",this);
                    mAcceptConnService.start();
                    //Show a dialog till a connection is accepted.
                    mDialog.show();
                }
                break;
        }
    }

    private void ensureDiscoverable() {
        //if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivityForResult(discoverableIntent,ENSURE_ENABLE_DISCOVERABLE);
        //}
    }

    private ArrayAdapter<String> mDeviceListAdapter;

    /**
     * This receiver receives a device info from Bluetooth discovery intent result and populate
     * it in the dialog. If no device is available then it shows the message.
     */
    private BroadcastReceiver mDeviceListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
               // Log.e("Multiplayer",device.getName());
                mDeviceListAdapter.add(device.getName() + "\n" + device.getAddress());
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mDeviceListAdapter.getCount() == 0) {
                    String noDevices = "No devices found";
                    MultiplayerActivity.this.unregisterReceiver(mDeviceListReceiver);
                    mDeviceListAdapter.add(noDevices);
                }
            }

        }
    };

    private void showNearbyDevicesToPair(){
        AlertDialog.Builder devicesDialog =  new AlertDialog.Builder(this);
        devicesDialog.setTitle("Nearby Devices");
        mDeviceListAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);
        //If user cancels the discovery then, the dialog is dismissed.
        devicesDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(mBluetoothAdapter != null) {
                    mBluetoothAdapter.cancelDiscovery();
                }
                MultiplayerActivity.this.unregisterReceiver(mDeviceListReceiver);
            }
        });
        //When user selects a device, the device info is extracted and a Bluetooth device object is created with that info
        //After getting the device, it is passed to connect service, which tries to establish a connection with the device.
        devicesDialog.setAdapter(mDeviceListAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(mBluetoothAdapter != null) {
                    mBluetoothAdapter.cancelDiscovery();
                }
                MultiplayerActivity.this.unregisterReceiver(mDeviceListReceiver);
                String info = mDeviceListAdapter.getItem(which);
                String deviceName = null;
                if(info != null && info.length() >= 17)
                     deviceName = info.substring(info.length()-17);
                if(deviceName != null) {
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceName);
                    mConnectService = new ConnectService("pong2d", device, MultiplayerActivity.this);
                    mConnectService.start();
                }
            }
        });
        devicesDialog.create().show();
    }


    @Override
    public void connected(boolean hostingGame, BluetoothSocket socket) {
        if(mDialog != null) mDialog.dismiss();
        //Stop any service which is listening for connections
        if(mAcceptConnService != null){
            mAcceptConnService.cancel();
            mAcceptConnService = null;
        }
        //Stop any service that is trying to connect to a device
        if(mConnectService != null){
            //mConnectService.cancel();
            mConnectService = null;
        }

        Intent pong = new Intent(this, PongActivity.class);
        pong.putExtra(PongActivity.GAME_MODE, GameThread.GAME_MODE_MULTIPLAYER);
        pong.putExtra(PongTable.KEY_HOSTING_GAME, hostingGame);
        SocketUtil.setSocket(socket);
        startActivity(pong);
    }

    @Override
    public void connectionFailed() {
        if(mDialog != null) mDialog.dismiss();
        if(mAcceptConnService != null){
            mAcceptConnService.cancel();
        }
        if(mConnectService != null){
            mConnectService.cancel();
        }
//        Toast.makeText(this, "Cannot establish connection. Please try again", Toast.LENGTH_LONG).show();
    }
}
