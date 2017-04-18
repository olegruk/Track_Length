package com.olegruk.tracklength;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.app.Notification;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Date;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import android.os.Environment;
import java.text.SimpleDateFormat;


import static android.location.LocationProvider.AVAILABLE;
import static android.location.LocationProvider.OUT_OF_SERVICE;
import static android.location.LocationProvider.TEMPORARILY_UNAVAILABLE;


public class TrackLengthService extends Service {

    private static String TAG = TrackLengthService.class.toString();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mGatt;
    private BluetoothDevice mBluetoothDevice;
    private String mBluetoothDeviceAddress;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private BluetoothGattCharacteristic buttonCharacteristic;
    private BluetoothGattCharacteristic alertCharacteristic;
    private FileWriter trackLogWriter;
    private FileWriter debugTrackLogWriter;

    private Location oldlocation = null;
    private long lastTime;
    private double mySpeed;
    private float accuracy;
    private double distance;
    private double showDistance;
    private int count;
    private float period;
    private double limit;
    float correction;
    private boolean autosend;
    boolean is_correction;
    private boolean isFirst;
    private boolean inAccuracy;
    private String status;
    private float simplifyValue;
    private boolean isSimplify;

    GPSStack localStack = new GPSStack();

//    private String lastException = "";

    public TrackLengthService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        isFirst = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String reason = intent.getStringExtra(Constants.EXTENDED_DATA_REASON);

        if (reason == null) {
            sendException("Illegal service start.");
            Log.d(TAG, "Illegal service start. Service stopped.");
            reason = "";
            status = "IS_STATUS_000";
            sendStatus(status);
            stopSelf();
        }

        switch (reason) {

            case "CHECK_STATE":

                if(isFirst) {
                    status = "IS_STATUS_000";
                    sendStatus(status);
                    Log.d(TAG, "Status checked. Service stopped.");
                    stopSelf();
                } else {
                    sendStatus(status);
                    Log.d(TAG, "Status checked. Service is started.");
                }

                break;

            case "START_SERVICE":

                Init_Service();
                Log.d(TAG, "Service init and starting.");
                isFirst = false;

                break;

            case "RECONNECT_BLE":

                Init_BLE ();
                Log.w(TAG, "Reconnecting to BLE device.");

                break;

            case "ENABLE_BUTTON":

                enableButtonNotify();
                Log.w(TAG, "Bracelet button enabled.");

                break;

            case "UPDATE_DATA":

                sendResult();

                break;

            case "UPDATE_SETTINGS":

                if (isFirst) {
                    status = "IS_STATUS_000";
                    sendStatus(status);
                    Log.w(TAG, "Trying to update. Service stopped.");
                    stopSelf();
                } else {
                    SharedPreferences settings = getSharedPreferences("Preferences", MODE_PRIVATE);
                    if (settings != null) {
                        accuracy = settings.getFloat("Accuracy", 20);
                        autosend = settings.getBoolean("Autosend", false);
                        period = settings.getFloat("Period", 500);
                        is_correction = settings.getBoolean("IsCorrection", false);
                        correction = settings.getFloat("Correction", 0);
                        inAccuracy = settings.getBoolean("InAccuracy", false);
                        simplifyValue  = settings.getFloat("SimplifyValue",0);
                        isSimplify = settings.getBoolean("IsSimplify",false);

                        Log.d(TAG, String.format("Readed accuracy: %s", accuracy));
                        Log.d(TAG, String.format("Readed autocheck: %s", autosend));
                        Log.d(TAG, String.format("Readed period: %s", period));
                        Log.d(TAG, String.format("Readed iscorrection: %s", is_correction));
                        Log.d(TAG, String.format("Readed correction: %s", correction));
                        Log.d(TAG, String.format("Readed correction: %s", inAccuracy));
                        Log.d(TAG, String.format("Readed simplifyData: %s", simplifyValue));
                        Log.d(TAG, String.format("Readed IsSimplify: %s", isSimplify));
                        Log.w(TAG, "Parameters updated.");
                    } else
                        sendException("Unable to getSharedPreferences at UPDATE_SETTINGS");
                    break;
                }
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        if (!isFirst) {
            CloseGATT();
            locationManager.removeUpdates(locationListener);
            Close_Track_File();
            Close_Debug_Track_File();
            sendStatus("IS_STATUS_000");
            Log.d(TAG, "onDestroy. All closed");
        } else
            Log.d(TAG, "onDestroy. Nothing to do.");
    }

    private void Init_Service() {

        oldlocation = null;
        lastTime = 0;
        mySpeed = 0;
        distance = 0.0;
        showDistance = 0.0;
        count = 0;
        limit = 0;

        SharedPreferences settings = getSharedPreferences("Preferences", MODE_PRIVATE);
        if (settings != null) {
            accuracy = settings.getFloat("Accuracy", 20);
            autosend = settings.getBoolean("Autosend", false);
            period = settings.getFloat("Period", 500);
            is_correction = settings.getBoolean("IsCorrection", false);
            correction = settings.getFloat("Correction", 0);
            simplifyValue  = settings.getFloat("SimplifyValue",0);
            isSimplify = settings.getBoolean("IsSimplify",false);

        } else
            sendException("Unable to getSharedPreferences at Init_Service");

        SendServiceForeground();
        Init_GPS();
        Init_BLE();
        Init_Track_File();
        Init_Debug_Track_File();
        status = "IS_STATUS_100";
        sendStatus(status);
    }

    private void SendServiceForeground() {

        // Создаем уведомление и переводим сервис в foreground
        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 1, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle("Track length sending... ")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent);
        Notification notification = builder.build();
        startForeground(407, notification);
    }

    private void Init_GPS() {
        //Получаем ссылку на Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            sendException("Unable to create locationManager");

        // Определяем Listener, который реагирует на обновления местоположения
        locationListener = new LocationListener() {

            public void onLocationChanged(Location location) {
                SetStatus("GPS");
                if (oldlocation == null) {
                    if (IsLockLocation(location)) {
                        oldlocation = location;
                        SendDistanceToMiBand2("Lock");
                    }
                }
                if (oldlocation != null) {
                    count = count + 1;
                    if (isFineLocation(oldlocation, location)) {
                        showDistance = CalcDistanceToSend_V2(oldlocation, location);
                        Append_Track(location);
                        if (distance > limit) {
                            limit = limit + period;
                            if (autosend)
                                SendDistanceToMiBand2(String.format("%s", showDistance));
                        }
                        oldlocation = location;
                    }
                    sendResult();
                }
                Append_Debug_Track(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.w(TAG, String.format("onStatusChanged: %s", status));
                switch (status) {
                    case OUT_OF_SERVICE:
                        ClearStatus("GPS");
                        break;

                    case TEMPORARILY_UNAVAILABLE:
                        ClearStatus("GPS");
                        break;

                    case AVAILABLE:
                        SetStatus("GPS");
                        break;
                }
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TO_DO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        // Регистрируем Listener для Location Manager
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.LOCATION_UPDATE_PERIOD, Constants.LOCATION_UPDATE_DISTANCE, locationListener);
    }

    private boolean isFineLocation(Location oldlocation, Location location) {

        double displacement = location.distanceTo(oldlocation);
        double deltatime = location.getTime() + 1 - oldlocation.getTime(); //Дельта по времени в миллисекундах
        double speed = (displacement / deltatime) * 3600; //3600 - коэфф. перевода м/миллисек в км/час

        if (speed > Constants.SPEED_LIMIT)
            Log.d(TAG, "Jump!!!");

        mySpeed = new BigDecimal(speed).setScale(2, RoundingMode.UP).doubleValue();

        if (inAccuracy)
            return ((location.getAccuracy() < accuracy) &&
                    (speed < Constants.SPEED_LIMIT) &&
                    (displacement > location.getAccuracy() + oldlocation.getAccuracy()));
        else
            return ((location.getAccuracy() < accuracy) &&
                    (speed < Constants.SPEED_LIMIT));
    }

    private boolean IsLockLocation(Location location) {
        localStack.AddNew(location);
        return localStack.IsFar(location);
    }

    private double CalcDistanceToSend_V2(Location oldlocation, Location location) {
        distance = distance + location.distanceTo(oldlocation);
        double showDistance = new BigDecimal(distance/1000).setScale(2, RoundingMode.UP).doubleValue();
        if (is_correction)
            showDistance = showDistance + correction;
        return showDistance;
    }

    private void SendDistanceToMiBand2(final String dist) {
        byte[] distcode = dist.getBytes(Charset.forName("UTF-8"));
        byte[] fulldistcode;
        fulldistcode = new byte[(distcode.length + 2)];
        fulldistcode[0] = 5;
        fulldistcode[1] = 1;
        System.arraycopy(distcode, 0, fulldistcode, 2, distcode.length);
        if (alertCharacteristic != null) {
            alertCharacteristic.setValue(fulldistcode);
            mGatt.writeCharacteristic(alertCharacteristic);
        } else {
            Log.d(TAG, "The immediate service not found");
        }
     }

    private void Init_BLE () {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothDeviceAddress != null && mGatt != null) {
            SetStatus("BLE");
            sendStatus(status);
            return;
        }
        if (mBluetoothAdapter != null)
            scanLeDevice();
    }

    private void scanLeDevice() {
        // Прекращаем сканирование по истечении периода SCAN_PERIOD.
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                Log.w(TAG, "Stop scanning: Timeout.");
            }
        }, Constants.SCAN_PERIOD);
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    String address = device.getName() + "   " + device.getAddress();
                    Log.d(TAG, String.format("mLeScanCallBack, address %s", address));
                    if (address.contains(Constants.DEVICE_ADDRESS)) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        Log.w(TAG, "Stop scanning: device found.");
                        mBluetoothDevice = device;
                        if (connect(Constants.DEVICE_ADDRESS)) {
                            SetStatus("BLE");
                            sendStatus(status);
                            Log.d(TAG, "BLE CONNECTED");
                        } else
                            Log.d(TAG, "BLE NOT CONNECTED");
                    }
                }
    };

    private final BluetoothGattCallback mListener = new BluetoothGattCallback() {

        private void printCallBack(String aMethod, BluetoothGatt aGatt, int aStatus) {
            List<BluetoothGattService> services = aGatt.getServices();
            Log.d(TAG, String.format("%s size %s, status %d", aMethod, services.size(), aStatus));
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            printCallBack(String.format("onConnectionStateChange, new state %d", newState), gatt, status);
            switch (newState) {
                case BluetoothGatt.STATE_DISCONNECTED:
                    ClearStatus("BLE");
                    break;

                case BluetoothGatt.STATE_CONNECTED:
                    SetStatus("BLE");
                    break;
            }
            gatt.discoverServices();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            printCallBack("onServicesDiscovered", gatt, status);

            for (BluetoothGattService item : gatt.getServices()) {
                selectNotifications(item, gatt);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            printCallBack("onCharacteristicWrite", gatt, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            if (characteristic.getUuid().equals(Constants.UUID_CHARACTERISTIC_10_BUTTON)) {
                Log.d(TAG, "Button characteristic changed:");
                handleButtonPressed();
            } else {
                Log.d(TAG, "Unhandled characteristic changed:");
            }

        }

        private void handleButtonPressed() {
            Date date = new Date();
            long currentTime = date.getTime();
            long delta = currentTime - lastTime;
            if ((lastTime > 0) && (delta < Constants.CLICK_DELTA)) {
                Log.d(TAG, String.format("Button double-clicked!!! Delta: %d - %d = %d", currentTime, lastTime, delta));
                SendDistanceToMiBand2(String.format("%s", showDistance));
                lastTime = 0;
            } else {
                Log.d(TAG, "Button pressed once.");
                lastTime = currentTime;
            }
        }

        private void selectNotifications(BluetoothGattService aService, BluetoothGatt aBluetoothGatt) {
            for (BluetoothGattCharacteristic item : aService.getCharacteristics()) {
                if (item.getUuid().equals(Constants.UUID_CHARACTERISTIC_10_BUTTON)) {
                    buttonCharacteristic = item;
                    for (BluetoothGattDescriptor descriptor : item.getDescriptors()) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        aBluetoothGatt.writeDescriptor(descriptor);
                    }
                }
                if (item.getUuid().equals(Constants.UUID_CHARACTERISTIC_NEW_ALERT)) {
                    alertCharacteristic = item;
                    SendDistanceToMiBand2("Init");
                }

            }
        }

    };

    private void enableButtonNotify() {
        if (buttonCharacteristic != null) {
            BluetoothGattDescriptor notifyDescriptor = buttonCharacteristic.getDescriptors().get(0);
            notifyDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mGatt.writeDescriptor(notifyDescriptor);
            mGatt.setCharacteristicNotification(buttonCharacteristic, true);
            buttonCharacteristic.setValue(Constants.AUTH);
            mGatt.writeCharacteristic(buttonCharacteristic);
            Log.w(TAG, String.format("Enabled!!! %s", buttonCharacteristic.getUuid()));
        }
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.d(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Пытаемся переподключиться, если устройство уже подключено
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return (mGatt.connect());
        }
        mGatt = mBluetoothDevice.connectGatt(this, true, mListener);
        Log.d(TAG, "Trying to create a new connection.");
        if (mGatt != null) {
            mBluetoothDeviceAddress = address;
            Log.d(TAG, "New connection created.");
            return true;
        }
        return false;
    }

    public void CloseGATT() {
        // Освобождаем ресурсы по окончании использования BLE-device.
        if (mBluetoothAdapter == null || mGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mGatt.disconnect();
        if (mGatt == null)
            return;
        mGatt.close();
        mGatt = null;
        ClearStatus("BLE");
        sendStatus(status);
    }

    private void sendResult() {
        Intent sendIntent = new Intent(Constants.CHANNEL);
        sendIntent.putExtra(Constants.EXTENDED_DATA_UPDATE, true);
        sendIntent.putExtra(Constants.EXTENDED_DATA_STATE, status);
        sendIntent.putExtra(Constants.EXTENDED_DATA_DISTANCE, showDistance);
        sendIntent.putExtra(Constants.EXTENDED_DATA_MYSPEED, mySpeed);
        sendIntent.putExtra(Constants.EXTENDED_DATA_COUNT, count);
        sendBroadcast(sendIntent);
        Log.d(TAG, String.format("Sended track length: %s", showDistance));
        Log.d(TAG, String.format("Sended speed: %s", mySpeed));
        Log.d(TAG, String.format("Sended updated: %s", count));
        sendIntent.putExtra(Constants.EXTENDED_DATA_UPDATE, false);

    }

    private void sendStatus(String status) {
        Intent sendIntent = new Intent(Constants.CHANNEL);
        sendIntent.putExtra(Constants.EXTENDED_DATA_STATE, status);
        sendBroadcast(sendIntent);
        Log.d(TAG, String.format("Sended status: %s", status));
    }

    private void sendException(String anException) {
        Intent sendIntent = new Intent(Constants.CHANNEL);
        sendIntent.putExtra(Constants.EXTENDED_DATA_EXCEPTION, anException);
        sendBroadcast(sendIntent);
        Log.d(TAG, String.format("Sended exception: %s", anException));
    }

    private void Init_Track_File() {

        File trackLogName;
        File trackLogDir;
        String sdState = Environment.getExternalStorageState();
        Date date = new Date();
        SimpleDateFormat filenameFormat = new SimpleDateFormat("yyyy-MM-dd'_'HHmmss");
        String trackLogFileName = filenameFormat.format(date) + ".gpx";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String currentTime = dateFormat.format(date);

        if (sdState.equals(Environment.MEDIA_MOUNTED)) {
            trackLogDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/tracklength");
            trackLogName = new File(trackLogDir, trackLogFileName);
        } else {
            trackLogDir = new File(getCacheDir().getAbsolutePath() + "/tracklength");
            trackLogName = new File(trackLogDir, trackLogFileName);
            Log.w(TAG, "Unable to write to SD.");
        }
        if (!trackLogDir.exists()){
            trackLogDir.mkdirs();
        }

        try {
            trackLogWriter = new FileWriter(trackLogName);
            trackLogWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
            trackLogWriter.append('\n');
            trackLogWriter.write("<gpx");
            trackLogWriter.append('\n');
            trackLogWriter.write("xmlns=\"http://www.topografix.com/GPX/1/1\"");
            trackLogWriter.append('\n');
            trackLogWriter.write(" creator=\"OziExplorer Version 3956f - http://www.oziexplorer.com\"");
            trackLogWriter.append('\n');
            trackLogWriter.write("version=\"1.1\"");
            trackLogWriter.append('\n');
            trackLogWriter.write(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            trackLogWriter.append('\n');
            trackLogWriter.write("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">");
            trackLogWriter.append('\n');
            trackLogWriter.write("<metadata>");
            trackLogWriter.append('\n');
            trackLogWriter.write("<time>" + currentTime + "</time>");
            trackLogWriter.append('\n');
            trackLogWriter.write("<bounds minlat=\"55.708071\" minlon=\"36.297900\" maxlat=\"56.628150\" maxlon=\"37.645919\"/>");
            trackLogWriter.append('\n');
            trackLogWriter.write("</metadata>");
            trackLogWriter.append('\n');
            trackLogWriter.write("<trk>");
            trackLogWriter.append('\n');
            trackLogWriter.write("<name>" + trackLogFileName + "</name>");
            trackLogWriter.append('\n');
            trackLogWriter.write("<desc>" + trackLogFileName + "</desc>");
            trackLogWriter.append('\n');
            trackLogWriter.write("<trkseg>");
            trackLogWriter.append('\n');
            trackLogWriter.flush();
            Log.w(TAG, String.format("File written: %s", trackLogName.getAbsolutePath()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.w(TAG, "FileNotFoundException");

        } catch (IOException e) {
            e.printStackTrace();
            Log.w(TAG, "IOException");
        }
    }

    private void Append_Track(Location location) {

        String current_location = String.format("<trkpt lat=\"%s\" lon=\"%s\">", String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
        String current_altitude = String.format("<ele>%s</ele>", String.valueOf(location.getAltitude()));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String current_time = "<time>" + dateFormat.format(location.getTime()) + "</time>";
        String current_accuracy = String.format("<pdop>%s</pdop>", String.valueOf(location.getAccuracy()));
        try {
            trackLogWriter.write(current_location);
            trackLogWriter.append('\n');
            trackLogWriter.write(current_altitude);
            trackLogWriter.append('\n');
            trackLogWriter.write(current_time);
            trackLogWriter.append('\n');
            trackLogWriter.write(current_accuracy);
            trackLogWriter.append('\n');
            trackLogWriter.write("</trkpt>");
            trackLogWriter.append('\n');
            trackLogWriter.flush();
        }
        catch(IOException ex){

            Log.d(TAG, String.format("Ошибка записи в файл. %s", ex.getMessage()));
        }

    }

    private void Close_Track_File()  {
        try {
            trackLogWriter.write("</trkseg>");
            trackLogWriter.append('\n');
            trackLogWriter.write("</trk>");
            trackLogWriter.append('\n');
            trackLogWriter.write("</gpx>");
            trackLogWriter.append('\n');
            trackLogWriter.flush();
            trackLogWriter.close();
        }
        catch(IOException ex){

            Log.d(TAG, String.format("Ошибка записи в файл. %s", ex.getMessage()));
        }

    }

    private void Init_Debug_Track_File() {

        File trackLogName;
        File trackLogDir;
        String sdState = Environment.getExternalStorageState();
        Date date = new Date();
        SimpleDateFormat filenameFormat = new SimpleDateFormat("yyyy-MM-dd'_'HHmmss");
        String trackLogFileName = filenameFormat.format(date) + "---debug.gpx";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String currentTime = dateFormat.format(date);

        if (sdState.equals(Environment.MEDIA_MOUNTED)) {
            trackLogDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/tracklength");
            trackLogName = new File(trackLogDir, trackLogFileName);
        } else {
            trackLogDir = new File(getCacheDir().getAbsolutePath() + "/tracklength");
            trackLogName = new File(trackLogDir, trackLogFileName);
            Log.w(TAG, "Unable to write to SD.");
        }
        if (!trackLogDir.exists()){
            trackLogDir.mkdirs();
        }

        try {
            debugTrackLogWriter = new FileWriter(trackLogName);
            debugTrackLogWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("<gpx");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("xmlns=\"http://www.topografix.com/GPX/1/1\"");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write(" creator=\"OziExplorer Version 3956f - http://www.oziexplorer.com\"");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("version=\"1.1\"");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("<metadata>");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("<time>" + currentTime + "</time>");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("<bounds minlat=\"55.708071\" minlon=\"36.297900\" maxlat=\"56.628150\" maxlon=\"37.645919\"/>");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("</metadata>");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("<trk>");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("<name>" + trackLogFileName + "</name>");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("<desc>" + trackLogFileName + "</desc>");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("<trkseg>");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.flush();
            Log.w(TAG, String.format("File written: %s", trackLogName.getAbsolutePath()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.w(TAG, "FileNotFoundException");

        } catch (IOException e) {
            e.printStackTrace();
            Log.w(TAG, "IOException");
        }
    }

    private void Append_Debug_Track(Location location) {

        String current_location = String.format("<trkpt lat=\"%s\" lon=\"%s\">", String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
        String current_altitude = String.format("<ele>%s</ele>", String.valueOf(location.getAltitude()));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String current_time = "<time>" + dateFormat.format(location.getTime()) + "</time>";
        String current_accuracy = String.format("<pdop>%s</pdop>", String.valueOf(location.getAccuracy()));
        try {
            debugTrackLogWriter.write(current_location);
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write(current_altitude);
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write(current_time);
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write(current_accuracy);
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("</trkpt>");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.flush();
        }
        catch(IOException ex){

            Log.d(TAG, String.format("Ошибка записи в файл. %s", ex.getMessage()));
        }

    }

    private void Close_Debug_Track_File()  {
        try {
            debugTrackLogWriter.write("</trkseg>");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("</trk>");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.write("</gpx>");
            debugTrackLogWriter.append('\n');
            debugTrackLogWriter.flush();
            debugTrackLogWriter.close();
        }
        catch(IOException ex){

            Log.d(TAG, String.format("Ошибка записи в файл. %s", ex.getMessage()));
        }

    }

    private void SetStatus(String param) {
        switch (param) {
            case "GPS":
                if (status.equals("IS_STATUS_100"))
                    status = "IS_STATUS_110";
                else if (status.equals("IS_STATUS_101"))
                    status = "IS_STATUS_111";
                break;

            case "BLE":
                if (status.equals("IS_STATUS_100"))
                    status = "IS_STATUS_101";
                else if (status.equals("IS_STATUS_110"))
                    status = "IS_STATUS_111";
                break;
        }
    }

    private void ClearStatus(String param) {
        switch (param) {
            case "GPS":
                if (status.equals("IS_STATUS_110"))
                    status = "IS_STATUS_100";
                else if (status.equals("IS_STATUS_111"))
                    status = "IS_STATUS_101";
                break;

            case "BLE":
                if (status.equals("IS_STATUS_101"))
                    status = "IS_STATUS_100";
                else if (status.equals("IS_STATUS_111"))
                    status = "IS_STATUS_110";
                break;
        }
    }

}