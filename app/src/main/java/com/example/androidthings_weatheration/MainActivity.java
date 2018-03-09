package com.example.androidthings_weatheration;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.androidthings_weatheration.utils.BoardSpec;
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver;

import java.io.IOException;
import java.text.DecimalFormat;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {

    private static final String TAG=MainActivity.class.getSimpleName();


    private enum DisplayMade{
        TEMPERATURE,
        PRESSURE
    }

    private DisplayMade mDisplayMade=DisplayMade.TEMPERATURE;

    private SensorManager mSensorManager;
    private Bmx280SensorDriver mEnvironmentalSensorDriver;

    private  float mLastTemperature;
    private  float mLastPressure;

    private ImageView mimageView;
    private TextView  temperatureDisplay;
    private TextView  barometerDisplay;

    private static final float BAROMETER_RANGE_SUNNY=1010.f;
    private static final float BAROMETER_RANGE_RAINY=990.f;


    private static final int MSG_UPDATE_BAROMETER_UI=1;
    private static final int MSG_UPDATE_TEMPERATURE=2;
    private static final int MSG_UPDATE_BAROMETER=3;

    //适用于十进制数字的格式化
    private static final DecimalFormat DECIMAL_FORMAT=new DecimalFormat("0.0");

    private final Handler mHandler=new Handler(){

        private int  mBarometerImage=-1;

        public void handleMessage(Message msg){
            switch (msg.what){
                case MSG_UPDATE_BAROMETER_UI:
                    int img;
                    if (mLastPressure > BAROMETER_RANGE_SUNNY){
                        img=R.drawable.ic_sunny;
                    }else if(mLastPressure < BAROMETER_RANGE_RAINY){
                        img=R.drawable.ic_rainy;
                    }else {
                        img=R.drawable.ic_cloudy;
                    }
                    if(img != mBarometerImage){
                        mimageView.setImageResource(img);
                        mBarometerImage =img;
                    }
                    break;
                case MSG_UPDATE_TEMPERATURE:
                    temperatureDisplay.setText(DECIMAL_FORMAT.format(mLastTemperature));
                    break;
                case  MSG_UPDATE_BAROMETER:
                    barometerDisplay.setText(DECIMAL_FORMAT.format(mLastPressure*0.1));
                    break;
            }
        }
    };

    //当我们向系统的SensorManager注册BMP280传感器驱动程序时使用回调。
    private SensorManager.DynamicSensorCallback  mDynamicSensorCallback=
            new SensorManager.DynamicSensorCallback() {
                @Override
                public void onDynamicSensorConnected(Sensor sensor) {
                    if(sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){
                        //我们的传感器已连接。 开始接收温度数据。
                        mSensorManager.registerListener(mTemperatureListener,sensor,
                                SensorManager.SENSOR_DELAY_NORMAL);
                    }else if(sensor.getType() == Sensor.TYPE_PRESSURE) {
                        //我们的传感器已连接。 开始接收压力数据。
                        mSensorManager.registerListener(mPressureListener,sensor,
                                SensorManager.SENSOR_DELAY_NORMAL);
                    }
                }
                @Override
                public void  onDynamicSensorDisconnected(Sensor sensor){
                    super.onDynamicSensorDisconnected(sensor);
                }
            };

// Callback when SensorManager delivers temperature data.
    private SensorEventListener mTemperatureListener =new SensorEventListener(){
        @Override
        public void onSensorChanged(SensorEvent event) {
            mLastTemperature = event.values[0];
            Log.d(TAG, "温度值反馈: " + mLastTemperature+"℃");
            updateTemperatureDisplay(mLastTemperature);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "accuracy changed: " + accuracy);
        }
    };


    //当SensorManager传送压力数据时回调。
    private SensorEventListener mPressureListener = new SensorEventListener(){

        @Override
        public void onSensorChanged(SensorEvent event) {
            mLastPressure = event.values[0];

            Log.d(TAG,"温度值反馈： "+ mLastPressure +"℃");

            updateBarometerDisplay(mLastPressure);

            updateBarometer(mLastPressure);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
                  Log.d(TAG,"accuracy change" +
                          "d :  " +  accuracy);
        }
    };

    private void updateBarometerDisplay(float pressure) {
        // Update UI.
        if(!mHandler.hasMessages(MSG_UPDATE_BAROMETER)){
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BAROMETER,5000);
        }
    }



    private void updateTemperatureDisplay(float pressure) {
        // Update UI.
        if(!mHandler.hasMessages(MSG_UPDATE_TEMPERATURE)){
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TEMPERATURE,5000);
        }
    }

    private void updateBarometer(float pressure) {
        // Update UI.
        if(!mHandler.hasMessages(MSG_UPDATE_BAROMETER_UI)){
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BAROMETER_UI,5000);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mimageView=findViewById(R.id.ImageView);

        temperatureDisplay=findViewById(R.id.temperatureDisplay);
        barometerDisplay=findViewById(R.id.barometerDisplay);

        mSensorManager= (SensorManager) getSystemService(SENSOR_SERVICE);

        try {
            mEnvironmentalSensorDriver = new Bmx280SensorDriver(BoardSpec.getI2cBus());

            mSensorManager.registerDynamicSensorCallback(mDynamicSensorCallback);

            mEnvironmentalSensorDriver.registerTemperatureSensor();
            mEnvironmentalSensorDriver.registerPressureSensor();
            Log.d(TAG,"Initialized I2C BMP280");

        } catch (IOException e) {
            throw new RuntimeException("Error initializing BMP280",e);

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //清理传感器注册
        mSensorManager.unregisterListener(mTemperatureListener);
        mSensorManager.unregisterListener(mPressureListener);
        mSensorManager.unregisterDynamicSensorCallback(mDynamicSensorCallback);

        //清理外围设备。
        if(mEnvironmentalSensorDriver !=null){

            try {
                mEnvironmentalSensorDriver.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mEnvironmentalSensorDriver=null;
        }

    }
}
