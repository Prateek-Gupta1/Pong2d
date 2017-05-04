package game.prateek.pong2d;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import game.prateek.pong2d.game.GameThread;
import game.prateek.pong2d.utils.SocketUtil;
import game.prateek.pong2d.view.PongTable;

/**
 * This is the main game activity that renders the game surface on the screen. It sets up the game mode and the table, but does not draw
 * any object. It establishes an initial connection with the opposite device, communicates the dimension of the device and sets up the table
 * dimensions.
 */

public class PongActivity extends AppCompatActivity {

    private static final String TAG = PongActivity.class.getSimpleName();
    private GameThread mGameThread;
    private SharedPreferences mSharedPref;
    public static final String GAME_MODE = "game_mode";
    boolean mPlayerHosting;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(TAG, "OnCreate Called");
        setContentView(R.layout.activity_pong);

        Intent intent = getIntent();
        //get if the player is hosting the game
        mPlayerHosting = intent.getBooleanExtra(PongTable.KEY_HOSTING_GAME, true);
        //Get the game mode
        int gameMode = intent.getIntExtra(GAME_MODE,GameThread.GAME_MODE_SINGLE);

        //Initialize pong table. This does not start the game but renders the game once.
        final PongTable table = (PongTable) findViewById(R.id.pongTable);
        table.setScoreOpponent((TextView)findViewById(R.id.tvScoreOpponent));
        table.setScorePlayer((TextView) findViewById(R.id.tvScorePlayer));
        table.setStatusView((TextView)findViewById(R.id.tvGameStatus));
        table.playerHostingGame(mPlayerHosting);

        mSharedPref = getSharedPreferences(getString(R.string.pref_file), Context.MODE_PRIVATE);
        //Check if the User has chosen sensors to guide the racquet
        boolean toggleSensors = mSharedPref.getBoolean(SettingsActivity.KEY_SENSOR_SELECTED,false);

        //get the game thread
        mGameThread = table.getGame();

        //Toggle sensor
        mGameThread.setSensorsOn(toggleSensors);

        //Set the game mode - single or multiplayer
        mGameThread.setGameMode(gameMode);

        if(savedInstanceState == null){
            Log.e(TAG, "Setting new state");
            //Set game mode to ready, but its not running yet
            if(gameMode != GameThread.GAME_MODE_MULTIPLAYER)
                mGameThread.setState(GameThread.STATE_READY);
        }else {
            mGameThread.restoreState(savedInstanceState);
        }

        //If Multi-Player mode is selected then run the initial setup task
        if(gameMode == GameThread.GAME_MODE_MULTIPLAYER) {
            InitPongTableAsync task = new InitPongTableAsync(table);
            task.execute();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGameThread.pauseGame();
        Log.e(TAG, "OnPause Called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "OnResume Called");
        if(mGameThread.getGameState() == GameThread.STATE_PAUSED){
            mGameThread.resumeGame();
            boolean toggleSensors = mSharedPref.getBoolean(SettingsActivity.KEY_SENSOR_SELECTED,false);
            mGameThread.setSensorsOn(toggleSensors);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketUtil.closeSocket();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter.isEnabled()){
            adapter.disable();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.e(TAG, "OnSaveInstanceState Called");
        mGameThread.saveState(outState);
    }

    private class InitPongTableAsync extends AsyncTask<Void, Void, Void> {

        private ProgressDialog dialog;
        private BluetoothSocket socket;
        private PongTable table;
        //This devices specifications
        private int windowPixelWidth;
        private int windowPixelHeight;
        private float windowDesity;
        //Opponents device specifications
        Integer oppWidth = new Integer(-1);
        Integer oppHeight = new Integer(-1);
        Float oppDensity = new Float(-1.0f);

        //Final game dimensions which both the devices will agree
        private int mGameWidth;
        private int mGameHeight;

        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        public InitPongTableAsync(PongTable table) {
            Log.e(TAG, "InitPongTableAsync constructor Called");
            this.table = table;

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //Setup the progress dialog and show it till the devices have communicated the dimensions
            dialog = new ProgressDialog(PongActivity.this);
            dialog.setTitle("Setting up the Table");
            dialog.setCancelable(false);

            //Get this device's specifications
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            windowPixelWidth = metrics.widthPixels;
            windowPixelHeight = metrics.heightPixels;
            windowDesity = metrics.density;
            //Log.e(TAG, "Showing dialpog");
            try {
                socket = SocketUtil.getSocket();
                //Setup the streams (Input and Output)
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                //send the specifications to the other device
                write(out);

                //Global access to streams
                SocketUtil.setInput(in);
                SocketUtil.setOutput(out);
            }catch(Exception e){
                Log.e(TAG, e.getMessage());
            }
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                //Read the specification from the opposite device
                read(in);
                //Get the game dimensions, which is the minimum of both the devices.
                mGameHeight = (int) Math.min(windowPixelHeight , oppHeight );
                mGameWidth = (int) Math.min(windowPixelWidth , oppWidth );

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } catch (ClassNotFoundException e) {
                Log.e(TAG, e.getMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            dialog.dismiss();

            //Set the game dimensions
            table.setmTableHeight(mGameHeight);
            table.setmTableWidth(mGameWidth);
            table.rescaleTableEntities();

            //Make streams accessible to the game thread
            mGameThread.setStreams(out,in);
            mGameThread.setState(GameThread.STATE_READY);
        }

        /**
         * Helper function to read the specifications of the other device
         * @param in
         * @throws IOException
         * @throws ClassNotFoundException
         */
        private void read(ObjectInputStream in) throws IOException, ClassNotFoundException {
            Object objWidth = in.readObject();
           // Log.e(TAG, "Reading object");
            if(objWidth instanceof Integer){
                oppWidth = (Integer)objWidth;
            }
            Object objHeight = in.readObject();
            if(objHeight instanceof Integer){
                oppHeight = (Integer) objHeight;
            }
            Object objDensity = in.readObject();
            if(objDensity instanceof Float){
                oppDensity = (Float) objDensity;
            }
        }

        /**
         * Helper function to send the specifications to other device.
         * @param out
         * @throws IOException
         */
        private void write(ObjectOutputStream out) throws IOException{
            out.writeObject(new Integer(windowPixelWidth));
            //out.flush();
            //Log.e(TAG, "Writing pixel height");
            out.writeObject(new Integer(windowPixelHeight));
            //out.flush();
            out.writeObject(new Float(windowDesity));
            out.flush();
        }
    }

}
