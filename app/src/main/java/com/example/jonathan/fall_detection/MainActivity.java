//Referenced http://www.jcomputers.us/vol9/jcp0907-07.pdf for algorithm
package com.example.jonathan.fall_detection;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import br.com.sapereaude.maskedEditText.MaskedEditText;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private SensorManager sensorManager;
    private MaskedEditText editText1;
    private MaskedEditText editText2;
    private TextView contactOneTextView;
    private TextView contactTwoTextView;
    private Button button;
    private Button transButton;
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
    private static final int MY_PERMISSION_READ_CONTACTS = 33;
    private String locationProvider;
    private Criteria criteria;
    private TextView textView;
    private CardView card2;
    private CardView cardView;
    private Button back;
    private JSONArray contacts;
    private HashMap<String, String> contactsMap;
    private boolean freefall;
    private ContactsController contactsController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        //changed to network since that uses gps and network to get location plus plain gps didn't work at BYU
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        //Get the systems sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //get accel sensor and register it an check it every 2 ms at least
        if (sensorManager != null) {
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
        fallSound = new MediaPlayer();
        fallSound.setAudioStreamType(AudioManager.STREAM_ALARM);
        AssetFileDescriptor afd = getBaseContext().getResources().openRawResourceFd(R.raw.fall);
        if (afd != null) {
            try {
                fallSound.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(), afd.getLength());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                afd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            fallSound.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        contactsController = new ContactsController(this);

        //TODO: maybe we should check permissions first, then analyze past call data to auto-fill two suggested contact numbers
        SharedPreferences sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefEditor = sharedPreferences.edit();

        cardView = (CardView) findViewById(R.id.card);
        editText1 = (MaskedEditText) findViewById(R.id.editText1);
        editText2 = (MaskedEditText) findViewById(R.id.editText2);
        contactOneTextView = (TextView) findViewById(R.id.contactOne);
        contactTwoTextView = (TextView) findViewById(R.id.contactTwo);
        textView = (TextView) findViewById(R.id.textView);
        card2 = (CardView) findViewById(R.id.card2);
        button = (Button) findViewById(R.id.saveButton);
        transButton = (Button) findViewById(R.id.transButton);
        back = (Button) findViewById(R.id.backButton);

        editText1.setOnFocusChangeListener(focusLister);
        editText2.setOnFocusChangeListener(focusLister);

        editText1.addTextChangedListener(numbersTextWatcher);
        editText2.addTextChangedListener(numbersTextWatcher);

        transButton.setOnClickListener(v ->
                Toast.makeText(MainActivity.this,
                        "Please enter two different phone numbers", Toast.LENGTH_SHORT).show());

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
        }
        else {
            checkSMSPermission();
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationProvider = locationManager.getBestProvider(criteria, true);
            locationManager.requestLocationUpdates(locationProvider, 0, 10, this);
            mCurrentLocation = locationManager.getLastKnownLocation(locationProvider);
        }
        else {
            checkLocationPermission();
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            //Gets a mapping of contact numbers to number of times the number has been contacted
            contacts = contactsController.getCurrentContacts();
            contactsMap = contactsController.getContactsMap();
            try {
                String phoneNumber1 = "";
                String phoneNumber2 = "";
                String contact1 = "";
                String contact2 = "";
                if (contacts.length() > 0) {
                    phoneNumber1 = PhoneNumberUtils.stripSeparators(contacts.getJSONObject(0).getString("phone_number"));
                    contact1 = contacts.getJSONObject(0).getString("name");
                    if (contacts.length() > 1) {
                        phoneNumber2 = PhoneNumberUtils.stripSeparators(contacts.getJSONObject(1).getString("phone_number"));
                        contact2 = contacts.getJSONObject(1).getString("name");
                    }
                }
                editText1.setText(phoneNumber1);
                contactOneTextView.setText(contact1);
                editText2.setText(phoneNumber2);
                contactTwoTextView.setText(contact2);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            checkContactsPermission();
        }

        button.setOnClickListener(view -> {
            String a1 = editText1.getText().toString().trim();
            String a2 = editText2.getText().toString().trim();
            Log.d("DEBUG", "1:" + a1 + " 2:" + a2);
            sharedPrefEditor.putString("contactNumber1", editText1.getText().toString());
            sharedPrefEditor.putString("contactNumber2", editText2.getText().toString());
            if (sharedPrefEditor.commit())
                Toast.makeText(MainActivity.this,
                        "Contact numbers saved", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(MainActivity.this,
                        "Unable to save contact numbers", Toast.LENGTH_LONG).show();

            textView.setVisibility(TextView.GONE);
            cardView.setVisibility(CardView.GONE);
            card2.setVisibility(TextView.VISIBLE);
            back.setVisibility(Button.VISIBLE);
            button.setVisibility(Button.GONE);

        });

        back.setOnClickListener(v -> {
            textView.setVisibility(TextView.VISIBLE);
            cardView.setVisibility(CardView.VISIBLE);
            card2.setVisibility(TextView.GONE);
            back.setVisibility(Button.GONE);
            button.setVisibility(Button.VISIBLE);
        });
    }

    private View.OnFocusChangeListener focusLister = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (editText1.hasFocus()) {
                editText1.setCursorVisible(true);
            }
            else {
                editText1.setCursorVisible(false);
            }

            if (editText2.hasFocus()) {
                editText2.setCursorVisible(true);
            }
            else {
                editText2.setCursorVisible(false);
            }
        }
    };


    private TextWatcher numbersTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String phonenum1 = editText1.getRawText();
            String phonenum2 = editText2.getRawText();
            String contact1 = "Contact 1";
            String contact2 = "Contact 2";
            if (contactsMap.containsKey(phonenum1))
                contact1 = contactsMap.get(phonenum1);
            if (contactsMap.containsKey(phonenum2))
                contact2 = contactsMap.get(phonenum2);
            contactOneTextView.setText(contact1);
            contactTwoTextView.setText(contact2);
            //if both have text 10 length long in them then it will be true && true and set enabled will be true
            button.setEnabled((phonenum1.length() == 10) && (phonenum2.length() == 10) && !(phonenum1.equals(phonenum2)));
            transButton.setEnabled(!(phonenum1.length() == 10) || !(phonenum2.length() == 10) || (phonenum1.equals(phonenum2)));
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };


    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor _sensor = event.sensor;
        double rootSquare = 0.0;
        if (_sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            index++;
            //call protects against buffer overflow
            if (index >= sensorValuesSize - 1) {
                index = 0; //wrap index back around
                fallDetected = false;
                freefall = false;
            }
            //get acceleration minus GX on the x-axis
            accelValuesX[index] = event.values[0];
            //get acceleration minus GY on the y-axis
            accelValuesY[index] = event.values[1];
            //get acceleration minus GZ on the z-axis
            accelValuesZ[index] = event.values[2];

            rootSquare = Math.sqrt(Math.pow(accelValuesX[index], 2) + Math.pow(accelValuesY[index], 2) +
                    Math.pow(accelValuesZ[index], 2));

            if (rootSquare < 1.5) { //person free falling
                freefall = true;
            }
            if (freefall && rootSquare > 3.0 && !fallDetected) { //person hit the ground
                freefall = false;
                fallDetected = true;
            }
            if (fallDetected) {
                fallDetected = false;
                fallSound.start();
                sensorManager.unregisterListener(this);
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
                    sms();
                }
                speechDetected = false;
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
                    sms();
                }
                if (matches.contains("call for help")) {
                    speechDetected = true;
                    sms();
                }
                if (matches.contains("help")) {
                    speechDetected = true;
                    sms();
                }
                if (matches.contains("no")) {
                    fallDetected = false;
                    speechDetected = true;
                }
                if (matches.contains("i'm all right")) {
                    fallDetected = false;
                    speechDetected = true;
                }
                if (matches.contains("i'm okay")) {
                    fallDetected = false;
                    speechDetected = true;
                }
                if (matches.contains("i'm fine")) {
                    fallDetected = false;
                    speechDetected = true;
                }
            }
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL); //spin up new listener for a new fall
        }
    }

    public void sms() {
        double lat = mCurrentLocation.getLatitude();
        double lon = mCurrentLocation.getLongitude();
        String message = "Help I have fallen at the location \n " +
                "https://maps.google.com/maps?q=" + lat + "," + lon;
//TODO: add all numbers to an array to go through and text them all
        String number1 = editText1.getText().toString();
        String number2 = editText2.getText().toString();
        SmsManager smsManager = SmsManager.getDefault();
        StringBuffer smsBody = new StringBuffer();
        smsBody.append(Uri.parse(message));
        smsManager.sendTextMessage(number1, null, smsBody.toString(), null, null);
        smsManager.sendTextMessage(number2, null, smsBody.toString(), null, null);
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

    private void checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    MY_PERMISSION_READ_CONTACTS);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        locationProvider = locationManager.getBestProvider(criteria, true);
                        locationManager.requestLocationUpdates(locationProvider, 0, 10, this);
                        mCurrentLocation = locationManager.getLastKnownLocation(locationProvider);
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
                    }
                }
                else {
                    //permission denied
                    checkSMSPermissionWithMessage();
                }
                break;
            }
            case MY_PERMISSION_READ_CONTACTS:
            {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                            == PackageManager.PERMISSION_GRANTED) {
                        contacts = contactsController.getCurrentContacts();
                        try {
                            editText1.setText(contacts.length() > 0 ? contacts.getJSONObject(0).getString("phone_number") : "");
                            editText2.setText(contacts.length() > 1 ? contacts.getJSONObject(1).getString("phone_number") : "");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    checkContactsPermissionWithMessage(); //permission denied
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
                    .setPositiveButton(R.string.ok, (dialog, i) ->
                            ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_LOCATION))
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
                    .setPositiveButton(R.string.ok, (dialog, i) ->
                            ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.SEND_SMS},
                            MY_PERMISSIONS_REQUEST_SEND_SMS))
                    .create()
                    .show();
        }
        else {
            checkSMSPermissionWithMessage(); //denied again
        }
    }

    private void checkContactsPermissionWithMessage() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_CONTACTS)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_contacts_permission)
                    .setMessage(R.string.text_contacts_permission)
                    .setPositiveButton(R.string.ok, (dialog, i) ->
                            ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_CONTACTS},
                            MY_PERMISSION_READ_CONTACTS))
                    .create()
                    .show();
        }
        else {
            checkContactsPermissionWithMessage(); //denied again
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
