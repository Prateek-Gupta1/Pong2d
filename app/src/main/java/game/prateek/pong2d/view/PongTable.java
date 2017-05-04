package game.prateek.pong2d.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.widget.TextView;

import java.util.Random;

import game.prateek.pong2d.R;
import game.prateek.pong2d.game.GameThread;
import game.prateek.pong2d.model.Ball;
import game.prateek.pong2d.model.Player;
import game.prateek.pong2d.utils.SensorUtil;

/**
 * Created by Prateek Gupta on 4/8/17.
 */

public class PongTable extends SurfaceView implements SurfaceHolder.Callback, SensorUtil.OnDeviceRotatedListener {

    private static final String TAG = PongTable.class.getSimpleName();
    private GameThread mGame;
    private TextView mStatus;
    private TextView mScorePlayer;
    private TextView mScoreOpponent;
    private Player mPlayer;
    private Player mOpponent;
    private Ball mBall;
    private Paint mNetPaint; // The middle line on the table
    private Paint mTableBoundsPaint; // To draw Bounds of the table
    private int mTableWidth;
    private int mTableHeight;
    private Context mContext;


    SurfaceHolder mHolder;
    public static float PHY_RACQUET_SPEED = 15.0f;
    public static float PHY_BALL_SPEED = 15.0f;

    public static final String KEY_HOSTING_GAME = "hosting_game";
    private boolean mPlayerHosting = true;

    private float mAiMoveProbability;

    /**
     * Helper method to init the game elements
     * @param ctx
     * @param attr
     */
    public void initPongTable(Context ctx, AttributeSet attr){

        Log.e(TAG, "InitPongTable called");
        mContext = ctx;
        mHolder = getHolder();
        mHolder.addCallback(this);

        //initialize the Game Thread. Not started the thread yet.
        mGame = new GameThread(this.getContext(), mHolder, this,
                new Handler(){

                    @SuppressWarnings("WrongConstant")
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        //TODO write code to handle game status
                        mStatus.setVisibility(msg.getData().getInt("visibility"));
                        mStatus.setText(msg.getData().getString("text"));
                    }
                },
                new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        //TODO write code to handle score
                        mScorePlayer.setText(msg.getData().getString("player"));
                        mScoreOpponent.setText(msg.getData().getString("opponent"));
                    }
                }
        );
        setFocusable(true);

        //Get style attributes of custom surface view
        TypedArray a = ctx.obtainStyledAttributes(attr, R.styleable.PongTable);
        int racketHeight = a.getInteger(R.styleable.PongTable_racketHeight,340);
        int racketWidth =  a.getInteger(R.styleable.PongTable_racketWidth,100);
        int ballRadius =  a.getInteger(R.styleable.PongTable_ballRadius,25);


        //PHY_BALL_SPEED = (float)ballRadius;

        a.recycle();

        //Set player
        Paint playerPaint = new Paint();
        playerPaint.setAntiAlias(true);
        playerPaint.setColor(ContextCompat.getColor(mContext,R.color.player_color));
        mPlayer = new Player(racketWidth, racketHeight, playerPaint);

        //Set Opponent
        Paint oppPaint = new Paint();
        oppPaint.setAntiAlias(true);
        oppPaint.setColor(ContextCompat.getColor(mContext,R.color.opponent_color));
        mOpponent = new Player(racketWidth, racketHeight, oppPaint);

        //Set Ball
        Paint ballPaint = new Paint();
        ballPaint.setAntiAlias(true);
        ballPaint.setColor(Color.BLACK);
        mBall = new Ball(ballRadius, ballPaint);

        //Draw middle line
        mNetPaint = new Paint();
        mNetPaint.setAntiAlias(true);
        mNetPaint.setColor(Color.BLACK);
        mNetPaint.setAlpha(80);
        mNetPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mNetPaint.setStrokeWidth(10.0f);
        mNetPaint.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));

        //Draw bounds
        mTableBoundsPaint = new Paint();
        mTableBoundsPaint.setAntiAlias(true);
        mTableBoundsPaint.setColor(Color.BLACK);
        mTableBoundsPaint.setStyle(Paint.Style.STROKE);
        mTableBoundsPaint.setStrokeWidth(15.0f);

        //Move probability of the AI, used in single player mode.
        //mimics the case when human players forget to move the racket
        mAiMoveProbability = 0.8f;
    }

    public PongTable(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPongTable(context, attrs);
    }

    public PongTable(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPongTable(context,attrs);
    }

    /**
     * called by game thread
     * @param canvas
     */
    @Override
    public void draw(Canvas canvas){
       // Log.e(TAG, "Draw called");
        super.draw(canvas);
        canvas.drawColor(ContextCompat.getColor(mContext,R.color.table_color));
        canvas.drawRect(0, 0, mTableWidth, mTableHeight, mTableBoundsPaint);

        int middle = mTableWidth / 2;
        canvas.drawLine(middle, 1, middle, mTableHeight - 1, mNetPaint);

        mGame.setScoreText(String.valueOf(mPlayer.score) , String.valueOf(mOpponent.score));

        //handleHit(mHumanPlayer);
        //handleHit(mComputerPlayer);

        mPlayer.draw(canvas);
        mOpponent.draw(canvas);
        mBall.draw(canvas);
    }

    /**
     * Checks for collisions and move the entities accordingly
     * @param canvas
     */
    public void update(Canvas canvas){

        if(checkCollisionPlayer(mPlayer,mBall)){
            handleCollision(mPlayer, mBall);

        }else if(checkCollisionPlayer(mOpponent, mBall)){
            handleCollision(mOpponent, mBall);

        }else if(checkCollisionWithTopOrBottomWall()){
            mBall.velocity_y = -mBall.velocity_y;

        }else if(checkCollisionWithLeftWall()){
            mGame.setState(GameThread.STATE_LOSE);
            return;
        }else if(checkCollisionWithRightWall()){
            mGame.setState(GameThread.STATE_WIN);
            return;
        }

        if(mGame.getGameMode() == GameThread.GAME_MODE_SINGLE)
            if(new Random(System.currentTimeMillis()).nextFloat() < mAiMoveProbability)
                doAI();

        mBall.moveBall(canvas);
        //Log.e(TAG," Player left = " + mPlayer.bounds.left + " Player top = " +mPlayer.bounds.top + "Player height = " + mPlayer.getRacquetHeight() + " width = "+ mPlayer.getRacquetWidth());
    }

    /**
     * Moves the Opponent racket in Single Player mode
     */
    private void doAI() {
        if (mOpponent.bounds.top > mBall.cy) {
            // move up
            movePlayer(mOpponent,
                    mOpponent.bounds.left,
                    mOpponent.bounds.top - PHY_RACQUET_SPEED);
        } else if (mOpponent.bounds.top + mOpponent.getRacquetHeight() < mBall.cy) {
            // move down
            movePlayer(mOpponent,
                    mOpponent.bounds.left,
                    mOpponent.bounds.top + PHY_RACQUET_SPEED);
        }
    }

    /**
     * Checks if the ball intersects the bounds of the PLayer
     * @param player
     * @param ball
     * @return
     */
    private boolean checkCollisionPlayer(Player player, Ball ball){
        return player.bounds.intersects(
                ball.cx - ball.getRadius(),
                ball.cy - ball.getRadius(),
                ball.cx + ball.getRadius(),
                ball.cy + ball.getRadius());
    }

    private boolean checkCollisionWithTopOrBottomWall(){
        return ((mBall.cy <= mBall.getRadius()) || (mBall.cy + mBall.getRadius() >= mTableHeight -1));
    }

    private boolean checkCollisionWithLeftWall(){
        return mBall.cx <= mBall.getRadius();
    }

    private boolean checkCollisionWithRightWall(){
        return mBall.cx + mBall.getRadius() >= mTableWidth - 1;
    }

    /**
     * Rebounds ball in the correct direction when it hits a Player.
     * Also increases the speed of the ball
     * @param player
     * @param ball
     */
    private void handleCollision(Player player, Ball ball){
        ball.velocity_x = -ball.velocity_x * 1.05f;
        if(player == mPlayer){
            ball.cx = mPlayer.bounds.right + ball.getRadius();
        }else if(player == mOpponent){
            ball.cx = mOpponent.bounds.left - ball.getRadius();
            PHY_RACQUET_SPEED = PHY_RACQUET_SPEED * 1.03f;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG, "OnSurfaceCreated called");
        mGame.setRunning(true);
        mGame.start();
    }

    public int getmTableWidth() {
        return mTableWidth;
    }

    public int getmTableHeight() {
        return mTableHeight;
    }

    public synchronized void setmTableWidth(int mTableWidth) {
        this.mTableWidth = mTableWidth;
    }

    public synchronized void setmTableHeight(int mTableHeight) {
        this.mTableHeight = mTableHeight;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(TAG, "surfaceChanged called");
        //Set the width and height of the table to devices pixel width and height
        if(mGame.getGameMode() == GameThread.GAME_MODE_SINGLE) {
            mTableWidth = width;
            mTableHeight = height;
        }
       // mTableWidth = width;
       // mTableHeight = height;
        mGame.setUpNewRound();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(TAG, "surfaceDestroyed called");

        //Tries to connect the Game Thread again in case the surface get demolished.
        boolean retry = true;
        mGame.setRunning(false);
        while (retry) {
            try {
                mGame.join();
                retry = false;
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }
        }

    }

    private boolean moving = false;
    private float mlastTouchY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(!mGame.SensorsOn()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    //Start game when touch down happens and game is not running
                    if (mGame.isBetweenRounds()) {
                        mGame.setState(GameThread.STATE_RUNNING);
                    } else {
                        //Check if the touch is within the bounds of racket
                        if (isTouchOnRacket(event, mPlayer)) {
                            moving = true;
                            //Store the touch position
                            mlastTouchY = event.getY();
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (moving) {
                        float y = event.getY();
                        //Get the difference b/w present position and last touch
                        float dy = y - mlastTouchY;
                        //Set the current touch position to last touch
                        mlastTouchY = y;
                        //Move player racket upto the difference in touch distances
                        movePlayerRacquet(dy, mPlayer);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    moving = false;
                    break;
            }
            //If sensors are ON then no need to monitor touch, just start game on first touch
        }else{
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                if (mGame.isBetweenRounds()) {
                    mGame.setState(GameThread.STATE_RUNNING);
                }
            }
        }

        return true;
    }

    /**
     * Moves players left and top bounds, keeping constant Height and width.
     * @param dy
     * @param player
     */
    public void movePlayerRacquet(float dy, Player player) {
        synchronized (mHolder) {
            movePlayer(player,
                    player.bounds.left,
                    player.bounds.top + dy);
        }
    }

    private boolean isTouchOnRacket(MotionEvent event, Player mPlayer) {
        return mPlayer.bounds.contains(event.getX(), event.getY());
    }


    public synchronized void movePlayer(Player player, float left, float top) {
        if (left < 2) {
            left = 2;
        } else if (left + player.getRacquetWidth() >= mTableWidth - 2) {
            left = mTableWidth - player.getRacquetWidth() - 2;
        }
        if (top < 0) {
            top = 0;
        } else if (top + player.getRacquetHeight() >= mTableHeight) {
            top = mTableHeight - player.getRacquetHeight() - 1;
        }
        player.bounds.offsetTo(left, top);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        Log.e(TAG, "onWindowFocusChanged called");
        super.onWindowFocusChanged(hasWindowFocus);
        if(!hasWindowFocus){
            mGame.pauseGame();
        }
    }

    public GameThread getGame(){
        return mGame;
    }

    /**
     * Places the players on the Table. Opponent on right and Player on left.
     */
    private void placePlayers(){
        mPlayer.bounds.offsetTo(2,(mTableHeight - mPlayer.getRacquetHeight())/2);
        mOpponent.bounds.offsetTo(mTableWidth - mOpponent.getRacquetWidth()-2, (mTableHeight - mOpponent.getRacquetHeight())/2);
    }

    /**
     * Places ball in the middle of the screen
     */
    private void placeBall(){
        mBall.cx = mTableWidth/2;
        mBall.cy = mTableHeight/2;
        mBall.velocity_y = (mBall.velocity_y / Math.abs(mBall.velocity_y) ) * PHY_BALL_SPEED;
        mBall.velocity_x = (mBall.velocity_x / Math.abs(mBall.velocity_x) ) * PHY_BALL_SPEED;
    }

    public void setupTable(){
        placeBall();
        placePlayers();
    }

    public Player getPlayer(){ return mPlayer; }
    public Player getOpponent(){ return mOpponent; }
    public Ball getBall(){ return mBall; }

    public void setPlayer(Player player){ if(player != null) mPlayer = player; }
    public void setBall(Ball ball){ if(ball != null) mBall = ball; }
    public void setOpponent(Player opponent){ if(opponent != null) mOpponent = opponent; }

    public void setScorePlayer(TextView view){ mScorePlayer = view; }
    public void setScoreOpponent(TextView view){ mScoreOpponent = view; }
    public void setStatusView( TextView view){ mStatus = view; }

    /**
     * scales the racket in Multiplayer mode
     */
    public void rescaleTableEntities(){
        float scale = 25.0f;//ctx.getResources().getDisplayMetrics().density;
        int racketHeight = ((int)(scale * mTableHeight)/100);
        // Log.e(TAG, "" + ctx.getResources().getDisplayMetrics().heightPixels);
        int racketWidth = ((int)(35 * racketHeight)/100);
        int ballRadius = racketWidth/4;

        mPlayer.setRacquetHeight(racketHeight);
        mPlayer.setRacquetWidth(racketWidth);
        mPlayer.bounds = new RectF(0,0,racketWidth, racketHeight);
        mOpponent.setRacquetWidth(racketWidth);
        mOpponent.setRacquetHeight(racketHeight);
        mOpponent.bounds = new RectF(0,0,racketWidth, racketHeight);
        mBall.setRadius(ballRadius);
    }

    /**
     * callback for sensor changes.
     * @param dy
     */
    @Override
    public void onDeviceRotated(float dy) {
        float deltaY = dy*20;
        //Log.e(TAG, "deltaY = " + deltaY);
        movePlayerRacquet(deltaY, mPlayer);
    }

    public synchronized void playerHostingGame(boolean playerHosting){
        mPlayerHosting = playerHosting;
    }

    public boolean ismPlayerHosting(){
        return mPlayerHosting;
    }
}
