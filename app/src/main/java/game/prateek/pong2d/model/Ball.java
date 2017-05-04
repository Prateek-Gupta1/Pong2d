package game.prateek.pong2d.model;

import android.graphics.Canvas;
import android.graphics.Paint;

import game.prateek.pong2d.view.PongTable;

/**
 * Created by Prateek Gupta on 4/8/17.
 * This is the model class for Ball object in the pong game.
 * It encompasses all relevant properties of the ball that define its state in the game as well as the drawing aspects.
 */

public class Ball {

    public float cx; //X coordinate on the screen
    public float cy; // Y coordinate on the screen
    private int radius;
    public float velocity_x;
    public float velocity_y;
    private Paint paint; // To draw on canvas

    public Ball(int radius, Paint paint) {
        this.paint = paint;
        this.radius = radius;
        // Initializing ball velocity to predefined speed
        this.velocity_x = -PongTable.PHY_BALL_SPEED;
        this.velocity_y = -PongTable.PHY_BALL_SPEED;
    }

    /**
     * Draws the ball on the canvas
     * @param canvas
     */
    public void draw(Canvas canvas){
        canvas.drawCircle(cx, cy, radius, paint);
    }

    /**
     * Every time this function is called, the velocity of the ball is increased
     * and it ensures that the ball does not go out ouf bounds of the table
     * @param canvas
     */
    public void moveBall(Canvas canvas){
        cx += velocity_x;
        cy += velocity_y;

        if( cy < radius){
            cy = radius;
        }else if( cy + radius >= canvas.getHeight()){
            cy = canvas.getHeight() - radius -1;
        }

    }

    public int getRadius() {
        return radius;
    }

    public Paint getPaint() {
        return paint;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    /**
     * Helper function for debugging
     * @return
     */
    @Override
    public String toString() {
        return "Cx = " + cx + " Cy=" + cy + " velX= " + velocity_x + " vely = " + velocity_y;
    }
}
