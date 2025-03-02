package com.dailydumbbell.plugin.camera;

import static android.Manifest.permission.CAMERA;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.getcapacitor.PermissionState;

import org.json.JSONArray;

@CapacitorPlugin(name = "CanvasCamera", permissions = { @Permission(strings = { CAMERA }, alias = CameraPlugin.CAMERA_PERMISSION_ALIAS) })
public class CameraPlugin extends Plugin implements CameraPreview.CameraPreviewListener {
    private static final String TAG = "CameraPlugin";
    static final String CAMERA_PERMISSION_ALIAS = "camera";
    private CameraPreview camera;
    private static final boolean LOGGING = true;
    private String renderCallbackId = "";
    private int defaultOrientation = -1;

    @PluginMethod
    public void initialize(PluginCall call) {
        camera = new CameraPreview(this.getActivity());
        camera.initialize(call.getData());
        camera.setEventListener(this);
        call.resolve();
    }

    @PluginMethod
    public void start(PluginCall call) {
        if (PermissionState.GRANTED.equals(getPermissionState(CAMERA_PERMISSION_ALIAS))) {
            startCamera(call);
        } else {
            requestPermissionForAlias(CAMERA_PERMISSION_ALIAS, call, "handleCameraPermissionResult");
        }
    }

    @SuppressLint("WrongConstant")
    @PluginMethod
    public void stop(PluginCall call) {
        if (defaultOrientation != -1) {
            getBridge().getActivity().setRequestedOrientation(defaultOrientation);
            defaultOrientation = -1;
        }
        releasePreviousCallback();
        camera.stop();
        call.resolve();
    }

    @PluginMethod
    public void setOrientationChange(PluginCall call) {
        String orientation = call.getString("value", "portrait");
        assert orientation != null;
        camera.setCameraOrientation(orientation.equals("portrait") ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE);
        call.resolve();
    }

    @PluginMethod
    public void flip(PluginCall call) {
        camera.flip();
        call.resolve();
    }

    @PluginMethod
    public void getSupportedFlashModes(PluginCall call) {
        if (!camera.mPreviewing) {
            call.reject("Camera is not running");
            return;
        }

        JSONArray flashModes = camera.getSupportedFlashModes();
        JSObject jsObject = new JSObject();
        jsObject.put("result", flashModes);
        call.resolve(jsObject);
    }

    @PluginMethod
    public void setFlashMode(PluginCall call) {
        boolean isFlashOn = call.getBoolean("value", false);
        camera.setFlashMode(isFlashOn);
        call.resolve();
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public void onRenderFrame(PluginCall call) {
        releasePreviousCallback();
        call.setKeepAlive(true);
        renderCallbackId = call.getCallbackId();
    }

    @PermissionCallback
    private void handleCameraPermissionResult(PluginCall call) {
        if (PermissionState.GRANTED.equals(getPermissionState(CAMERA_PERMISSION_ALIAS))) {
            startCamera(call);
        } else {
            debug("User denied camera permission: " + getPermissionState(CAMERA_PERMISSION_ALIAS).toString());
            call.reject("Permission failed: user denied access to camera.");
        }
    }

    private void releasePreviousCallback() {
        if (renderCallbackId != "") {
            try {
                bridge.getSavedCall(renderCallbackId).release(bridge);
            } catch (Exception e) {}
        }
    }

    @Override
    public void onFrameUpdate(JSObject frame) {
        if (camera.mPreviewing && renderCallbackId != "") {
            try {
                bridge.getSavedCall(renderCallbackId).resolve(frame);
            } catch (Exception e) {}
        }
    }


    private void startCamera(PluginCall call) {
        bridge
            .getActivity()
            .runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (defaultOrientation != -1) {
                        defaultOrientation = getBridge().getActivity().getRequestedOrientation();
                    }
                    getBridge().getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

                    boolean hasStarted = camera.start();

                    if (hasStarted) {
                        call.resolve();
                    } else {
                        call.reject("Could not start camera!");
                    }
                }
            });
    }

    private void debug(@NonNull String log) {
        Logger.debug(getLogTag(), log);
    }

    private void log(@NonNull String log) {
        Logger.info(getLogTag(), log);
    }
}
