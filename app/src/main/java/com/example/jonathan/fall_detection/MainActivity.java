//Referenced http://www.jcomputers.us/vol9/jcp0907-07.pdf for algorithm
package com.example.jonathan.fall_detection;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private TextView text;
    static int sensorValuesSize = 70;
    float accelValuesX[] = new float[sensorValuesSize];
    float accelValuesY[] = new float[sensorValuesSize];
    float accelValuesZ[] = new float[sensorValuesSize];
    int index = 0;
    boolean fallDetected = false;
    public MediaPlayer fallSound;
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    private boolean speechDetected = false;

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
        fallSound = MediaPlayer.create(this, R.raw.fall);
        fallSound.setVolume(10000, 10000);
    }
//TODO: when phone drops senses the fall but when I fall onto love sac it doesn't change parameters once everything else is working
    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor _sensor = event.sensor;
        fallDetected = false;
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
                fallSound.start();
                text.setText("FALL DETECTED!");
                (new Handler()).postDelayed(this::startVoiceRecognitionActivity, 1000);
                //startVoiceRecognitionActivity();
            }
            /*else {
                text.setText("Fall not Detected");
            }*/
        }
    }

    public void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening...");
        new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                if (!speechDetected) {
                    Toast.makeText(getBaseContext(), "time for response ran out SMS sent", Toast.LENGTH_SHORT).show();
                    //finishActivity(VOICE_RECOGNITION_REQUEST_CODE);
                    //call sms function
                }
            }
        }.start();
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }
//TODO: add so case doesn't matter with the words received
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (matches.contains("yes")) {
                speechDetected = true;
                text.setText("User said yes!");
                //call sms function
            }
            if (matches.contains("call for help")) {
                speechDetected = true;
                text.setText("User said yes!");
                //call sms function
            }
            if (matches.contains("help")) {
                speechDetected = true;
                text.setText("User said yes!");
                //call sms function
            }
            if (matches.contains("no")) {
                fallDetected = false;
                speechDetected = true;
                text.setText("User is okay!");
            }
            if (matches.contains("I'm all right")) {
                fallDetected = false;
                speechDetected = true;
                text.setText("User is okay!");
            }
            if (matches.contains("I'm okay")) {
                fallDetected = false;
                speechDetected = true;
                text.setText("User is okay!");
            }
            if (matches.contains("I'm fine")) {
                fallDetected = false;
                speechDetected = true;
                text.setText("User is okay!");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
