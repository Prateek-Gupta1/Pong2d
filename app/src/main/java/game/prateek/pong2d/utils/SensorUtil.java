package game.prateek.pong2d.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import game.prateek.pong2d.view.PongTable;

/**
 * Created by Prateek Gupta on 4/18/17.
 *
 */

public class SensorUtil implements SensorEventListener {

    static final String TAG = "SensorUtil";

    Context mContext;

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private float[] mRotationMatrix = new float[16];

    private float[] mOrientation = new float[9];

    SensorManager mSensorManager;

    OnDeviceRotatedListener mListener;

    public SensorUtil(Context mContext) {
        this.mContext = mContext;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) {
            System.arraycopy(event.values, 0, mAccelerometerReading, 0, mAccelerometerReading.length);
        }
        else if (event.sensor == mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) {
            System.arraycopy(event.values, 0, mMagnetometerReading, 0, mMagnetometerReading.length);
        }
        mSensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerometerReading, mMagnetometerReading);

        mSensorManager.getOrientation(mRotationMatrix, mOrientation);

        //Log.e(TAG,"Pitch = " + mOrientation[1]);
        Log.e(TAG,"Cos = " + Math.cos(Math.toDegrees(mOrientation[1])));

        mListener.onDeviceRotated((float) Math.cos(Math.toDegrees(mOrientation[1])));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void registerListener(OnDeviceRotatedListener listener){
        if(mSensorManager != null) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);

        }
        if(listener != null && mListener == null) mListener = listener;
    }

    public void unregisterListener(){
        if(mListener != null) mListener = null;
        if(mSensorManager != null)
            mSensorManager.unregisterListener(this);
    }

    /**
     * Callback listener to listen to changes in sensor's value when the device is rotated.
     */
    public interface OnDeviceRotatedListener{
         void onDeviceRotated(float dy);
    }

}
