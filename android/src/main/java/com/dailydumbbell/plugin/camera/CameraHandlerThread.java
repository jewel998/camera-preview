package com.dailydumbbell.plugin.camera;


import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;

import com.getcapacitor.Logger;

public class CameraHandlerThread extends HandlerThread {
    private static final String TAG = "CameraHandler";
    private Camera mCamera = null;
    private Handler mHandler = null;

    CameraHandlerThread() {
        super("CameraHandlerThread");
        start();
        mHandler = new Handler(getLooper());
    }

    private synchronized void cameraOpened() {
        notify();
    }

    public Camera openCamera(final int cameraId) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCamera = Camera.open(cameraId);
                    Logger.info(TAG, "Camera [" + cameraId + "] opened.");
                } catch (RuntimeException e) {
                    Logger.error(TAG, "Unable to open camera : " + e.getMessage(), e);
                }
                cameraOpened();
            }
        });

        try {
            wait();
        } catch (InterruptedException e) {
            Logger.warn(TAG, "Camera opening thread wait was interrupted : " + e.getMessage());
        }

        return mCamera;
    }
}
