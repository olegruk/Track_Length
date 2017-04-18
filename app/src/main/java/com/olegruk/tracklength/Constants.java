package com.olegruk.tracklength;

import java.util.UUID;

class Constants {

    static final String CHANNEL =
            "com.olegruk.trackmessenger.TrackMessengerService.OUTCHANNEL";
    static final String EXTENDED_DATA_DISTANCE =
            "com.olegruk.trackmessenger.TrackMessengerService.DISTANCE";
    static final String EXTENDED_DATA_MYSPEED =
            "com.olegruk.trackmessenger.TrackMessengerService.MYSPEED";
    static final String EXTENDED_DATA_COUNT =
            "com.olegruk.trackmessenger.TrackMessengerService.COUNT";
    static final String EXTENDED_DATA_STATE =
            "com.olegruk.trackmessenger.TrackMessengerService.STATE";
    static final String EXTENDED_DATA_EXCEPTION =
            "com.olegruk.trackmessenger.TrackMessengerService.DIAG";
    static final String EXTENDED_DATA_UPDATE =
            "com.olegruk.trackmessenger.TrackMessengerService.UPDATE";
    static final String EXTENDED_DATA_REASON =
            "com.olegruk.trackmessenger.TrackMessengerService.REASON";

    static final UUID UUID_CHARACTERISTIC_NEW_ALERT =
            UUID.fromString("00002a46-0000-1000-8000-00805f9b34fb");
    static final UUID UUID_CHARACTERISTIC_AUTH =
            UUID.fromString("00000009-0000-3512-2118-0009af100700");
    static final UUID UUID_CHARACTERISTIC_10_BUTTON =
            UUID.fromString("00000010-0000-3512-2118-0009af100700");

    static byte[] AUTH =
            new byte[]{0x01, 0x08, 0x48, 0x49, 0x50, 0x51,
                    0x52, 0x53, 0x54, 0x55, 0x56, 0x57,
                    0x64, 0x65, 0x66, 0x67, 0x68, 0x69};

    static final long LOCATION_UPDATE_PERIOD = 0;
    static final float LOCATION_UPDATE_DISTANCE = 5;
    static final String DEVICE_ADDRESS = "ED:41:AA:97:A5:B0";
    static final long SCAN_PERIOD = 1000 * 10;
    static final long WAIT_FOR_SEND_PERIOD = 1000;
    static final long SPEED_LIMIT = 150; // 10 км/час
    static final long CLICK_DELTA = 1000;
    static final long STATIC_SHIFT = 15;

}