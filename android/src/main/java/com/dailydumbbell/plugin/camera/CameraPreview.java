package com.dailydumbbell.plugin.camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Base64;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.getcapacitor.Bridge;
import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

public class CameraPreview {
    private static final String TAG = "CameraPreview";

    public interface CameraPreviewListener {
        void onFrameUpdate(JSObject frame);
    }

    private CameraPreviewListener eventListener;
    private CameraUtil util = null;
    protected final String K_FPS_KEY = "fps";
    protected final String K_WIDTH_KEY = "width";
    protected final String K_HEIGHT_KEY = "height";
    protected final String K_CANVAS_KEY = "canvas";
    protected final String K_CAPTURE_KEY = "capture";
    protected final String K_FLASH_MODE_KEY = "flashMode";
    protected final String K_LENS_ORIENTATION_KEY = "cameraFacing";

    private static final int SEC_START_CAPTURE = 0;
    private static final int SEC_STOP_CAPTURE = 1;
    private static final int SEC_FLASH_MODE = 2;
    private static final int SEC_CAMERA_POSITION = 3;

    protected int mFps;
    protected int mWidth;
    protected int mHeight;
    private int mOrientation;
    protected int mCameraFacing;
    protected String mFlashMode;
    protected int mCanvasHeight;
    protected int mCanvasWidth;
    protected int mCaptureHeight;
    protected int mCaptureWidth;
    private int mDisplayOrientation = 0;
    private JSONArray mArgs;

    private Camera mCamera;
    private int mCameraId = 0;
    private int mPreviewFormat;
    private int[] mPreviewFpsRange;
    private String mPreviewFocusMode;
    private Camera.Size mPreviewSize;
    public boolean mPreviewing = false;

    private Activity mActivity = null;
    private TextureView mTextureView = null;
    private CameraHandlerThread mThread = null;
    private Thread renderThread = null;

    public CameraPreview(Activity activity) {
        mActivity = activity;
        util = new CameraUtil(mActivity);
        setDefaults();
        mOrientation = util.getCurrentOrientation();
    }

    public void setEventListener(CameraPreviewListener listener) {
        eventListener = listener;
    }

    public void initialize(JSONObject options) {
        try {
            parseOptions(options);
        } catch (Exception e) {
            error("Failed to parse options", e);
        }
    }

    public boolean start() {
        stop();
        removePreviewSurface();
        if (checkCameraHardware(mActivity)) {
            mPreviewing = true;
            log("Initializing preview surface...");
            return initPreviewSurface();
        } else {
            mPreviewing = false;
            warn("No camera detected !");
            return false;
        }
    }

    public void stop() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
                renderThread.stop();
                mThread.stop();
                log("Camera [" + mCameraId + "] stopped.");
                mCameraId = 0;
            } catch (Exception e) {
                error("Could not stop camera [" + mCameraId + "] : " + e.getMessage(), e);
            }
        }
        mPreviewing = false;
    }

    private void removePreviewSurface() {
        if (mTextureView != null) {
            try {
                ViewGroup parentViewGroup = (ViewGroup) mTextureView.getParent();
                if (parentViewGroup != null) {
                    parentViewGroup.removeView(mTextureView);
                }
                log("Camera preview surface removed.");
            } catch (Exception e) {
                warn("Could not remove view : " + e.getMessage());
            }
        }
    }

    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    private boolean initPreviewSurface() {
        if (mActivity != null) {
            mTextureView = new TextureView(mActivity);
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            WindowManager mW = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
            int screenWidth = mW.getDefaultDisplay().getWidth();
            int screenHeight = mW.getDefaultDisplay().getHeight();
            mActivity.addContentView(mTextureView, new ViewGroup.LayoutParams(screenWidth, screenHeight));
            log("Camera preview surface initialized.");
            return true;
        } else {
            warn("Could not initialize preview surface.");
            return false;
        }
    }

    private void setPreviewParameters() {
        if (mCamera != null) {
            // set display orientation
            mDisplayOrientation = util.getDisplayOrientation(mCameraId);
            Camera.Parameters parameters = mCamera.getParameters();
            // sets optimal preview size.
            mPreviewSize = util.getOptimalPreviewSize(parameters, mCaptureWidth, mCaptureHeight);
            if (mPreviewSize != null) {
                parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
                log("Preview size is set to w : " + mPreviewSize.width + ", h : " + mPreviewSize.height + ".");
            }
            // sets camera rotation
            int mCameraRotation = util.getCameraRotation(mCameraId);
            parameters.setRotation(mCameraRotation);
            // sets optimal preview fps range.
            mPreviewFpsRange = util.getOptimalFrameRate(parameters, mFps);
            if (mPreviewFpsRange != null) {
                parameters.setPreviewFpsRange(mPreviewFpsRange[0], mPreviewFpsRange[1]);
                log("Preview fps range is set to min : " + (mPreviewFpsRange[0] / 1000) + ", max : " + (mPreviewFpsRange[1] / 1000) + ".");
            }
            // sets optimal preview focus mode.
            mPreviewFocusMode = util.getOptimalFocusMode(parameters);
            if (mPreviewFocusMode != null) {
                parameters.setFocusMode(mPreviewFocusMode);
                log("Preview focus mode is set to : " + mPreviewFocusMode + ".");
            }
            // sets flash mode
            mFlashMode = util.getOptimalFlashMode(parameters, mFlashMode);
            if (mFlashMode != null) {
                parameters.setFlashMode(mFlashMode);
                log("Preview flash mode is set to : " + mFlashMode + ".");
            }
            // sets camera parameters
            mCamera.setParameters(parameters);
            // gets preview pixel format
            mPreviewFormat = parameters.getPreviewFormat();
        }
    }

    private Camera getCameraInstance() {
        Camera camera = null;

        try {
            int cameraId;
            int cameraCount = Camera.getNumberOfCameras();
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

            for (cameraId = 0; cameraId < cameraCount; cameraId++) {
                Camera.getCameraInfo(cameraId, cameraInfo);
                if (cameraInfo.facing == mCameraFacing) {
                    log("Trying to open camera : " + cameraId);
                    try {
                        mCameraId = cameraId;
                        //camera = Camera.open(cameraId);

                        if (mThread == null) {
                            mThread = new CameraHandlerThread();
                        }

                        synchronized (mThread) {
                            camera = mThread.openCamera(cameraId);
                        }

                        log("Camera [" + cameraId + "] opened.");
                        break;
                    } catch (RuntimeException e) {
                        error("Unable to open camera : " + e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            error("No available camera : " + e.getMessage(), e);
        }

        return camera;
    }

    private final Camera.PreviewCallback mCameraPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(final byte[] data, Camera camera) {
            // Every Frame is processed in separate threads
            // to prevent blocking of main thread to drop frames
            renderThread = new Thread(() -> {
                if (mPreviewing && data.length > 0) {
                    // Get display orientation.
                    int displayOrientation = util.getDisplayOrientation(mCameraId);

                    // Creating full size image.
                    byte[] bytes = util.getImageBytes(data, mPreviewSize.width, mPreviewSize.height, mPreviewFormat);
                    bytes = util.getResizedAndRotatedImage(bytes, mCanvasWidth, mCanvasHeight, displayOrientation, mCameraFacing);

                    // JSON output for full size image
                    JSObject frame = new JSObject();
                    String imageDataUri = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.DEFAULT);
                    frame.put("data", imageDataUri);
                    frame.put("width", mPreviewSize.width);
                    frame.put("height", mPreviewSize.height);
                    frame.put("timestamp", (new java.util.Date()).getTime());

                    // Send the frame to the JavaScript layer via an event listener
                    eventListener.onFrameUpdate(frame);
                }
            });
            renderThread.start();
        }
    };

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mCamera = getCameraInstance();

            if (mCamera != null) {

                mTextureView.setVisibility(View.INVISIBLE);
                mTextureView.setAlpha(0);

                try {
                    setPreviewParameters();

                    mCamera.setPreviewTexture(surface);
                    mCamera.setDisplayOrientation(mDisplayOrientation);
                    mCamera.setErrorCallback(mCameraErrorCallback);
                    mCamera.setPreviewCallback(mCameraPreviewCallback);

                    mCamera.startPreview();
                    mPreviewing = true;
                    log("Camera [" + mCameraId + "] started.");
                } catch (Exception e) {
                    mPreviewing = false;
                    error("Failed to init preview: " + e.getMessage(), e);
                    stop();
                }
            } else {
                mPreviewing = false;
                warn("Could not get camera instance.");
            }
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Ignored, Camera does all the work for us
        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            stop();
            return true;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // Invoked every time there's a new Camera preview frame
        }
    };

    private final Camera.ErrorCallback mCameraErrorCallback = new Camera.ErrorCallback() {
        @Override
        public void onError(int error, Camera camera) {
            switch (error) {
                case Camera.CAMERA_ERROR_EVICTED:
                    log("Camera was disconnected due to use by higher priority user. (error code - " + error + ")");
                    break;
                case Camera.CAMERA_ERROR_UNKNOWN:
                    log("Unspecified camera error. (error code - " + error + ")");
                    break;
                case Camera.CAMERA_ERROR_SERVER_DIED:
                    log("Media server died. In this case, the application must release the Camera object and instantiate a new one. (error code - " + error + ")");
                    break;
                default:
                    log("Camera error callback : (error code - " + error + ")");
                    break;
            }

            try {
                if (start()) {
                    log("Camera successfully restarted.");
                } else {
                    warn("Could not restart camera.");
                }
            } catch (Exception e) {
                error("Something happened while stopping camera", e);
            }
        }
    };

    private void warn(@NonNull String log) {
        Logger.warn(TAG, log);
    }

    private void log(@NonNull String log) {
        Logger.info(TAG, log);
    }

    private void error(@NonNull String log, @Nullable Throwable tr) {
        Logger.error(TAG, log, tr);
    }

    public void setDefaults() {
        mFps = 30;
        mWidth = 352;
        mHeight = 288;
        mCanvasWidth = 352;
        mCanvasHeight = 288;
        mCaptureWidth = 352;
        mCaptureHeight = 288;
        mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    private void parseOptions(JSONObject options) throws Exception {
        if (options == null) {
            return;
        }

        // flash mode
        if (options.has(K_FLASH_MODE_KEY)) {
            mFlashMode = util.getFlashMode(options.getBoolean(K_FLASH_MODE_KEY));
        }

        // lens orientation
        if (options.has(K_LENS_ORIENTATION_KEY)) {
            mCameraFacing = util.getCameraFacing(options.getString(K_LENS_ORIENTATION_KEY));
        }

        // fps
        if (options.has(K_FPS_KEY)) {
            mFps = options.getInt(K_FPS_KEY);
        }

        // width
        if (options.has(K_WIDTH_KEY)) {
            mWidth = mCaptureWidth = mCanvasWidth = options.getInt(K_WIDTH_KEY);
        }

        // height
        if (options.has(K_HEIGHT_KEY)) {
            mHeight = mCaptureHeight = mCanvasHeight = options.getInt(K_HEIGHT_KEY);
        }

        // canvas
        if (options.has(K_CANVAS_KEY)) {
            JSONObject canvas = options.getJSONObject(K_CANVAS_KEY);
            if (canvas.has(K_WIDTH_KEY)) {
                mCanvasWidth = canvas.getInt(K_WIDTH_KEY);
            }
            if (canvas.has(K_HEIGHT_KEY)) {
                mCanvasHeight = canvas.getInt(K_HEIGHT_KEY);
            }
        }

        // capture
        if (options.has(K_CAPTURE_KEY)) {
            JSONObject capture = options.getJSONObject(K_CAPTURE_KEY);
            // resolution.width
            if (capture.has(K_WIDTH_KEY)) {
                mCaptureWidth = capture.getInt(K_WIDTH_KEY);
            }
            // resolution.height
            if (capture.has(K_HEIGHT_KEY)) {
                mCaptureHeight = capture.getInt(K_HEIGHT_KEY);
            }
        }
    }

    public void setCameraOrientation(int orientation) {
        mOrientation = orientation;
        log("Orientation changed.");
        if (start()) {
            log("Camera successfully restarted.");
        } else {
            warn("Could not restart camera.");
        }
    }

    public void flip() {
        if (mCamera != null) {
            mCameraFacing = util.getCameraFacing(mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT ? "rear" : "front");

            if (start()) {
                log("Camera switched !");
            } else {
                warn("Could not switch camera. Could not start camera !");
            }
        } else {
            warn("Could not switch camera. No camera available !");
        }
    }

    public void setFlashMode(boolean isFlashModeOn) {
        if (mCamera != null) {
            mFlashMode = util.getFlashMode(isFlashModeOn);

            if (start()) {
                log("Flash mode applied !");
            } else {
                warn("Could not set flash mode. Could not start camera !");
            }
        } else {
            warn("Could not set flash mode. No camera available !");
        }
    }

    public JSONArray getSupportedFlashModes() {
        Camera.Parameters params = mCamera.getParameters();
        return util.getSupportedFlashModes(params);
    }
}
