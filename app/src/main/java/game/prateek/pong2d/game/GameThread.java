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
 */

public class GameThread extends Thread {

    public static final int STATE_READY = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_WIN = 3;
    public static final int STATE_LOSE = 4;
    private static final String TAG = GameThread.class.getSimpleName();
    private static final String KEY_PLAYER_DATA = "Player";
    private static final String KEY_OPPONENT_DATA = "Opponent";
    private static final String KEY_BALL_DATA = "Ball";
    private static final String KEY_GAME_STATE = "GameState";

    private boolean mSensorsOn;

    private final Context mCtx;
    private final SurfaceHolder mSurfaceHolder;
    private final PongTable mPongTable;
    private final Handler mGameStatusHandler;
    private final Handler mScoreHandler;
    private final SensorUtil mSensorListener;

    private ObjectInputStream bleInStream;
    private ObjectOutputStream bleOutStream;

    private boolean mRun = false;
    private int mGameState;
    private Object mRunLock;

    private static final int PHYS_FPS = 60;

    public static final int GAME_MODE_SINGLE = 100;
    public static final int GAME_MODE_MULTIPLAYER = 101;
    private int mGameMode;

    public GameThread(Context ctx, SurfaceHolder holder, PongTable pongTable, Handler statushandler, Handler scoreHandler) {
        Log.e(TAG, "GameThread Constructor called");

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

        long mNextGameTick = SystemClock.uptimeMillis();
        int skipTicks = 1000 / PHYS_FPS;
        while (mRun) {
            Canvas c = null;
            try {
                c = mSurfaceHolder.lockCanvas(null);
                if (c != null) {
                    synchronized (mSurfaceHolder) {
                        if (mGameState == STATE_RUNNING) {
                            if (mGameMode == GAME_MODE_MULTIPLAYER) {
                                if (mPongTable.ismPlayerHosting()) {
                                    writeToOpponent();
                                    readFromOpponent();
                                    mPongTable.update(c);
                                } else {
                                    readFromOpponent();
                                    writeToOpponent();
                                }
                            }else{
                                mPongTable.update(c);
                            }
                        }
                        synchronized (mRunLock) {
                            if (mRun) {
                                mPongTable.draw(c);
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (c != null) {
                    mSurfaceHolder.unlockCanvasAndPost(c);
                }
            }
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

    public void setState(int mode) {
        synchronized (mSurfaceHolder) {
            mGameState = mode;
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

    public void pauseGame() {
        synchronized (mSurfaceHolder) {
            if (mGameState == STATE_RUNNING) {
                setState(STATE_PAUSED);
            }
            if (mSensorsOn) mSensorListener.unregisterListener();
        }
    }

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
        Log.e(TAG, "Tablewidth = " + mPongTable.getmTableWidth());
        Log.e(TAG, "TableHeight = " + mPongTable.getmTableHeight());
        Log.e(TAG, " Read Player = " + mPongTable.getPlayer().toString());
        Log.e(TAG, " REad Opponent = " + mPongTable.getOpponent().toString());
    }
    private synchronized void writeToOpponent() throws IOException {
        GameStateObject gameState = new GameStateObject();
        gameState.ballCx = mPongTable.getBall().cx;
        gameState.ballCy = mPongTable.getBall().cy;
        gameState.ballVx = mPongTable.getBall().velocity_x;
        gameState.ballVx = mPongTable.getBall().velocity_y;
        gameState.playerRacquetTop = mPongTable.getPlayer().bounds.top;
        gameState.opponentScore = mPongTable.getOpponent().score;
        gameState.playerScore = mPongTable.getPlayer().score;
        //gameState.gameState = mGameState;
        bleOutStream.writeObject(gameState);
        Log.e(TAG, " Write Player = " + mPongTable.getPlayer().toString());
        Log.e(TAG, " Write Opponent = " + mPongTable.getOpponent().toString());
        Log.e(TAG, "Ball = " + mPongTable.getBall().toString());
    }

}
