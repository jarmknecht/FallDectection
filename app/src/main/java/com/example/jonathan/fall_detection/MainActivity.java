package com.example.jonathan.fall_detection;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor senseAccel;
    static int sensorValuesSize = 70;
    float accelValuesX[] = new float[sensorValuesSize];
    float accelValuesY[] = new float[sensorValuesSize];
    float accelValuesZ[] = new float[sensorValuesSize];
    int index = 0;
    int k = 0;
    boolean fallDetected = false;
    //TextView x_value;
    //TextView y_value;
    //TextView z_value;
    //public double ax, ay, az;
    //public double normalizedVal;
    //public int i = 0;
    /*static int BUFF_SIZE = 50;
    static public double[] window = new double[BUFF_SIZE];
    double sigma = 0.5;
    double theta = 10;
    double theta1 = 5;
    double theta2 = 2;
    public static String curr_state;
    public static String prev_state;*/
    // media play controls playback of a stream streaming the accelerometer
    // public MediaPlayer m1_fall,m2_sit,m3_stand,m4_walk;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get the systems sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //get accel sensor and register it an check it every 2 ms at least
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);

        //initialize();
        //x_value = (TextView) findViewById(R.id.xVal);
        //y_value = (TextView) findViewById(R.id.yVal);
        //z_value = (TextView) findViewById(R.id.zVal);

    }

    /*private void initialize() {
        for (int i = 0; i < BUFF_SIZE; i++) {
            window[i] = 0;
        }
        prev_state = "none";
        curr_state = "none";
    }*/

    @Override
    public void onSensorChanged(SensorEvent event) {
        double rootSquare = 0.0;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            index++;
            accelValuesX[index] = event.values[0];
            accelValuesY[index] = event.values[1];
            accelValuesZ[index] = event.values[2];

            rootSquare = Math.sqrt(Math.pow(accelValuesX[index], 2) + Math.pow(accelValuesY[index], 2) +
             Math.pow(accelValuesZ[index], 2));

            if (rootSquare < 2.0) {
                Toast.makeText(this, "Fall Detected", Toast.LENGTH_LONG).show();
                fallDetected = true;
            }
            /*AddData(ax, ay, az);
            posture_recognition(window, ay);
            SystemState(curr_state, prev_state);
            if (!prev_state.equalsIgnoreCase(curr_state)) {
                prev_state = curr_state;
            }*/
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*private void posture_recognition(double[] window2, double ay2) {
        int zrc = compute_zrc(window2);

        if (zrc == 0) {
            if (Math.abs(ay2) < theta1) {
                curr_state = "sitting";
            }
            else {
                curr_state = "standing";
            }
        }
        else {
            if (zrc > theta2) {
                curr_state = "walking";
            }
            else {
                curr_state = "none";
            }
        }
    }*/

   /* private int compute_zrc(double[] window2) {
        int count = 0;
        for (int i = 1; i <= BUFF_SIZE - 1; i++) {
            if ((window2[i] - theta) < sigma && (window2[i - 1] - theta) > sigma) {
                count = count + 1;
            }
        }
        return count;
    }*/

    /*private void SystemState(String curr_state1, String prev_state1) {
        if(!prev_state1.equalsIgnoreCase(curr_state1)) {
            //fall occured
            Toast.makeText(getApplicationContext(), "FALL", Toast.LENGTH_LONG);
        }
        if(curr_state1.equalsIgnoreCase("sitting")){
            //m2_sit.start();
        }
        if(curr_state1.equalsIgnoreCase("standing")){
           // m3_stand.start();
        }
        if(curr_state1.equalsIgnoreCase("walking")){
           // m4_walk.start();
        }
    }*/

    /*private void AddData(double ax, double ay, double az) {
        normalizedVal = Math.sqrt(ax*ax + ay*ay + az*az);
        for (int i = 0; i <= BUFF_SIZE - 2; i++) {
            window[i] = window[i + 1];
        }
        window[BUFF_SIZE - 1] = normalizedVal;
    }

    public void exit_app(View view) {
        finish();
    }*/

    //private void updateValues(SensorEvent event) {
     //   float[] values = event.values;

       // x_value.setText("Value of X: " + values[0]);
        //y_value.setText("Value of Y: " + values[1]);
        //z_value.setText("Value of Z: " + values[2]);
    //}
}
