//Referenced http://www.jcomputers.us/vol9/jcp0907-07.pdf for algorithm
package com.example.jonathan.fall_detection;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private TextView text;
    static int sensorValuesSize = 70;
    float accelValuesX[] = new float[sensorValuesSize];
    float accelValuesY[] = new float[sensorValuesSize];
    float accelValuesZ[] = new float[sensorValuesSize];
    int index = 0;
    int k = 0;
    boolean fallDetected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.Fall);
        //Get the systems sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //get accel sensor and register it an check it every 2 ms at least
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor _sensor = event.sensor;
        double rootSquare = 0.0;
        if (_sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            index++;
            //call protects against buffer overflow
            if (index >= sensorValuesSize - 1) {
                index = 0; //wrap index back around
                sensorManager.unregisterListener(this);
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
            //get acceleration minus GX on the x-axis
            accelValuesX[index] = event.values[0];
            //get acceleration minus GY on the y-axis
            accelValuesY[index] = event.values[1];
            //get acceleration minus GZ on the z-axis
            accelValuesZ[index] = event.values[2];

            rootSquare = Math.sqrt(Math.pow(accelValuesX[index], 2) + Math.pow(accelValuesY[index], 2) +
                    Math.pow(accelValuesZ[index], 2));

            if (rootSquare < 2.0) {
                //Toast.makeText(this, "Fall Detected", Toast.LENGTH_LONG).show();
                fallDetected = true;
            }
            if (fallDetected) {
                text.setText("FALL DETECTED!");
            }
            else {
                text.setText("Fall not Detected");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
