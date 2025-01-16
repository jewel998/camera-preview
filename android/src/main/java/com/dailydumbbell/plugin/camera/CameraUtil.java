package com.dailydumbbell.plugin.camera;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.view.Surface;

import com.getcapacitor.JSObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class CameraUtil {
    private Activity mActivity = null;

    public CameraUtil(Activity activity) {
        mActivity = activity;
    }

    public int getCurrentOrientation() {
        if (mActivity != null) {
            return mActivity.getResources().getConfiguration().orientation;
        } else {
            return Configuration.ORIENTATION_UNDEFINED;
        }
    }

    public String getCurrentOrientationToString(int mOrientation) {
        switch (mOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                return "landscape";
            case Configuration.ORIENTATION_PORTRAIT:
                return "portrait";
            default:
                return "unknown";
        }
    }

    public Camera.Size getOptimalPreviewSize(Camera.Parameters parameters, int mCaptureWidth, int mCaptureHeight) {
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) mCaptureWidth / mCaptureHeight;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = mCaptureHeight;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public int getCameraRotation(int mCameraId) {
        int degrees = getDisplayRotation();

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int cameraRotationOffset = info.orientation;

        int cameraRotation;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            cameraRotation = (360 + cameraRotationOffset + degrees) % 360;
        } else {
            cameraRotation = (360 + cameraRotationOffset - degrees) % 360;
        }

        return cameraRotation;
    }

    public int getDisplayRotation() {
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; // Natural orientation.
            case Surface.ROTATION_90:
                degrees = 90;
                break; // Landscape left.
            case Surface.ROTATION_180:
                degrees = 180;
                break; // Upside down.
            case Surface.ROTATION_270:
                degrees = 270;
                break; // Landscape right.
            default:
                degrees = 0;
                break;
        }

        return degrees;
    }

    public int getDisplayOrientation(int mCameraId) {
        int degrees = getDisplayRotation();

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int cameraRotationOffset = info.orientation;

        int displayOrientation;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayOrientation = (cameraRotationOffset + degrees) % 360;
            displayOrientation = (360 - displayOrientation) % 360;
        } else {
            displayOrientation = (cameraRotationOffset - degrees + 360) % 360;
        }

        return displayOrientation;
    }

    public int[] getOptimalFrameRate(Camera.Parameters params, int mFps) {
        List<int[]> supportedRanges = params.getSupportedPreviewFpsRange();

        int[] optimalFpsRange = new int[]{30, 30};

        for (int[] range : supportedRanges) {
            optimalFpsRange = range;
            if (range[1] <= (mFps * 1000)) {
                break;
            }
        }

        return optimalFpsRange;
    }

    public String getOptimalFocusMode(Camera.Parameters params) {
        List<String> focusModes = params.getSupportedFocusModes();

        String result;

        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            result = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            result = Camera.Parameters.FOCUS_MODE_AUTO;
        } else {
            result = params.getSupportedFocusModes().get(0);
        }

        return result;
    }

    public String getOptimalFlashMode(Camera.Parameters parameters, String mFlashMode) {
        if (mFlashMode != null) {
            List<String> supportedFlashModes = parameters.getSupportedFlashModes();
            if (supportedFlashModes != null) {
                for (String str : supportedFlashModes) {
                    if (str.trim().contains(mFlashMode))
                        return mFlashMode;
                }
                return null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public byte[] getImageBytes(byte[] byteArray, int width, int height, int mPreviewFormat) {
        if (byteArray.length > 0) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // The second parameter is the actual image format
            YuvImage yuvImage = new YuvImage(byteArray, mPreviewFormat, width, height, null);
            // width and height define the size of the bitmap filled with the preview image
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
            // returns the jpeg as bytes array
            return out.toByteArray();
        } else {
            return byteArray;
        }
    }

    public byte[] getResizedAndRotatedImage(
            byte[] byteArray,
            int targetWidth,
            int targetHeight,
            int angle,
            int mCameraFacing
    ) {
        if (byteArray.length > 0) {
            // Sets bitmap factory options
            BitmapFactory.Options bOptions = new BitmapFactory.Options();
            // Set inJustDecodeBounds=true to check dimensions
            bOptions.inJustDecodeBounds = true;
            // Decode unscaled unrotated bitmap boundaries only
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, bOptions);

            if (targetWidth > 0 && targetHeight > 0) {
                // Calculate aspect ratio
                int[] widthHeight = calculateAspectRatio(bOptions.outWidth , bOptions.outHeight, targetWidth, targetHeight);

                int width = widthHeight[0];
                int height = widthHeight[1];

                bOptions.inSampleSize = 1;
                // Adjust inSampleSize
                if (bOptions.outHeight > height || bOptions.outWidth > width) {
                    final int halfOutHeight = bOptions.outHeight / 2;
                    final int halfOutWidth = bOptions.outWidth / 2;

                    while ((halfOutHeight / bOptions.inSampleSize) >= height
                            && (halfOutWidth / bOptions.inSampleSize) >= width) {
                        bOptions.inSampleSize *= 2;
                    }
                }
                // Set inJustDecodeBounds=false to get all pixels
                bOptions.inJustDecodeBounds = false;
                // Decode unscaled unrotated bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, bOptions);
                // Create scaled bitmap
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

                if (angle != 0 || mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    final Matrix matrix = new Matrix();

                    // Mirroring ?
                    if (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        matrix.preScale(-1.0f, 1.0f);
                    }
                    // Rotation ?
                    if (angle != 0) {
                        // Rotation
                        matrix.postRotate(angle);
                    }

                    // Create rotated bitmap
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
                }

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);

                // Recycling bitmap
                bitmap.recycle();

                return byteArrayOutputStream.toByteArray();
            } else {
                return byteArray;
            }
        } else {
            return byteArray;
        }
    }

    public int[] calculateAspectRatio(int origWidth, int origHeight, int targetWidth, int targetHeight) {
        int newWidth = targetWidth;
        int newHeight = targetHeight;

        // If no new width or height were specified return the original bitmap
        if (newWidth <= 0 && newHeight <= 0) {
            newWidth = origWidth;
            newHeight = origHeight;
        }
        // Only the width was specified
        else if (newWidth > 0 && newHeight <= 0) {
            newHeight = (int) (newWidth / (double) origWidth * origHeight);
        }
        // only the height was specified
        else if (newWidth <= 0 && newHeight > 0) {
            newWidth = (int) (newHeight / (double) origHeight * origWidth);
        }
        // If the user specified both a positive width and height
        // (potentially different aspect ratio) then the width or height is
        // scaled so that the image fits while maintaining aspect ratio.
        // Alternatively, the specified width and height could have been
        // kept and Bitmap.SCALE_TO_FIT specified when scaling, but this
        // would result in whitespace in the new image.
        else {
            double newRatio = newWidth / (double) newHeight;
            double origRatio = origWidth / (double) origHeight;

            if (origRatio > newRatio) {
                newHeight = (newWidth * origHeight) / origWidth;
            } else if (origRatio < newRatio) {
                newWidth = (newHeight * origWidth) / origHeight;
            }
        }

        int[] widthHeight = new int[2];
        widthHeight[0] = newWidth;
        widthHeight[1] = newHeight;

        return widthHeight;
    }

    public String getFlashMode(boolean isFlashModeOn) {
        if (isFlashModeOn) {
            return Camera.Parameters.FLASH_MODE_TORCH;
        } else {
            return Camera.Parameters.FLASH_MODE_OFF;
        }
    }

    public int getCameraFacing(String option) {
        if ("front".equals(option)) {
            return Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            return Camera.CameraInfo.CAMERA_FACING_BACK;
        }
    }

    public JSONArray getSupportedFlashModes(Camera.Parameters parameters) {
        List<String> supportedFlashModes;
        supportedFlashModes = parameters.getSupportedFlashModes();
        JSONArray jsonFlashModes = new JSONArray();

        if (supportedFlashModes != null) {
            for (int i = 0; i < supportedFlashModes.size(); i++) {
                jsonFlashModes.put(new String(supportedFlashModes.get(i)));
            }
        }

        return jsonFlashModes;
    }
}
