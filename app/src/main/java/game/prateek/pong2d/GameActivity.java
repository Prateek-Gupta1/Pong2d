package game.prateek.pong2d;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import game.prateek.pong2d.game.GameThread;
import game.prateek.pong2d.view.PongTable;

public class GameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        TextView tvVsAI = (TextView) findViewById(R.id.tvVsAI);
        tvVsAI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GameActivity.this, PongActivity.class);
                intent.putExtra(PongActivity.GAME_MODE, GameThread.GAME_MODE_SINGLE);
                intent.putExtra(PongTable.KEY_HOSTING_GAME, true);
                startActivity(intent);
            }
        });

        ImageView ivSettings = (ImageView) findViewById(R.id.ivSettings);
        ivSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GameActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

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
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter.isEnabled()){
            adapter.disable();
        }
    }
}
