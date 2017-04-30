package game.prateek.pong2d.utils;

import java.io.Serializable;

/**
 * Created by Prateek Gupta on 4/29/17.
 */

public class GameStateObject implements Serializable {

    public float playerRacquetTop;
    public float opponentRacquetTop;
    public int playerScore;
    public int opponentScore;
    public float ballCx;
    public float ballCy;
    public float ballVx;
    public float ballVy;

}
