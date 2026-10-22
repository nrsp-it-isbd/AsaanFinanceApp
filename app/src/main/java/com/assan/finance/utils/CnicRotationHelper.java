package com.assan.finance.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Helper to ensure CNIC images are properly oriented and straightened.
 * Handles EXIF rotation, ML Kit based upright detection, and manual 90-degree rotations.
 */
public class CnicRotationHelper {
    private static final String TAG = "CnicRotationHelper";

    public interface StraightenCallback {
        void onResult(Uri resultUri);
    }

    /**
     * Reads EXIF orientation from source Uri and returns a file Uri with the bitmap
     * correctly rotated according to EXIF metadata before cropping or processing.
     */
    public static Uri prepareForCrop(Context context, Uri sourceUri) {
        if (sourceUri == null || context == null) return sourceUri;
        try {
            int exifDegree = getExifRotationDegrees(context, sourceUri);
            if (exifDegree == 0) {
                return sourceUri;
            }
            Bitmap bitmap = loadBitmapWithExif(context, sourceUri);
            if (bitmap != null) {
                File tempFile = new File(context.getCacheDir(), "pre_crop_" + System.currentTimeMillis() + ".jpg");
                saveBitmapToFile(bitmap, tempFile);
                return Uri.fromFile(tempFile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in prepareForCrop", e);
        }
        return sourceUri;
    }

    /**
     * Loads a Bitmap from Uri, automatically applying EXIF orientation.
     */
    public static Bitmap loadBitmapWithExif(Context context, Uri uri) {
        if (context == null || uri == null) return null;
        try {
            int rotation = getExifRotationDegrees(context, uri);
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, opts);
            is.close();

            int maxDim = Math.max(opts.outWidth, opts.outHeight);
            int inSampleSize = 1;
            while (maxDim / inSampleSize > 2048) {
                inSampleSize *= 2;
            }

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = inSampleSize;

            is = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
            if (is != null) is.close();

            if (bitmap != null && rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotated != bitmap) {
                    bitmap.recycle();
                    bitmap = rotated;
                }
            }
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error loading bitmap with EXIF", e);
            return null;
        }
    }

    /**
     * Determines EXIF rotation degrees (0, 90, 180, 270) from a Uri.
     */
    public static int getExifRotationDegrees(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return 0;
            ExifInterface exif = new ExifInterface(is);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Rotates an image Uri by +90 degrees clockwise and saves it to a new file.
     */
    public static Uri rotateUriBy90(Context context, Uri imageUri) {
        if (context == null || imageUri == null) return imageUri;
        try {
            Bitmap bitmap = loadBitmapWithExif(context, imageUri);
            if (bitmap == null) return imageUri;

            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            File rotatedFile = new File(context.getCacheDir(), "rotated_" + System.currentTimeMillis() + ".jpg");
            saveBitmapToFile(rotated, rotatedFile);

            if (rotated != bitmap) {
                bitmap.recycle();
            }
            return Uri.fromFile(rotatedFile);
        } catch (Exception e) {
            Log.e(TAG, "Error rotating image by 90", e);
            return imageUri;
        }
    }

    /**
     * Straightens a CNIC image:
     * 1. A Pakistani CNIC is landscape (~1.58:1 ratio).
     * 2. If height > width, it MUST be rotated (90° or 270°).
     * 3. ML Kit Text Recognition evaluates candidate angles to find the upright text orientation.
     */
    public static void straightenCnic(Context context, Uri croppedUri, boolean isFront, StraightenCallback callback) {
        new Thread(() -> {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            try {
                Bitmap bitmap = loadBitmapWithExif(context, croppedUri);
                if (bitmap == null) {
                    mainHandler.post(() -> callback.onResult(croppedUri));
                    return;
                }

                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                boolean isVertical = height > width;

                int[] candidateAngles;
                if (isVertical) {
                    // Vertical image: default to 270° (upright landscape for portrait photos)
                    candidateAngles = new int[]{270, 90};
                } else {
                    // Landscape image: check if right-side up (0) or upside-down (180)
                    candidateAngles = new int[]{0, 180};
                }

                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                int bestAngle = candidateAngles[0];
                int maxScore = -1;

                for (int angle : candidateAngles) {
                    try {
                        InputImage inputImage = InputImage.fromBitmap(bitmap, angle);
                        com.google.android.gms.tasks.Task<Text> task = recognizer.process(inputImage);
                        Text visionText = com.google.android.gms.tasks.Tasks.await(task, 3500, java.util.concurrent.TimeUnit.MILLISECONDS);
                        int score = scoreCnicText(visionText != null ? visionText.getText() : "");
                        if (score > maxScore) {
                            maxScore = score;
                            bestAngle = angle;
                        }
                    } catch (Exception te) {
                        Log.w(TAG, "Text orientation check warning for angle " + angle + ": " + te.getMessage());
                    }
                }

                // If vertical and no text detected, default to 270 degrees to make landscape upright
                if (isVertical && maxScore <= 0) {
                    bestAngle = 270;
                } else if (!isVertical && maxScore <= 0) {
                    bestAngle = 0;
                }

                if (bestAngle == 0) {
                    mainHandler.post(() -> callback.onResult(croppedUri));
                    return;
                }

                Matrix matrix = new Matrix();
                matrix.postRotate(bestAngle);
                Bitmap straightened = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

                File straightenedFile = new File(context.getCacheDir(), "straightened_" + System.currentTimeMillis() + ".jpg");
                saveBitmapToFile(straightened, straightenedFile);

                if (straightened != bitmap) {
                    bitmap.recycle();
                }

                mainHandler.post(() -> callback.onResult(Uri.fromFile(straightenedFile)));
            } catch (Exception e) {
                Log.e(TAG, "Error in straightenCnic", e);
                mainHandler.post(() -> callback.onResult(croppedUri));
            }
        }).start();
    }

    private static int scoreCnicText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return 0;
        String text = rawText.toUpperCase(Locale.ENGLISH);
        int score = 0;

        // CNIC format (13 digits or 5-7-1 pattern)
        if (Pattern.compile("\\d{5}[-\\s]?\\d{7}[-\\s]?\\d").matcher(text).find()) {
            score += 50;
        } else if (Pattern.compile("\\d{11,13}").matcher(text).find()) {
            score += 30;
        }

        // Date format (DD.MM.YYYY or DD/MM/YYYY)
        if (Pattern.compile("\\d{2}[./-]\\d{2}[./-]\\d{4}").matcher(text).find()) {
            score += 30;
        }

        // Pakistani CNIC Keywords
        String[] keywords = {
                "PAKISTAN", "IDENTITY", "CARD", "NATIONAL", "NADRA",
                "NAME", "FATHER", "HUSBAND", "DATE OF BIRTH", "EXPIRY",
                "ISSUE", "GENDER", "COUNTRY", "REPUBLIC", "GOVERNMENT"
        };
        for (String kw : keywords) {
            if (text.contains(kw)) {
                score += 15;
            }
        }

        // Urdu characters
        for (char c : rawText.toCharArray()) {
            if (c >= 0x0600 && c <= 0x06FF) {
                score += 2;
            }
        }

        // Each line detected
        score += rawText.split("\n").length;

        return score;
    }

    private static void saveBitmapToFile(Bitmap bitmap, File file) throws Exception {
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
            out.flush();
        }
    }
}
