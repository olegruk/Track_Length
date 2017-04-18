package com.olegruk.tracklength;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.util.Log;
import android.content.SharedPreferences;
import android.widget.Switch;


public class SettingsActivity extends AppCompatActivity {

    private static String TAG = SettingsActivity.class.toString();

    private EditText AccuracyData;
    private Switch AutosendCheck;
    private EditText PeriodData;
    private Switch CorrectionCheck;
    private EditText CorrectionData;
    private Switch InAccuracy;
    private EditText SimplifyData;
    private Switch IsSimplify;

    SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        AccuracyData = (EditText) findViewById(R.id.Accuracy_ID);
        AutosendCheck = (Switch) findViewById(R.id.Autosend_ID);
        PeriodData = (EditText) findViewById(R.id.Period_ID);
        CorrectionCheck = (Switch) findViewById(R.id.Is_Correction_ID);
        CorrectionData = (EditText) findViewById(R.id.Correction_ID);
        InAccuracy = (Switch) findViewById(R.id.In_Accuracy_ID);
        SimplifyData = (EditText) findViewById(R.id.Simplify_ID);
        IsSimplify = (Switch) findViewById(R.id.Is_Track_Simplify_ID);

        settings = getSharedPreferences("Preferences", MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume.");

        float accuracy = settings.getFloat("Accuracy",20);
        boolean autosend = settings.getBoolean("Autosend",false);
        float period = settings.getFloat("Period",500);
        boolean is_correction = settings.getBoolean("IsCorrection",false);
        float correction = settings.getFloat("Correction",0);
        boolean inAccuracy = settings.getBoolean("InAccuracy",false);
        float simplifyValue  = settings.getFloat("SimplifyValue",0);
        boolean isSimplify = settings.getBoolean("IsSimplify",false);

        AccuracyData.setText(String.format("%s ", accuracy));
        AutosendCheck.setChecked(autosend);
        PeriodData.setText(String.format("%s ", period));
        CorrectionCheck.setChecked(is_correction);
        CorrectionData.setText(String.format("%s ", correction));
        InAccuracy.setChecked(inAccuracy);
        SimplifyData.setText(String.format("%s ", simplifyValue));
        IsSimplify.setChecked(isSimplify);

        Log.d(TAG, String.format("Readed accuracy: %s", accuracy));
        Log.d(TAG, String.format("Readed autocheck: %s", autosend));
        Log.d(TAG, String.format("Readed period: %s", period));
        Log.d(TAG, String.format("Readed iscorrection: %s", is_correction));
        Log.d(TAG, String.format("Readed correction: %s", correction));
        Log.d(TAG, String.format("Readed isCorrection: %s", inAccuracy));
        Log.d(TAG, String.format("Readed SimplifyValue: %s", simplifyValue));
        Log.d(TAG, String.format("Readed IsSimplify: %s", isSimplify));
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "onPause.");

        SharedPreferences.Editor prefEditor = settings.edit();

        float accuracy = Float.valueOf(AccuracyData.getText().toString());
        boolean autosend = AutosendCheck.isChecked();
        float period = Float.valueOf(PeriodData.getText().toString());
        boolean is_correction = CorrectionCheck.isChecked();
        float correction = Float.valueOf(CorrectionData.getText().toString());
        boolean inAccuracy = InAccuracy.isChecked();
        boolean isSimplify = IsSimplify.isChecked();
        float simplifyValue = Float.valueOf(SimplifyData.getText().toString());

        prefEditor.putFloat("Accuracy",accuracy);
        prefEditor.putBoolean("Autosend",autosend);
        prefEditor.putFloat("Period",period);
        prefEditor.putBoolean("IsCorrection",is_correction);
        prefEditor.putFloat("Correction",correction);
        prefEditor.putBoolean("InAccuracy",inAccuracy);
        prefEditor.putBoolean("IsSimplify",isSimplify);
        prefEditor.putFloat("SimplifyValue",simplifyValue);

        prefEditor.apply();

        Log.d(TAG, String.format("Saved accuracy: %s", accuracy));
        Log.d(TAG, String.format("Saved autocheck: %s", autosend));
        Log.d(TAG, String.format("Saved period: %s", period));
        Log.d(TAG, String.format("Saved iscorrection: %s", is_correction));
        Log.d(TAG, String.format("Saved correction: %s", correction));
        Log.d(TAG, String.format("Saved inAccuracy: %s", inAccuracy));
        Log.d(TAG, String.format("Saved IsSimplify: %s", isSimplify));
        Log.d(TAG, String.format("Saved SimplifyValue: %s", simplifyValue));

        Intent serviceIntent = new Intent(this, TrackLengthService.class);
        serviceIntent.putExtra(Constants.EXTENDED_DATA_REASON, "UPDATE_SETTINGS");
        startService(serviceIntent);
    }

}