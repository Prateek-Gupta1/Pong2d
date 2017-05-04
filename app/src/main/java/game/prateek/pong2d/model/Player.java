package game.prateek.pong2d.model;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Created by Prateek Gupta on 4/8/17.
 * This is the model class for Player object in the pong game.
 * It encompasses all relevant properties of the Player Racket that defines its state in the game as well as the drawing aspects.
 */

public class Player {

    private int racquetWidth;
    private int racquetHeight;
    public int score;
    private Paint paint;
    public RectF bounds; // This object is useful in collision detections.

    public Player(int racquetWidth, int racquetHeight, Paint paint) {
        this.racquetWidth = racquetWidth;
        this.racquetHeight = racquetHeight;
        this.paint = paint;
        score = 0;
        bounds = new RectF(0, 0, racquetWidth, racquetHeight);
    }

    public void draw(Canvas canvas){
        canvas.drawRoundRect(bounds, 5, 5, paint);
    }

    public int getRacquetWidth() {
        return racquetWidth;
    }

    public int getRacquetHeight() {
        return racquetHeight;
    }

    public void setRacquetWidth(int racquetWidth) {
        this.racquetWidth = racquetWidth;
    }

    public void setRacquetHeight(int racquetHeight) {
        this.racquetHeight = racquetHeight;
    }

    public Paint getPaint() {
        return paint;
    }

    /**
     * Helper function for debugging
     * @return
     */
    @Override
    public String toString() {
        return "Width = " + racquetWidth + " Height = " + racquetHeight + " score = " + score + " Top = " + bounds.top + " Left = " + bounds.left;
    }
}
