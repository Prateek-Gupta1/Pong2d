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


public class PongActivity extends AppCompatActivity {

    private static final String TAG = PongActivity.class.getSimpleName();
    private GameThread mGameThread;
    private SharedPreferences mSharedPref;
    public static final String GAME_MODE = "game_mode";
    boolean mPlayerHosting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pong);

        Intent intent = getIntent();
        mPlayerHosting = intent.getBooleanExtra(PongTable.KEY_HOSTING_GAME, true);
        int gameMode = intent.getIntExtra(GAME_MODE,GameThread.GAME_MODE_SINGLE);

        final PongTable table = (PongTable) findViewById(R.id.pongTable);
        table.setScoreOpponent((TextView)findViewById(R.id.tvScoreOpponent));
        table.setScorePlayer((TextView) findViewById(R.id.tvScorePlayer));
        table.setStatusView((TextView)findViewById(R.id.tvGameStatus));
        table.playerHostingGame(mPlayerHosting);

        mSharedPref = getSharedPreferences(getString(R.string.pref_file), Context.MODE_PRIVATE);
        boolean toggleSensors = mSharedPref.getBoolean(SettingsActivity.KEY_SENSOR_SELECTED,false);

        mGameThread = table.getGame();

        mGameThread.setSensorsOn(toggleSensors);
        mGameThread.setGameMode(gameMode);

        if(savedInstanceState == null){
            mGameThread.setState(GameThread.STATE_READY);
        }else{
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mGameThread.getGameState() == GameThread.STATE_PAUSED){
            mGameThread.resumeGame();
            boolean toggleSensors = mSharedPref.getBoolean(SettingsActivity.KEY_SENSOR_SELECTED,false);
            mGameThread.setSensorsOn(toggleSensors);
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mGameThread.saveState(outState);
    }

    private class InitPongTableAsync extends AsyncTask<Void, Void, Void> {

        public InitPongTableAsync(PongTable table) {
            this.table = table;
        }

        private ProgressDialog dialog;
        private BluetoothSocket socket;
        private PongTable table;
        private int windowPixelWidth;
        private int windowPixelHeight;
        private float windowDesity;
        Integer width = new Integer(-1);
        Integer height = new Integer(-1);
        Float density = new Float(-1.0f);

        private int gameWidth;
        private int gameHeight;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(PongActivity.this);
            dialog.setTitle("Setting up the Table");
            dialog.setCancelable(false);
            dialog.show();
            socket = SocketUtil.getSocket();
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            windowPixelWidth = metrics.widthPixels;
            windowPixelHeight = metrics.heightPixels;
            windowDesity = metrics.density;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ObjectInputStream in = null;
            ObjectOutputStream out = null;

            try {
                in = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(socket.getOutputStream());

                if(mPlayerHosting) { read(in); write(out); }
                else { write(out); read(in); }

                gameHeight = (int) Math.min(windowPixelHeight / windowDesity, height / density);
                gameWidth = (int) Math.min(windowPixelWidth / windowDesity, width / density);

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } catch (ClassNotFoundException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                if(in != null){
                    try { in.close(); }
                    catch (IOException e) { Log.e(TAG,e.getMessage()); }
                }
                if(out != null){
                    try { out.close(); }
                    catch (IOException e) { Log.e(TAG,e.getMessage()); }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            dialog.dismiss();
            table.setmTableHeight(gameHeight);
            table.setmTableWidth(gameWidth);
        }

        private void read(ObjectInputStream in) throws IOException, ClassNotFoundException {
            Object objWidth = in.readObject();
            if(objWidth instanceof Integer){
                width = (Integer)objWidth;
            }
            Object objHeight = in.readObject();
            if(objHeight instanceof Integer){
                height = (Integer) objHeight;
            }
            Object objDensity = in.readObject();
            if(objDensity instanceof Float){
                density = (Float) objDensity;
            }
        }

        private void write(ObjectOutputStream out) throws IOException{
            out.writeObject(new Integer(windowPixelWidth));
            out.flush();
            out.writeObject(new Integer(windowPixelHeight));
            out.flush();
            out.writeObject(new Float(windowDesity));
            out.flush();
        }
    }

}
