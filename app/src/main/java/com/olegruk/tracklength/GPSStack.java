package com.olegruk.tracklength;

import android.location.Location;
import android.util.Log;

class GPSStack {

    private static String TAG = GPSStack.class.toString();

    private double LatitudeStack[] = new double[] {0,0,0,0,0,0,0,0,0,0};
    private double LongitudeStack[] = new double[] {0,0,0,0,0,0,0,0,0,0};

    GPSStack() {

    }

    void AddNew(Location location) {

        Log.w(TAG, "Stack update...");

        for (int i = 9; i > 0; i--) {
            LatitudeStack[i] = LatitudeStack[i-1];
            LongitudeStack[i] = LongitudeStack[i-1];
            Log.d(TAG, String.format("LatitudeStack[%d]: %s", i, LatitudeStack[i]));
            Log.d(TAG, String.format("LongitudeStack[%d]: %s", i, LongitudeStack[i]));
        }

        LatitudeStack[0] = location.getLatitude();
        LongitudeStack[0] = location.getLongitude();
        Log.d(TAG, String.format("LatitudeStack[%d]: %s", 0, LatitudeStack[0]));
        Log.d(TAG, String.format("LongitudeStack[%d]: %s", 0, LongitudeStack[0]));
    }

    private double CalcMid(double[] stack) {

        double sum = 0;
        int ii = 0;

        for (int i = 0; i < 10; i++)
            if (stack[i] > 0) {
                sum = sum + stack[i];
                ii=i+1;
            }
        Log.d(TAG, String.format("Calculated for %s points", ii));
        return sum/ii;
    }

    boolean IsFar(Location location) {
        Location midLocation = new Location("");
        double midlat = CalcMid(LatitudeStack);
        double midlong = CalcMid(LongitudeStack);

        midLocation.setLatitude(midlat);
        midLocation.setLongitude(midlong);
        Log.d(TAG, String.format("Middle LatitudeStack: %s", midlat));
        Log.d(TAG, String.format("Middle LongitudeStack: %s", midlong));

        double dev = location.distanceTo(midLocation);
        Log.d(TAG, String.format("Deviation: %s", dev));

        return (dev > Constants.STATIC_SHIFT);
    }

}