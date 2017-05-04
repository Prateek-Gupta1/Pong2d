package game.prateek.pong2d.utils;

import java.io.Serializable;

/**
 * Created by Prateek Gupta on 4/29/17.
 * As Serializable object that is used as a wrapper of game state to be channeled on to the streams.
 */

public class GameStateObject implements Serializable {

    public float playerRacquetTop;
    public float opponentRacquetLeft;
    public int playerScore;
    public int opponentScore;
    public float ballCx;
    public float ballCy;
    public float ballVx;
    public float ballVy;
    public int gameState;

}
