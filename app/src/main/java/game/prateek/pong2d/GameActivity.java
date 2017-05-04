/**
 * Author - Prateek Gupta
 * This is the launcher activity and allows users to select game mode i.e. Single Player or Multiplayer.
 * In Single player mode, the user plays against the inbuilt Artificially not so intelligent computer.
 * In Multiplayer, user can either host a game or connect to a game via bluetooth and start playing against a more intelligent Human user.
 * Users are also allowed to choose the way they want to maneuver the Racket. They choose to drag it or use sensors to guide it.
 *
 * The following resources helped me to build this game. Most of the code comes from here, but I tweaked a significant part to achieve the goals.
 * https://github.com/catalinc/pong-game-android
 * https://examples.javacodegeeks.com/android/android-bluetooth-connection-example/
 */

package game.prateek.pong2d;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import game.prateek.pong2d.game.GameThread;
import game.prateek.pong2d.view.PongTable;

public class GameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        //VS AI option
        TextView tvVsAI = (TextView) findViewById(R.id.tvVsAI);
        tvVsAI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GameActivity.this, PongActivity.class);
                intent.putExtra(PongActivity.GAME_MODE, GameThread.GAME_MODE_SINGLE); // Put mode single player
               // intent.putExtra(PongTable.KEY_HOSTING_GAME, true); // set game hosting
                startActivity(intent);
            }
        });

        //Settings icon
        ImageView ivSettings = (ImageView) findViewById(R.id.ivSettings);
        ivSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GameActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        //Vs PLayer option
        TextView tvVsPLayer = (TextView) findViewById(R.id.tvVsPlayer);
        tvVsPLayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GameActivity.this,MultiplayerActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Disable Bluetooth if it is enabled
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter.isEnabled()){
            adapter.disable();
        }
    }
}
