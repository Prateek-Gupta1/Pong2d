/**
 * Gives user a choice to either Drag the racket or maneuver it using sensors.
 * The choice is stored in SharedPreference
 */
package game.prateek.pong2d;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.RadioGroup;

public class SettingsActivity extends AppCompatActivity {

    public static final String KEY_SENSOR_SELECTED = "sensor_selected";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        RadioGroup rgSetting = (RadioGroup) findViewById(R.id.rgSettings);
        SharedPreferences prefs = this.getSharedPreferences(getString(R.string.pref_file), Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();

        rgSetting.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if(checkedId == R.id.rbDrag){
                    editor.putBoolean(KEY_SENSOR_SELECTED, false);
                    editor.commit();
                    finish();

                }else if(checkedId == R.id.rbSensor){
                    editor.putBoolean(KEY_SENSOR_SELECTED, true);
                    editor.commit();
                    finish();
                }
            }
        });
    }
}
