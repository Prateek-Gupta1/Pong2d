package game.prateek.pong2d.game;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import game.prateek.pong2d.R;
import game.prateek.pong2d.utils.GameStateObject;
import game.prateek.pong2d.utils.SensorUtil;
import game.prateek.pong2d.utils.SocketUtil;
import game.prateek.pong2d.view.PongTable;

/**
 * Created by Prateek Gupta on 4/8/17.
 * This is the heart of the Game. It creates and maintains the Game Loop, updates the state of the objects on the table,
 * manages the state of the Game and handles communicate the states between two players,
 * if user selects multiplayer option, via bluetooth's input output streams.
 *
 */

public class GameThread extends Thread {

    private static final String TAG = GameThread.class.getSimpleName();

    //Game states
    public static final int STATE_READY = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_WIN = 3;
    public static final int STATE_LOSE = 4;

    //Keys to store the state of the game in savedInstanceState object
    private static final String KEY_PLAYER_DATA = "Player";
    private static final String KEY_OPPONENT_DATA = "Opponent";
    private static final String KEY_BALL_DATA = "Ball";
    private static final String KEY_GAME_STATE = "GameState";

    //Check if user has selected sensors to guide the racket
    private boolean mSensorsOn;

    //Game view objects
    private final Context mCtx;
    private final SurfaceHolder mSurfaceHolder;
    private final PongTable mPongTable;
    private final Handler mGameStatusHandler;
    private final Handler mScoreHandler;
    private final SensorUtil mSensorListener; // Sensor listener that reacts to change in x,y,z values of the device.

    //Bluetooth streams
    private ObjectInputStream bleInStream;
    private ObjectOutputStream bleOutStream;

    //Game progress and state
    private boolean mRun = false;
    private int mGameState;
    private Object mRunLock;

    //Frames per second
    private static final int PHYS_FPS = 60;

    //Game modes
    public static final int GAME_MODE_SINGLE = 100;
    public static final int GAME_MODE_MULTIPLAYER = 101;
    private int mGameMode;


    public GameThread(Context ctx, SurfaceHolder holder, PongTable pongTable, Handler statushandler, Handler scoreHandler) {
        //Log.e(TAG, "GameThread Constructor called");
        this.mCtx = ctx;
        this.mSurfaceHolder = holder;
        this.mPongTable = pongTable;
        this.mGameStatusHandler = statushandler;
        this.mScoreHandler = scoreHandler;
        this.mRunLock = new Object();
        mSensorListener = new SensorUtil(mCtx);
    }

    @Override
    public void run() {
        Log.e(TAG, "run method called");

        // get current time since last bootup
        long mNextGameTick = SystemClock.uptimeMillis();
        //Number of frames
        int skipTicks = 1000 / PHYS_FPS;
        while (mRun) {
            Canvas c = null;
            try {
                c = mSurfaceHolder.lockCanvas(null); // get hold of canvas object from view
                if (c != null) {
                    synchronized (mSurfaceHolder) { // lock the state of game
                        if (mGameState == STATE_RUNNING) { // If game is running, start updating the state of objects, else do nothing
                            if (mGameMode == GAME_MODE_MULTIPLAYER) {

                                // If player is hosting in multiplayer mode, read-from and write-to stream
                                // and update the state of object
                                if (mPongTable.ismPlayerHosting()) {
                                    writeToOpponent();
                                    readFromOpponent();
                                    mPongTable.update(c);
                                } else {
                                    //If player is not hosting, then get the state of both the players as evaluated by
                                    // host device and update it in the client device
                                    writeToOpponent();
                                    readFromOpponent();
                                }
                            }else{
                                //If game mode is Single Player
                                mPongTable.update(c);
                            }
                        }
                        synchronized (mRunLock) { // If the game is Not paused
                            if (mRun) {
                                mPongTable.draw(c); // draw the objects on canvas
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                Log.e(TAG, e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                if (c != null) {
                    //Release lock from canvas so that surface can draw
                    mSurfaceHolder.unlockCanvasAndPost(c);
                }
            }

            //Draw each frame after an interval of frame rate
            mNextGameTick += skipTicks;
            long sleepTime = mNextGameTick - SystemClock.uptimeMillis();
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted", e);
                }
            }
        }
    }


    public void setUpNewRound() {
        synchronized (mSurfaceHolder){
            mPongTable.setupTable();
            if(mSensorsOn) mSensorListener.registerListener(mPongTable);
        }
    }

    public void setRunning(boolean running){
        synchronized (mRunLock){
            mRun = running;
        }
    }

    /**
     * Saves the state of the game in the savedInstanceState object in the activity.
     * @param map
     */
    public void saveState(Bundle map) {
        synchronized (mSurfaceHolder) {
            map.putFloatArray(KEY_PLAYER_DATA,
                    new float[]{
                            mPongTable.getPlayer().bounds.left,
                            mPongTable.getPlayer().bounds.top,
                            mPongTable.getPlayer().score
                    }
            );

            map.putFloatArray(KEY_OPPONENT_DATA,
                    new float[]{
                            mPongTable.getOpponent().bounds.left,
                            mPongTable.getOpponent().bounds.top,
                            mPongTable.getOpponent().score
                    }
            );

            map.putFloatArray(KEY_BALL_DATA,
                    new float[]{
                            mPongTable.getBall().cx,
                            mPongTable.getBall().cy,
                            mPongTable.getBall().velocity_x,
                            mPongTable.getBall().velocity_y
                    }
            );

            map.putInt(KEY_GAME_STATE, mGameState);
        }
    }

    public void restoreState(Bundle map) {
        synchronized (mSurfaceHolder) {
            float[] humanPlayerData = map.getFloatArray(KEY_PLAYER_DATA);
            mPongTable.getPlayer().score = (int) humanPlayerData[2];
            mPongTable.movePlayer( mPongTable.getPlayer(), humanPlayerData[0], humanPlayerData[1]);

            float[] computerPlayerData = map.getFloatArray(KEY_OPPONENT_DATA);
            mPongTable.getOpponent().score = (int) computerPlayerData[2];
            mPongTable.movePlayer(mPongTable.getOpponent(), computerPlayerData[0], computerPlayerData[1]);

            float[] ballData = map.getFloatArray(KEY_BALL_DATA);
            mPongTable.getBall().cx = ballData[0];
            mPongTable.getBall().cy = ballData[1];
            mPongTable.getBall().velocity_x = ballData[2];
            mPongTable.getBall().velocity_y = ballData[3];

            int state = map.getInt(KEY_GAME_STATE);
            setState(state);
        }
    }

    /**
     * Takes the state of the game and selects the appropriate action that should be taken.
     * @param state
     */
    public void setState(int state) {
        synchronized (mSurfaceHolder) {
            mGameState = state;
            Resources res = mCtx.getResources();
            switch (mGameState) {
                case STATE_READY:
                    setUpNewRound();
                    break;
                case STATE_RUNNING:
                    hideStatusText();
                    break;
                case STATE_WIN:
                    setStatusText(res.getString(R.string.mode_win));
                    mPongTable.getPlayer().score++;
                    setUpNewRound();
                    break;
                case STATE_LOSE:
                    setStatusText(res.getString(R.string.mode_lose));
                    mPongTable.getOpponent().score++;
                    setUpNewRound();
                    break;
                case STATE_PAUSED:
                    setStatusText(res.getString(R.string.mode_pause));
                    break;
            }
        }
    }

    /**
     * set state of the game to PAUSED and unregisters the sensor listener.
     */
    public void pauseGame() {
        synchronized (mSurfaceHolder) {
            if (mGameState == STATE_RUNNING) {
                setState(STATE_PAUSED);
            }
            if (mSensorsOn) mSensorListener.unregisterListener();
        }
    }

    /**
     * Set the state to RUNNING so that the thread could draw the objects.
     * Registers for sensor listener if sensors are turned ON.
     */
    public void resumeGame() {
        synchronized (mSurfaceHolder) {
            setState(STATE_RUNNING);
        }

        if (mSensorsOn) mSensorListener.registerListener(mPongTable);
    }

    public void startNewGame(){
        synchronized (mSurfaceHolder){
            mPongTable.getPlayer().score = 0;
            mPongTable.getOpponent().score = 0;
            setUpNewRound();
            setState(STATE_RUNNING);
        }
    }

    /**
     * Checks if the Game is either Paused or Ready
     * @return
     */
    public boolean isBetweenRounds() {
        return mGameState != STATE_RUNNING;
    }

    private void setStatusText(String text) {
        Message msg = mGameStatusHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("text", text);
        b.putInt("visibility", View.VISIBLE);
        msg.setData(b);
        mGameStatusHandler.sendMessage(msg);
    }

    private void hideStatusText() {
        Message msg = mGameStatusHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt("visibility", View.INVISIBLE);
        msg.setData(b);
        mGameStatusHandler.sendMessage(msg);
    }

    public void setScoreText(String playerScore, String opponentScore ) {
        Message msg = mScoreHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("player", playerScore);
        b.putString("opponent", opponentScore);
        msg.setData(b);
        mScoreHandler.sendMessage(msg);
    }

    public synchronized int getGameState(){ return mGameState; }

    public void setSensorsOn(boolean toggle){
        mSensorsOn = toggle;
    }

    public boolean SensorsOn(){
        return mSensorsOn;
    }

    public synchronized void setGameMode(int mode){
        mGameMode = mode;

    }

    public synchronized void setStreams(ObjectOutputStream out, ObjectInputStream in){
        bleInStream = in;
        bleOutStream = out;
    }
    public int getGameMode(){
        return mGameMode;
    }

    /**
     * Helper function that reads the Game State from the Input stream of the bluetooth connection.
     * If the device is Hosting the game then it only reads data related to opponent's vertical movement
     * and update the UI accordingly.
     * If the device is client, then it reads all necessary object data and updates the object state according to data
     * send by Host device.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private synchronized void readFromOpponent() throws IOException, ClassNotFoundException {
        Object obj = bleInStream.readObject();
        if(obj instanceof GameStateObject){
            GameStateObject gameState = (GameStateObject) obj;
            if(mPongTable.ismPlayerHosting()){
                mPongTable.movePlayer(mPongTable.getOpponent(), mPongTable.getOpponent().bounds.left,gameState.playerRacquetTop);
                //mPongTable.getOpponent().bounds.top = gameState.playerRacquetTop;
            }else {
                //mPongTable.getOpponent().bounds.top = gameState.playerRacquetTop;
                //setState(gameState.gameState);
                mPongTable.movePlayer(mPongTable.getOpponent(), mPongTable.getOpponent().bounds.left,gameState.playerRacquetTop);
                mPongTable.getBall().cx = mPongTable.getmTableWidth() - gameState.ballCx;
                mPongTable.getBall().cy = gameState.ballCy;
                mPongTable.getBall().velocity_x = -gameState.ballVx;
                mPongTable.getBall().velocity_y = gameState.ballVy;
                mPongTable.getPlayer().score = gameState.opponentScore;
                mPongTable.getOpponent().score = gameState.playerScore;
            }
        }
        /*Log.e(TAG, "Tablewidth = " + mPongTable.getmTableWidth());
        Log.e(TAG, "TableHeight = " + mPongTable.getmTableHeight());
        Log.e(TAG, " Read Player = " + mPongTable.getPlayer().toString());
        Log.e(TAG, " REad Opponent = " + mPongTable.getOpponent().toString());*/
    }

    /**
     * Helper function that writes the Game state on the OutputStream of Bluetooth connection.
     * All data is communicated regardless of device status.
     * @throws IOException
     */
    private synchronized void writeToOpponent() throws IOException {
        GameStateObject gameState = new GameStateObject();
        gameState.ballCx = mPongTable.getBall().cx;
        gameState.ballCy = mPongTable.getBall().cy;
        gameState.ballVx = mPongTable.getBall().velocity_x;
        gameState.ballVx = mPongTable.getBall().velocity_y;
        gameState.playerRacquetTop = mPongTable.getPlayer().bounds.top;
        gameState.opponentScore = mPongTable.getOpponent().score;
        gameState.playerScore = mPongTable.getPlayer().score;
        bleOutStream.writeObject(gameState);
       /* Log.e(TAG, " Write Player = " + mPongTable.getPlayer().toString());
        Log.e(TAG, " Write Opponent = " + mPongTable.getOpponent().toString());
        Log.e(TAG, "Ball = " + mPongTable.getBall().toString());*/
    }

}
