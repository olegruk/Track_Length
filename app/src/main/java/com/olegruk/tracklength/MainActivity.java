package com.olegruk.tracklength;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.widget.TextView;
import android.graphics.Color;

public class MainActivity extends AppCompatActivity {

    private static String TAG = MainActivity.class.toString();

    private TextView isSvcConnected;
    private TextView isGPSConnected;
    private TextView isBLEConnected;
    private TextView distView;
    private TextView mySpeedView;
    private TextView countView;
    private TextView stateView;
    private TextView exceptionView;
    private Button StartButton;
    private Button StopButton;
    private Button SettingsButton;
    private Button RescanButton;
    private Button EnableButton;
    private Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Инициализируем переменные для работы с интерфейсом
        isSvcConnected = (TextView) findViewById(R.id.IsSvcConnected_View_ID);
        isGPSConnected = (TextView) findViewById(R.id.IsGPSConnected_View_ID);
        isBLEConnected = (TextView) findViewById(R.id.IsBLEConnected_View_ID);
        distView = (TextView) findViewById(R.id.TrackLength_ID);
        mySpeedView = (TextView) findViewById(R.id.MySpeed_ID);
        countView = (TextView) findViewById(R.id.Count_View_ID);
        stateView = (TextView) findViewById(R.id.State_View_ID);
        exceptionView  = (TextView) findViewById(R.id.Exception_View_ID);
        StartButton = (Button) findViewById(R.id.Start_Button_ID);
        StopButton = (Button) findViewById(R.id.Stop_Button_ID);
        SettingsButton = (Button) findViewById(R.id.Settings_Button_ID);
        RescanButton = (Button) findViewById(R.id.Rescan_Button_ID);
        EnableButton = (Button) findViewById(R.id.Enable_Button_ID);

        // Интент для работы с сервисом
        serviceIntent = new Intent(this, TrackLengthService.class);

        Log.d(TAG, String.format("onCreated %s",
                savedInstanceState == null ? " save is null" : "not null"));
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart.");
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        Log.d(TAG, "onRestart.");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Регистрируем ресивер для получения событий от сервиса
        registerReceiver(receiver, new IntentFilter(Constants.CHANNEL));

        // Запрашиваем состояние сервиса
        serviceIntent.putExtra(Constants.EXTENDED_DATA_REASON, "CHECK_STATE");
        startService(serviceIntent);

        serviceIntent.putExtra(Constants.EXTENDED_DATA_REASON, "UPDATE_DATA");
        startService(serviceIntent);

        Log.d(TAG, "onResume.");
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Отписываемся от событий сервиса
        unregisterReceiver(receiver);

        Log.d(TAG, "onPause.");
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy.");
    }

    public void onClick(View view) {

        if (view.getId()==R.id.Start_Button_ID) {

            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            final BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

            // Проверяем доступен ли Bluetooth на устройстве.
            // Если нет, кажем диалог для включения.
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }

            // Стартуем сервис
            serviceIntent.putExtra(Constants.EXTENDED_DATA_REASON, "START_SERVICE");
            startService(serviceIntent);

        } else if (view.getId()==R.id.Stop_Button_ID) {

            // Останавливаем сервис
            stopService(serviceIntent);

        } else if (view.getId()==R.id.Settings_Button_ID) {

            //Открываем активити с настройками
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (view.getId()==R.id.Rescan_Button_ID) {

            // Переподключаемся к браслету
            serviceIntent.putExtra(Constants.EXTENDED_DATA_REASON, "RECONNECT_BLE");
            startService(serviceIntent);
        } else if (view.getId()==R.id.Enable_Button_ID) {

            // Активируем кнопку на браслете
            serviceIntent.putExtra(Constants.EXTENDED_DATA_REASON, "ENABLE_BUTTON");
            startService(serviceIntent);
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String state = intent.getStringExtra(Constants.EXTENDED_DATA_STATE);
            String lastException = intent.getStringExtra(Constants.EXTENDED_DATA_EXCEPTION);
            boolean isUpdate = intent.getBooleanExtra(Constants.EXTENDED_DATA_UPDATE, false);

            switch (state) {

                case "IS_STATUS_000":

                    isSvcConnected.setBackgroundColor(0xffcc0000);
                    isSvcConnected.setText("Service not connected");
                    isGPSConnected.setBackgroundColor(0xffcc0000);
                    isGPSConnected.setText("GPS not connected");
                    isBLEConnected.setBackgroundColor(0xffcc0000);
                    isBLEConnected.setText("Bracelet not connected");

                    StartButton.setEnabled(true);
                    StopButton.setEnabled(false);
                    SettingsButton.setEnabled(false);
                    RescanButton.setEnabled(false);
                    EnableButton.setEnabled(false);

                    break;

                case "IS_STATUS_100":

                    isSvcConnected.setBackgroundColor(Color.GREEN);
                    isSvcConnected.setText("Service connected");
                    isGPSConnected.setBackgroundColor(0xffcc0000);
                    isGPSConnected.setText("GPS not connected");
                    isBLEConnected.setBackgroundColor(0xffcc0000);
                    isBLEConnected.setText("Bracelet not Connected");

                    StartButton.setEnabled(false);
                    StopButton.setEnabled(true);
                    SettingsButton.setEnabled(true);
                    RescanButton.setEnabled(true);
                    EnableButton.setEnabled(true);

                    break;

                case "IS_STATUS_110":

                    isSvcConnected.setBackgroundColor(Color.GREEN);
                    isSvcConnected.setText("Service connected");
                    isGPSConnected.setBackgroundColor(Color.GREEN);
                    isGPSConnected.setText("GPS connected");
                    isBLEConnected.setBackgroundColor(0xffcc0000);
                    isBLEConnected.setText("Bracelet not connected");

                    StartButton.setEnabled(false);
                    StopButton.setEnabled(true);
                    SettingsButton.setEnabled(true);
                    RescanButton.setEnabled(true);
                    EnableButton.setEnabled(true);

                    break;

                case "IS_STATUS_101":

                    isSvcConnected.setBackgroundColor(Color.GREEN);
                    isSvcConnected.setText("Service connected");
                    isGPSConnected.setBackgroundColor(0xffcc0000);
                    isGPSConnected.setText("GPS not connected");
                    isBLEConnected.setBackgroundColor(Color.GREEN);
                    isBLEConnected.setText("Bracelet connected");

                    StartButton.setEnabled(false);
                    StopButton.setEnabled(true);
                    SettingsButton.setEnabled(true);
                    RescanButton.setEnabled(true);
                    EnableButton.setEnabled(true);

                    break;

                case "IS_STATUS_111":

                    isSvcConnected.setBackgroundColor(Color.GREEN);
                    isSvcConnected.setText("Service connected");
                    isGPSConnected.setBackgroundColor(Color.GREEN);
                    isGPSConnected.setText("GPS connected");
                    isBLEConnected.setBackgroundColor(Color.GREEN);
                    isBLEConnected.setText("Bracelet connected");

                    StartButton.setEnabled(false);
                    StopButton.setEnabled(true);
                    SettingsButton.setEnabled(true);
                    RescanButton.setEnabled(true);
                    EnableButton.setEnabled(true);

                    break;
            }

            if (isUpdate) {
                double distance = intent.getDoubleExtra(Constants.EXTENDED_DATA_DISTANCE,0.0);
                double mySpeed = intent.getDoubleExtra(Constants.EXTENDED_DATA_MYSPEED,0);
                int count = intent.getIntExtra(Constants.EXTENDED_DATA_COUNT,0);

                distView.setText(String.format("Пройдено: %s ", distance));
                countView.setText(String.format("Обновлений местоположения: %s ", count));
                mySpeedView.setText(String.format("Условная скорость: %s ", mySpeed));

                Log.d(TAG, String.format("Received track length: %s", distance));
                Log.d(TAG, String.format("Received speed: %s", mySpeed));
                Log.d(TAG, String.format("Received updated: %s", count));

            }

            if (lastException != null) {
                exceptionView.setText(String.format("%s ", lastException));
            }

            stateView.setText(String.format("Статус: %s ", state));
            Log.d(TAG, String.format("Received status: %s", state));
        }
    };

}