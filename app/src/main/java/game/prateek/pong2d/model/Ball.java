package game.prateek.pong2d.model;

import android.graphics.Canvas;
import android.graphics.Paint;

import game.prateek.pong2d.view.PongTable;

/**
 * Created by Prateek Gupta on 4/8/17.
 */

public class Ball {

    public float cx;
    public float cy;
    private int radius;
    public float velocity_x;
    public float velocity_y;
    private Paint paint;

    public Ball(int radius, Paint paint) {
        this.paint = paint;
        this.radius = radius;
        this.velocity_x = -PongTable.PHY_BALL_SPEED;
        this.velocity_y = -PongTable.PHY_BALL_SPEED;
    }

    public void draw(Canvas canvas){
        canvas.drawCircle(cx, cy, radius, paint);
    }

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
}
