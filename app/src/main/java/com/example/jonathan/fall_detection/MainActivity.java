//Referenced http://www.jcomputers.us/vol9/jcp0907-07.pdf for algorithm
package com.example.jonathan.fall_detection;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.ListIterator;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private SensorManager sensorManager;
    //private TextView text;
    //private TextView lon;
    //private TextView lat;
    private EditText editText1;
    private EditText editText2;
    private Button button;
    static int sensorValuesSize = 70;
    float accelValuesX[] = new float[sensorValuesSize];
    float accelValuesY[] = new float[sensorValuesSize];
    float accelValuesZ[] = new float[sensorValuesSize];
    int index = 0;
    boolean fallDetected = false;
    public MediaPlayer fallSound;
    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    private boolean speechDetected = false;
    private Location mCurrentLocation;
    public LocationManager locationManager;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS= 50;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //text = (TextView) findViewById(R.id.Fall);
        //lon = (TextView) findViewById(R.id.lon);
        //lat = (TextView) findViewById(R.id.lat);
        //Get the systems sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //get accel sensor and register it an check it every 2 ms at least
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        fallSound = MediaPlayer.create(this, R.raw.fall);
        fallSound.setVolume(10000, 10000);
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);


        SharedPreferences sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefEditor = sharedPreferences.edit();
        String contactNumber1 = sharedPreferences.getString("contact_number_1", "");
        String contactNumber2 = sharedPreferences.getString("contact_number_2", "");
        Log.d("DEBUG", "1:" + contactNumber1 + " 2:" + contactNumber2);

        editText1 = (EditText) findViewById(R.id.editText1);
        editText2 = (EditText) findViewById(R.id.editText2);
        editText1.setText(contactNumber1);
        editText2.setText(contactNumber2);
        button = (Button) findViewById(R.id.saveButton);

        editText1.addTextChangedListener(numbersTextWatcher);
        editText2.addTextChangedListener(numbersTextWatcher);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String a1 = editText1.getText().toString();
                String a2 = editText2.getText().toString();
                Log.d("DEBUG", "1:" + a1 + " 2:" + a2);
                sharedPrefEditor.putString("contactNumber1", editText1.getText().toString());
                sharedPrefEditor.putString("contactNumber2", editText2.getText().toString());
                if (sharedPrefEditor.commit())
                    Toast.makeText(MainActivity.this, "Contact numbers saved", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(MainActivity.this, "Unable to save contact numbers", Toast.LENGTH_LONG).show();
            }
        });


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d("Message sms permission", "permitted");
        }
        else {
            checkSMSPermission();
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10, this);
            mCurrentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        else {
            checkLocationPermission();
        }
    }

    private TextWatcher numbersTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String phonenum1 = editText1.getText().toString().trim(); //trim() gets rid of spaces
            String phonenum2 = editText2.getText().toString().trim();
            //if both have text in them then it will be true && true and set enabled will be true
            button.setEnabled(!phonenum1.isEmpty() && !phonenum2.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };


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
                //text.setText("FALL DETECTED!");
                (new Handler()).postDelayed(this::startVoiceRecognitionActivity, 1000);
            }
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
                    //Toast.makeText(getBaseContext(), "time for response ran out SMS sent", Toast.LENGTH_SHORT).show();
                    //finishActivity(VOICE_RECOGNITION_REQUEST_CODE);
                    sms();
                    //call sms function
                }
            }
        }.start();
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                ListIterator<String> iter = matches.listIterator();
                while (iter.hasNext()) { //set everything to lowercase for checking purposes
                    iter.set(iter.next().toLowerCase());
                }
                if (matches.contains("yes")) {
                    speechDetected = true;
                    //text.setText("User said yes!");
                    //lon.setText(String.valueOf(mCurrentLocation.getLongitude()));
                    //lat.setText(String.valueOf(mCurrentLocation.getLatitude()));
                    sms();
                    //call sms function send location
                }
                if (matches.contains("call for help")) {
                    speechDetected = true;
                    //text.setText("User said yes!");
                    //lon.setText(String.valueOf(mCurrentLocation.getLongitude()));
                    //lat.setText(String.valueOf(mCurrentLocation.getLatitude()));
                    sms();
                    //call sms function send location
                }
                if (matches.contains("help")) {
                    speechDetected = true;
                    //text.setText("User said yes!");
                    //lon.setText(String.valueOf(mCurrentLocation.getLongitude()));
                    //lat.setText(String.valueOf(mCurrentLocation.getLatitude()));
                    sms();
                    //call sms function send location
                }
                if (matches.contains("no")) {
                    fallDetected = false;
                    speechDetected = true;
                    //text.setText("User is okay!");
                    //lon.setText("-");
                    //lat.setText("-");
                }
                if (matches.contains("i'm all right")) {
                    fallDetected = false;
                    speechDetected = true;
                    //text.setText("User is okay!");
                    //lon.setText("-");
                    //lat.setText("-");
                }
                if (matches.contains("i'm okay")) {
                    fallDetected = false;
                    speechDetected = true;
                    //text.setText("User is okay!");
                    //lon.setText("-");
                    //lat.setText("-");
                }
                if (matches.contains("i'm fine")) {
                    fallDetected = false;
                    speechDetected = true;
                    //text.setText("User is okay!");
                    //lon.setText("-");
                    //lat.setText("-");
                }
            }
        }
    }
//TODO: make it send a map link instead of a link thats why maps actvity was added may or may not need
    public void sms() {
        double lat = mCurrentLocation.getLatitude();
        double lon = mCurrentLocation.getLongitude();
        String message = "Help I have fallen at the location \n " +
                "https://maps.google.com/maps?q=" + lat + "," + lon;
        /*String message = "Help I have fallen at the location \n " +
                "http://maps.googleapis.com/maps/api/staticmap?center=" +
                lat + "," + lon + "&zoom=12&size=400x400&key=AIzaSyDSQE0n5ho4a_zYsp8GsJ09jDrTOXunDiI";
        /*StringBuilder message = new StringBuilder();
        //message.append("Help I have fallen at the location \n ");
        message.append("https://maps.googleapis.com/maps/api/staticmap?center=");
        message.append(lat);
        message.append(",");
        message.append(lon);
        message.append("&zoom=12&size=400x400&key=AIzaSyDSQE0n5ho4a_zYsp8GsJ09jDrTOXunDiI");
        String mess = message.toString();
        System.out.print(mess);*/
        /*Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra("sms_body", "Help I have fallen at the location");
        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(message));
        sendIntent.setType("image/png");
        startActivity(sendIntent);*/

        String number = "8015550001";
        SmsManager smsManager = SmsManager.getDefault();
        StringBuffer smsBody = new StringBuffer();
        smsBody.append(Uri.parse(message));
        smsManager.getDefault().sendTextMessage(number, null, smsBody.toString(), null, null);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
        }
    }

    private void checkSMSPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10, this);
                        mCurrentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                }
                else {
                    //permission denied
                    checkLocationPermissionWithMessage();
                }
                break;
            }
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                            == PackageManager.PERMISSION_GRANTED) {
                        Log.d("Message sms permission", "permitted");
                    }
                }
                else {
                    //permission denied
                    checkSMSPermissionWithMessage();
                }
                break;
            }
        }
    }

    private void checkLocationPermissionWithMessage() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_location_permission)
                    .setMessage(R.string.text_location_permission)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    MY_PERMISSIONS_REQUEST_LOCATION);
                        }
                    })
                    .create()
                    .show();
        }
        else {
            checkLocationPermissionWithMessage(); //denied again
        }
    }

    private void checkSMSPermissionWithMessage() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.SEND_SMS)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_sms_permission)
                    .setMessage(R.string.text_sms_permission)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.SEND_SMS},
                                    MY_PERMISSIONS_REQUEST_SEND_SMS);
                        }
                    })
                    .create()
                    .show();
        }
        else {
            checkSMSPermissionWithMessage(); //denied again
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
