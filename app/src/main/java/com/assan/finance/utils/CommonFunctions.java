package com.assan.finance.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.airbnb.lottie.LottieAnimationView;
import com.assan.finance.R;
import com.assan.finance.apis.ApiManager;
import com.assan.finance.dataModels.DataModelGetLedger;
import com.assan.finance.managers.SignalRManager;
import com.assan.finance.sharedPreferences.SharedManager;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommonFunctions {
    @SuppressLint("StaticFieldLeak")
    public static View contentProgressBar;
    public static boolean doubleBackToExitPressedOnce = false;
    public static Handler mHandler = new Handler(android.os.Looper.getMainLooper());
    private static final int REQUEST_IMAGE_CODE_CAMERA = 101;
    private static final int REQUEST_IMAGE_CODE_GALLERY = 102;

    public static void showImageSourceDialog(final Activity activity) {
        final Dialog dialog = new Dialog(activity);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_image_source, null);
        dialog.setContentView(dialogView);

        View cameraImageView = dialog.findViewById(R.id.openCamera);
        View galleryImageView = dialog.findViewById(R.id.openGallery);
        View btnCancel = dialog.findViewById(R.id.btnCancelDialog);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        cameraImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImageFromCamera(activity, REQUEST_IMAGE_CODE_CAMERA);
                dialog.dismiss();
            }
        });

        galleryImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImageFromGallery(activity, REQUEST_IMAGE_CODE_GALLERY);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public static void showLogoutDialog(final Activity activity) {
        final Dialog dialog = new Dialog(activity);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_logout, null);
        dialog.setContentView(dialogView);
        dialog.setCancelable(false);

        TextView btnCancel = dialog.findViewById(R.id.btnCancel);
        TextView btnYes = dialog.findViewById(R.id.btnYes);

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            performLogout(activity);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
    
    /** Clears the session on this phone and returns to the start screen (logout and account deletion). */
    public static void performLogout(Activity activity) {
        SharedManager sharedManager = SharedManager.getInstance(activity);
        boolean isFirstTime = sharedManager.getBoolean(com.assan.finance.utils.Constants.isFirstTimeLaunch, true);
        sharedManager.clearPreferences();
        sharedManager.saveBoolean(com.assan.finance.utils.Constants.isFirstTimeLaunch, isFirstTime);

        // Stop this phone receiving the logged-out user's push notifications
        deletePushToken(activity);

        // Disconnect SignalR WebSocket
        SignalRManager.INSTANCE.disconnect();

        Intent intent = new Intent(activity, com.assan.finance.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static final String KEY_FCM_TOKEN_DELETE_PENDING = "FCM_TOKEN_DELETE_PENDING";

    /**
     * Deletes this phone's FCM token so the server can no longer push the logged-out user's
     * notifications here. The next login gets a fresh token that is saved only for that user.
     * Call after clearPreferences(). If it fails (e.g. offline) it is retried on the next login.
     */
    public static void deletePushToken(Context context) {
        final SharedManager sharedManager = SharedManager.getInstance(context.getApplicationContext());
        sharedManager.saveBoolean(KEY_FCM_TOKEN_DELETE_PENDING, true);
        com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        sharedManager.saveBoolean(KEY_FCM_TOKEN_DELETE_PENDING, false);
                        Log.d("FCM", "Push token deleted on logout");
                    } else {
                        Log.w("FCM", "Push token delete failed, will retry on next login", task.getException());
                    }
                });
    }

    public static void showQuotaExceededDialog(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        
        final Dialog dialog = new Dialog(activity);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_quota_exceeded, null);
        dialog.setContentView(dialogView);
        dialog.setCancelable(false);

        TextView btnOkay = dialog.findViewById(R.id.btnOkay);
        btnOkay.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    public static void showErrorDialog(final Activity activity, String message) {
        final Dialog dialog = new Dialog(activity);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_error, null);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);

        TextView txtErrorMessage = dialog.findViewById(R.id.txtErrorMessage);
        com.google.android.material.button.MaterialButton btnDismiss = dialog.findViewById(R.id.btnDismiss);

        txtErrorMessage.setText(message);

        btnDismiss.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public static void showSuccessDialog(final Activity activity, String message,
            View.OnClickListener onOkClickListener) {
        final Dialog dialog = new Dialog(activity);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_success, null);
        dialog.setContentView(dialogView);
        dialog.setCancelable(false);

        TextView txtMessage = dialog.findViewById(R.id.txtMessage);
        View okButton = dialog.findViewById(R.id.okButton);

        if (message != null && !message.isEmpty()) {
            txtMessage.setText(message);
        }

        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (onOkClickListener != null) {
                onOkClickListener.onClick(v);
            }
        });

        dialog.show();
    }

    @SuppressLint("QueryPermissionsNeeded")
    public static void captureImageFromCamera(Activity activity, int REQUEST_IMAGE_CODE_CAMERA) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.CAMERA}, 1001);
            android.widget.Toast.makeText(activity, activity.getString(R.string.str_camera_permission_required), android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CODE_CAMERA);
        }
    }

    // Open in Landscape Mode
    @SuppressLint("QueryPermissionsNeeded")
    public static void captureImageFromCameraForCNIC(Activity activity, int REQUEST_IMAGE_CODE_CAMERA) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.CAMERA}, 1001);
            android.widget.Toast.makeText(activity, activity.getString(R.string.str_camera_permission_required), android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            // Lock the orientation to landscape
            // activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CODE_CAMERA);
        }

    }

    public static void setCameraOrientation(String mCurrentPhotoPath) {
        try {
            ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            int rotate = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }
            Log.d("CameraHelper", "Image Orientation: " + orientation);
            Log.d("CameraHelper", "Rotation: " + rotate);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Picks one image without any storage/media permission (Google Play's personal-loan policy
     * forbids READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE). Android 13+ opens the system Photo
     * Picker, older versions the system file picker. Both return a content Uri in getData().
     */
    public static Intent createImagePickerIntent() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        intent.setType("image/*");
        return intent;
    }

    // Function to select an image from the gallery
    public static void selectImageFromGallery(Activity activity, int REQUEST_IMAGE_CODE_GALLERY) {
        activity.startActivityForResult(createImagePickerIntent(), REQUEST_IMAGE_CODE_GALLERY);
    }

    public static Uri getImageUri(Context context, Bitmap bitmap) {
        // Get the directory for storing temporary images
        File imagesDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "temp_images");
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }

        // Create a file to save the bitmap
        File imageFile = new File(imagesDir, "temp_image.jpg");
        try {
            // Write the bitmap data to the file
            OutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Uri.fromFile(imageFile);
    }

    public static Uri handleImageActivityResultProfileImage(int requestCode,
            int resultCode,
            Intent data,
            Activity activity,
            int REQUEST_IMAGE_CODE_CAMERA,
            int REQUEST_IMAGE_CODE_GALLERY,
            ImageView ivProfilePic) {
        if (resultCode == Activity.RESULT_OK) {
            Uri selectedImageUri = null;
            if (requestCode == REQUEST_IMAGE_CODE_CAMERA) {
                Bundle extras = data.getExtras();

                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    ivProfilePic.setImageBitmap(imageBitmap);
                    // selectedImageUri = data.getData();
                    selectedImageUri = getImageUri(activity, imageBitmap);
                }

                return selectedImageUri;
            } else if (requestCode == REQUEST_IMAGE_CODE_GALLERY) {
                selectedImageUri = data.getData();
                return selectedImageUri;
            }
        }
        return null;
    }

    /*
     * public static Uri handleImageActivityResultCNIC(int requestCode,
     * int resultCode,
     * Intent data,
     * Activity activity,
     * int REQUEST_IMAGE_CAPTURE_CNIC_FRONT,
     * int REQUEST_IMAGE_CAPTURE_CNIC_BACK
     * ) {
     * if (resultCode == Activity.RESULT_OK) {
     * if (requestCode == REQUEST_IMAGE_CAPTURE_CNIC_FRONT) {
     * Bundle extras = data.getExtras();
     * Bitmap imageBitmap = (Bitmap) extras.get("data");
     * 
     * } else if (requestCode == REQUEST_IMAGE_CAPTURE_CNIC_BACK) {
     * Uri selectedImageUri = data.getData();
     * return selectedImageUri;
     * }
     * }
     * return null;
     * }
     */

    private static Uri saveImageToGallery(Bitmap bitmap, Activity activity) {
        String savedImageURL = MediaStore.Images.Media.insertImage(
                activity.getContentResolver(),
                bitmap,
                "title",
                "description");
        return Uri.parse(savedImageURL);
    }

    public static void showButtonLoader(com.google.android.material.button.MaterialButton button, String loadingText) {
        button.setEnabled(false);
        button.setTag(R.id.btn_original_text, button.getText().toString());
        if (loadingText != null && !loadingText.isEmpty()) {
            button.setText(loadingText);
        }

        androidx.swiperefreshlayout.widget.CircularProgressDrawable progressDrawable = new androidx.swiperefreshlayout.widget.CircularProgressDrawable(
                button.getContext());
        progressDrawable.setStyle(androidx.swiperefreshlayout.widget.CircularProgressDrawable.DEFAULT);
        progressDrawable.setColorSchemeColors(
                androidx.core.content.ContextCompat.getColor(button.getContext(), android.R.color.white));
        progressDrawable.start();

        button.setIcon(progressDrawable);
        button.setIconGravity(com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START);
    }

    public static void hideButtonLoader(com.google.android.material.button.MaterialButton button) {
        button.setEnabled(true);
        String originalText = (String) button.getTag(R.id.btn_original_text);
        if (originalText != null) {
            button.setText(originalText);
        }
        button.setIcon(null);
    }

    public static void startLoadingAnimation(final TextView textView, final Handler handler) {
        handler.postDelayed(new Runnable() {
            private int dotCount = 0;

            @Override
            public void run() {
                dotCount++;
                String dots = new String(new char[dotCount % 4]).replace("\0", ".");
                textView.setText(textView.getContext().getString(R.string.msg_please_wait) + dots);
                handler.postDelayed(this, 500);
            }
        }, 500);
    }

    public static void showToast(Context context, String toastMsg) {
        Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show();
    }

    public static void navigateToActivity(Context context, Class<?> destinationActivity) {
        Intent intent = new Intent(context, destinationActivity);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    public static void navigateToActivityAndFinishPreviousActivity(Context context, Class<?> destinationActivity) {
        Intent intent = new Intent(context, destinationActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    public static void navigateToActivityPutIntent(Context context, Class<?> destinationActivity, Bundle extras) {
        Intent intent = new Intent(context, destinationActivity);
        if (extras != null) {
            intent.putExtras(extras);
        }
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    public static Bundle getExtrasFromIntent(Intent intent) {
        if (intent != null) {
            return intent.getExtras();
        }
        return null;
    }

    public static void navigateToActivityAndFinish(Context context, Class<?> destinationActivity) {
        Intent intent = new Intent(context, destinationActivity);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).finish();
            ((Activity) context).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    public static void onBackFinish(Context context) {
        if (context instanceof Activity) {
            ((Activity) context).finish();
            ((Activity) context).overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }

    }

    public static void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    public static int getScreenWidth(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    public static void showAlertDialog(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    public static boolean isPermissionGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    public static void shareContent(Context context, String subject, String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(Intent.createChooser(shareIntent, "Share using"));
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void openWebPage(Context context, String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(browserIntent);
    }

    public static int generateRandomNumber(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }

    public static boolean isNullOrEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static boolean isEditTextEmpty(AppCompatEditText editText) {
        String text = Objects.requireNonNull(editText.getText()).toString().trim();
        return text.isEmpty();
    }

    public static boolean isNullOrZero(Integer value) {
        return value == null || value == 0;
    }

    public static int getDeviceOrientation(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        return (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
                ? Configuration.ORIENTATION_PORTRAIT
                : Configuration.ORIENTATION_LANDSCAPE;
    }

    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public static <T> boolean isNullOrEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    public static <T> boolean isNullOrEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <K, V> boolean isNullOrEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNullOrWhiteSpace(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static int stringToInt(String text, int defaultValue) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static String intToString(int number) {
        return String.valueOf(number);
    }

    public static double stringToDouble(String text, double defaultValue) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static String doubleToString(double number) {
        return String.valueOf(number);
    }

    public static boolean stringToBoolean(String text, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(text);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static String booleanToString(boolean value) {
        return String.valueOf(value);
    }

    public static void fadeInView(View view, int duration) {
        if (view != null) {
            AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
            fadeIn.setDuration(duration);
            view.startAnimation(fadeIn);
            view.setVisibility(View.VISIBLE);
        }
    }

    public static void fadeOutView(View view, int duration) {
        if (view != null) {
            AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
            fadeOut.setDuration(duration);
            view.startAnimation(fadeOut);
            view.setVisibility(View.INVISIBLE);
        }
    }

    public static void slideUpView(View view, int duration) {
        if (view != null) {
            TranslateAnimation slideUp = new TranslateAnimation(0, 0, view.getHeight(), 0);
            slideUp.setDuration(duration);
            view.startAnimation(slideUp);
            view.setVisibility(View.VISIBLE);
        }
    }

    public static void slideDownView(View view, int duration) {
        if (view != null) {
            TranslateAnimation slideDown = new TranslateAnimation(0, 0, 0, view.getHeight());
            slideDown.setDuration(duration);
            view.startAnimation(slideDown);
            view.setVisibility(View.INVISIBLE);
        }
    }

    public static void showCustomDialog(Context context, String title, String message, View customView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setView(customView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Handle positive button click if needed
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Handle negative button click if needed
                    }
                })
                .show();
    }

    public static void showSnakeBar(View view, String message) {
        if (view != null) {
            Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(getColor(view.getContext(), R.color.snakeBarBackground));
            // snackbarView.setBackgroundColor(Color.DKGRAY);
            TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
            textView.setTextColor(Color.WHITE);
            snackbar.show();
        }
    }

    public static void showSnakeBarWithAction(View view, String message, String actionText,
            View.OnClickListener actionListener) {
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                    .setAction(actionText, actionListener)
                    .show();
        }
    }

    public static int getColor(Context context, int colorResId) {
        Resources resources = context.getResources();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return resources.getColor(colorResId, context.getTheme());
        }
        return colorResId;
    }

    public static int getColorFromHex(String hexColor) {
        return Color.parseColor(hexColor);
    }

    public static String split(String yourString) {
        String[] parts = yourString.split(",");
        String secondValue = parts.length > 1 ? parts[1] : ""; // If there is no second part, an empty string is
                                                               // assigned

        return secondValue;
    }

    public static int convertToInteger(String input) {
        try {
            // Attempt to parse the string to an integer
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            // Handle the case where the string is not a valid integer
            return 0; // You can choose an appropriate default value
        }
    }

    public static String convertToString(int number) {
        String str = number + "";

        return str;
    }

    public static String convertToString(double number) {

        return number + "";
    }

    public static void logErrorMessage(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }

    public static void logMessagePrint(String tag, String message) {
        Log.e(tag, message);
    }

    public static String generateUniqueID() {
        // Create a secure random number generator
        SecureRandom secureRandom = new SecureRandom();

        // Generate a random BigInteger
        BigInteger randomBigInteger = new BigInteger(128, secureRandom);

        // Convert the BigInteger to a unique string format
        String uniqueID = randomBigInteger.toString(32);

        // Ensure the string is of the desired length
        int targetLength = 32; // Adjust as needed
        while (uniqueID.length() < targetLength) {
            uniqueID = "0" + uniqueID;
        }

        Log.e("uniqueIDForVisit::", uniqueID);

        return uniqueID;
    }

    public static void addPhoneNumberFormatting(final EditText editText, Context context) {
        final TextWatcher phoneWatcher = new TextWatcher() {
            boolean isFormatting;
            boolean deletingHyphen;
            int hyphenStart;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Check if the user is deleting a hyphen
                if (count > 0 && after == 0 && s.charAt(start) == '-') {
                    deletingHyphen = true;
                    hyphenStart = start;
                } else {
                    deletingHyphen = false;
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isFormatting) {
                    isFormatting = true;

                    if (deletingHyphen) {
                        // Remove the hyphen and the digit before it
                        if (hyphenStart > 0 && s.length() > hyphenStart - 1) {
                            s.delete(hyphenStart - 1, hyphenStart);
                        }
                        deletingHyphen = false;
                    } else {
                        // Format phone number
                        formatPhoneNumber(s);
                    }

                    isFormatting = false;
                }
            }

            private void formatPhoneNumber(Editable s) {
                // Remove all non-digit characters
                String digits = s.toString().replaceAll("[^\\d]", "");

                // Insert dash after the first 4 characters if the length is greater than 4
                if (digits.length() > 4) {
                    StringBuilder formatted = new StringBuilder(digits);
                    formatted.insert(4, '-');
                    s.replace(0, s.length(), formatted.toString());
                } else {
                    s.replace(0, s.length(), digits);
                }
            }
        };

        editText.addTextChangedListener(phoneWatcher);
        editText.setTag(R.id.phone_watcher_tag, phoneWatcher); // Store the TextWatcher with a unique tag ID
    }

    private static void formatPhoneNumber(Editable s, EditText editText, Context context) {
        if (editText.getTag(R.id.phone_watcher_tag) instanceof TextWatcher) {
            TextWatcher phoneWatcher = (TextWatcher) editText.getTag(R.id.phone_watcher_tag);
            String phoneNumber = s.toString().replaceAll("\\D", "");

            StringBuilder formattedNumber = new StringBuilder();

            // Add dash after first 4 digits
            for (int i = 0; i < phoneNumber.length(); i++) {
                if (i == 4) {
                    formattedNumber.append("-");
                }
                formattedNumber.append(phoneNumber.charAt(i));
            }

            // Update the EditText with the formatted number
            editText.removeTextChangedListener(phoneWatcher);
            editText.setText(formattedNumber.toString());
            editText.setSelection(formattedNumber.length());
            editText.addTextChangedListener(phoneWatcher);
        }
    }

    public static void addCNICFormatting(final EditText editText) {
        final TextWatcher cnicWatcher = new TextWatcher() {
            boolean isFormatting;
            boolean deletingHyphen;
            int hyphenStart;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Check if the user is deleting a hyphen
                if (count > 0 && after == 0 && s.charAt(start) == '-') {
                    deletingHyphen = true;
                    hyphenStart = start;
                } else {
                    deletingHyphen = false;
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isFormatting) {
                    isFormatting = true;

                    if (deletingHyphen) {
                        // Remove the hyphen and the digit before it
                        if (hyphenStart > 0 && s.length() > hyphenStart - 1) {
                            s.delete(hyphenStart - 1, hyphenStart);
                        }
                        deletingHyphen = false;
                    } else {
                        // Format CNIC
                        formatCNIC(s);
                    }

                    isFormatting = false;
                }
            }

            private void formatCNIC(Editable s) {
                String digits = s.toString().replaceAll("[^\\d]", "");
                StringBuilder formatted = new StringBuilder(digits);

                if (digits.length() > 12) {
                    formatted.insert(5, '-');
                    formatted.insert(13, '-');
                } else if (digits.length() > 5) {
                    formatted.insert(5, '-');
                }

                s.replace(0, s.length(), formatted.toString());
            }
        };

        editText.addTextChangedListener(cnicWatcher);
        editText.setTag(R.id.cnic_watcher_tag, cnicWatcher); // Store the TextWatcher with a unique tag ID
    }

    private static void formatCNIC(Editable s, EditText editText) {
        if (editText.getTag(R.id.cnic_watcher_tag) instanceof TextWatcher) {
            TextWatcher cnicWatcher = (TextWatcher) editText.getTag(R.id.cnic_watcher_tag);
            String cnicNumber = s.toString().replaceAll("\\D", "");
            StringBuilder formattedCNIC = new StringBuilder();

            // Add hyphens in the CNIC number
            for (int i = 0; i < cnicNumber.length(); i++) {
                if (i == 5 || i == 12) {
                    formattedCNIC.append("-");
                }
                formattedCNIC.append(cnicNumber.charAt(i));
            }

            // Update the EditText with the formatted CNIC number
            editText.removeTextChangedListener(cnicWatcher);
            editText.setText(formattedCNIC.toString());
            editText.setSelection(formattedCNIC.length());
            editText.addTextChangedListener(cnicWatcher);
        }
    }

    public static void showDatePickerDialog(Context context, View view, boolean isDateOfBirth) {
        final Calendar calendar = Calendar.getInstance();
        int yearNow = calendar.get(Calendar.YEAR);
        int monthNow = calendar.get(Calendar.MONTH);
        int dayNow = calendar.get(Calendar.DAY_OF_MONTH);

        SimpleDateFormat sdf = new SimpleDateFormat(Constants.UI_DATE_PATTERN, Locale.ENGLISH);

        if (isDateOfBirth) {
            // Applicant must be 18 to 60 years old: only those dates of birth can be picked
            long maxDateInMillis = getDobPickerMaxMillis();
            long minDateInMillis = getDobPickerMinMillis();
            yearNow -= MINIMUM_APPLICANT_AGE; // open the picker on the latest allowed date

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    context,
                    (datePicker, year, month, day) -> {
                        calendar.set(year, month, day);
                        String dateOfBirth = sdf.format(calendar.getTime());
                        // CommonFunctions.showToast(context, dateOfBirth);
                        if (view instanceof EditText) {
                            ((EditText) view).setText(dateOfBirth);
                        } else if (view instanceof TextView) {
                            ((TextView) view).setText(dateOfBirth);
                        }
                    },
                    yearNow,
                    monthNow,
                    dayNow);

            datePickerDialog.getDatePicker().setMinDate(minDateInMillis);
            datePickerDialog.getDatePicker().setMaxDate(maxDateInMillis);

            datePickerDialog.show();
        } else {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    context,
                    (datePicker, year, month, day) -> {
                        calendar.set(year, month, day);
                        String selectedDate = sdf.format(calendar.getTime());
                        if (view instanceof EditText) {
                            ((EditText) view).setText(selectedDate);
                        } else if (view instanceof TextView) {
                            ((TextView) view).setText(selectedDate);
                        }
                    },
                    yearNow,
                    monthNow,
                    dayNow);

            datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
            datePickerDialog.show();
        }
    }

    public static void setFormattedDateOnTextView(Calendar selectedDate, String datePattern, TextView textview) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dateFormat = new SimpleDateFormat();
        if (selectedDate != null && datePattern != null) {
            dateFormat.applyPattern(datePattern);
            String formattedDate = dateFormat.format(selectedDate.getTime());
            textview.setText(formattedDate);
        }
    }

    public static void handleBackPressed(Activity activity) {
        if (doubleBackToExitPressedOnce) {
            activity.finish(); // or use super.onBackPressed();
            return;
        }

        doubleBackToExitPressedOnce = true;
        Toast.makeText(activity, activity.getResources().getString(R.string.please_click_back_again_to_exit),
                Toast.LENGTH_SHORT).show();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000); // 2 seconds to press back again
    }

    public static String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.HYHEN_SEPARATED_SHORT_YEAR_DATE_PATTERN,
                Locale.US);
        return dateFormat.format(new Date());
    }

    public static String getCurrentTimeAsString() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(Constants.TimeHoursMinutes, Locale.getDefault());
        return formatter.format(currentTime);
    }

    public static String formatDateOfBirth(String dob) {
        if (dob == null || dob.trim().isEmpty()) {
            return "";
        }
        String cleanDob = dob.trim();
        if (cleanDob.contains("T")) {
            cleanDob = cleanDob.split("T")[0];
        }

        String[] inputFormats = {
                Constants.UI_DATE_PATTERN, // dd-MMM-yyyy
                "dd-MMM-yyyy",
                "d-MMM-yyyy",
                Constants.SERVER_DATE_PATTERN, // M/d/yyyy
                "M/d/yyyy",
                "MM/dd/yyyy",
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "dd-MM-yyyy",
                "dd/MM/yyyy",
                "d-M-yyyy",
                "d/M/yyyy",
                Constants.internationalStandardDateFormat,
                Constants.ShortNumericDate,
                Constants.HyphenSeparatedDate,
                Constants.HYHEN_SEPARATED_DATE_PATTERN,
                Constants.HYHEN_SEPARATED_SHORT_YEAR_DATE_PATTERN,
                Constants.YearMonthDaySlash,
                Constants.DayMonthYearAbbreviated,
                Constants.YearMonthDayWithTime,
                Constants.DayOfWeekAbbreviated,
                Constants.TimeWithMonthAndYear,
                Constants.FullDateTimeWithMilliseconds,
                Constants.FullMonthDateYear,
                Constants.TimeWithDayAndMonth,
                Constants.CompactNumericDate,
                Constants.MonthYearDaySlash,
                Constants.TimeWithMilliseconds,
                Constants.DotSeparatedDate,
                Constants.MonthDayYearAbbreviated,
                Constants.TimeWithDayAndMonthAbbreviated,
                Constants.FullNumericDateTime,
                Constants.YearMonthDayAbbreviatedHyphen,
                Constants.FullDateTimeWithMonth,
                Constants.DayOfWeekAbbreviatedHyphen,
                Constants.CompactNumericDateTimeWithMilliseconds,
                Constants.FullMonthDateCommaYear,
                Constants.FullDateTimeWithMonthAndTime
        };

        // Define the output date format
        SimpleDateFormat outputFormat = new SimpleDateFormat(Constants.UI_DATE_PATTERN, Locale.ENGLISH);

        // Attempt to parse the input date string using each format in the array
        for (String inputFormat : inputFormats) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(inputFormat, Locale.ENGLISH);
                // FIX: lenient parsing turned "26-09-2030" into the year 0032 (via yyyy-MM-dd) and
                // "15/08/1990" into March 1991 (via M/d/yyyy). Strict parsing rejects those so the
                // correct pattern further down the list is used.
                dateFormat.setLenient(false);
                Date date = dateFormat.parse(cleanDob);
                if (date != null) return outputFormat.format(date);
            } catch (ParseException ignored) {
            }
        }

        return cleanDob;
    }

    public static String formatDateForServer(String uiDate) {
        if (uiDate == null || uiDate.trim().isEmpty()) return "";

        String cleanDate = uiDate.trim();
        if (cleanDate.contains("T")) {
            cleanDate = cleanDate.split("T")[0];
        }

        String[] inputFormats = {
                Constants.UI_DATE_PATTERN, // dd-MMM-yyyy
                "dd-MMM-yyyy",
                "d-MMM-yyyy",
                Constants.SERVER_DATE_PATTERN, // M/d/yyyy
                "M/d/yyyy",
                "MM/dd/yyyy",
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "dd-MM-yyyy",
                "dd/MM/yyyy",
                "d-M-yyyy",
                "d/M/yyyy",
                "yyyyMMdd",
                "dd.MM.yyyy",
                "MM/dd/yy"
        };

        SimpleDateFormat serverFormat = new SimpleDateFormat(Constants.SERVER_DATE_PATTERN, Locale.ENGLISH);

        for (String format : inputFormats) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(format, Locale.ENGLISH);
                parser.setLenient(false); // FIX: see formatDateOfBirth
                Date date = parser.parse(cleanDate);
                if (date != null) {
                    return serverFormat.format(date);
                }
            } catch (ParseException ignored) {
            }
        }

        return cleanDate;
    }

    /** Allowed age (full years, as of today) to sign up or apply for financing: 18 to 60. */
    public static final int MINIMUM_APPLICANT_AGE = 18;
    public static final int MAXIMUM_APPLICANT_AGE = 60;

    /** Age in full years as of today, or -1 when the date cannot be read (any format the app uses). */
    public static int getAgeInYears(String dateOfBirth) {
        String serverDate = formatDateForServer(dateOfBirth);
        if (serverDate.isEmpty()) return -1;
        try {
            SimpleDateFormat parser = new SimpleDateFormat(Constants.SERVER_DATE_PATTERN, Locale.ENGLISH);
            parser.setLenient(false);
            Date birth = parser.parse(serverDate);
            if (birth == null) return -1;
            Calendar dob = Calendar.getInstance();
            dob.setTime(birth);
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            boolean birthdayNotYetThisYear = today.get(Calendar.MONTH) < dob.get(Calendar.MONTH)
                    || (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH)
                    && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH));
            if (birthdayNotYetThisYear) age--;
            return age;
        } catch (ParseException e) {
            return -1;
        }
    }

    /** True when the applicant is between 18 and 60 years old (inclusive) today. */
    public static boolean isAgeAllowed(String dateOfBirth) {
        int age = getAgeInYears(dateOfBirth);
        return age >= MINIMUM_APPLICANT_AGE && age <= MAXIMUM_APPLICANT_AGE;
    }

    /** Popup shown when the applicant is younger than 18 or older than 60. */
    public static void showAgeRestrictionDialog(Activity activity, boolean isSignup, String dateOfBirth) {
        String action = isSignup ? "register" : "apply for financing";
        int age = getAgeInYears(dateOfBirth);
        String reason = age > MAXIMUM_APPLICANT_AGE
                ? "According to your date of birth you are above " + MAXIMUM_APPLICANT_AGE + " years of age."
                : age >= 0 ? "According to your date of birth you are under " + MINIMUM_APPLICANT_AGE + " years of age."
                : "Your date of birth could not be read.";
        showPremiumFeedbackDialog(activity, "error", "Age Requirement",
                "You must be between " + MINIMUM_APPLICANT_AGE + " and " + MAXIMUM_APPLICANT_AGE + " years old to " + action + ". " + reason, false);
    }

    /** Latest date of birth the pickers allow (the applicant turns 18 today). */
    public static long getDobPickerMaxMillis() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, -MINIMUM_APPLICANT_AGE);
        return c.getTimeInMillis();
    }

    /** Earliest date of birth the pickers allow (the applicant is still 60 and turns 61 tomorrow). */
    public static long getDobPickerMinMillis() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, -(MAXIMUM_APPLICANT_AGE + 1));
        c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    public static String getOneMonthAgoDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1); // Subtract one month

        SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.HYHEN_SEPARATED_SHORT_YEAR_DATE_PATTERN,
                Locale.US);
        return dateFormat.format(calendar.getTime());
    }

    public static void shareApp(Context context) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.app_name));
            String shareMessage = "Let me recommend you this " + context.getResources().getString(R.string.app_name)
                    + " Application \n\n";
            // shareMessage = shareMessage +
            // "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID
            // + "\n";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            context.startActivity(Intent.createChooser(shareIntent, "choose one"));
        } catch (Exception e) {
            // e.toString();
        }
    }

    public static String addCommasToNumber(double number) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(number);
    }

    public static String formatDecimalValue(double value) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(value);
    }

    public static String calculateSum(List<DataModelGetLedger> receiptList) {
        double sum = 0;
        for (DataModelGetLedger receipt : receiptList) {
            sum += Double.parseDouble(receipt.getTotal());
        }
        return CommonFunctions.formatDecimalValue(sum);
    }

    public static boolean isValidCNIC(String cnic) {
        cnic = CommonFunctions.removeDashes(cnic);
        return cnic.matches("\\d{13}");
    }

    public static boolean isValidPhone(String phone) {
        phone = CommonFunctions.removeDashes(phone);
        return phone.matches("\\d{11}");
    }

    /// Field Validation
    public static boolean isValidFullName(String fullName) {
        // Full name should contain only characters (letters and spaces)
        return !TextUtils.isEmpty(fullName) && fullName.matches("[a-zA-Z ]+");
    }

    public static boolean isValidImage(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

        final Pattern pattern = Pattern.compile(EMAIL_REGEX);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    /*
     * public static boolean isValidCNIC(String cnic) {
     * // CNIC should contain only digits and should be exactly 13 characters long
     * return !TextUtils.isEmpty(cnic) && cnic.matches("[0-9]+") && cnic.length() ==
     * 13;
     * }
     */

    public static boolean isValidExpiryDate(String expiryDate) {
        // Expiry date should be in the format MM/YYYY and contain only digits and a
        // slash "/"
        return !TextUtils.isEmpty(expiryDate) && expiryDate.matches("\\d{2}/\\d{4}");
    }

    public static boolean isValidDateOfBirth(String dob) {
        // Date of birth should be in the format MM/DD/YYYY and contain only digits and
        // a slash "/"
        return !TextUtils.isEmpty(dob) && dob.matches("\\d{2}/\\d{2}/\\d{4}");
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {
        // Mobile number should be exactly 11 digits and start with 03
        return !TextUtils.isEmpty(phoneNumber) && phoneNumber.matches("03\\d{9}");
    }

    public static boolean isValidPinCode(String pinCode) {
        // Pin code should be exactly 4 digits
        return !TextUtils.isEmpty(pinCode) && pinCode.matches("\\d{4}");
    }

    public static String convertToBase64(Object image, Context context) {
        if (image instanceof Bitmap) {
            Bitmap bitmap = (Bitmap) image;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } else if (image instanceof Uri) {
            Uri uri = (Uri) image;
            try {
                Bitmap bitmap = CnicRotationHelper.loadBitmapWithExif(context, uri);
                if (bitmap != null) {
                    // Calculate target dimensions (max 1024x1024)
                    int maxWidth = 1024;
                    int maxHeight = 1024;
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    
                    if (width > maxWidth || height > maxHeight) {
                        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
                        width = Math.round(ratio * width);
                        height = Math.round(ratio * height);
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                        if (scaledBitmap != bitmap) {
                            bitmap.recycle();
                            bitmap = scaledBitmap;
                        }
                    }
                    
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);
                    byte[] bytes = byteArrayOutputStream.toByteArray();
                    return Base64.encodeToString(bytes, Base64.NO_WRAP);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;

    }

    public static boolean validateEditText(Context context, NestedScrollView scrollView, EditText editText,
            String fieldName) {
        String text = Objects.requireNonNull(editText.getText()).toString().trim();
        if (TextUtils.isEmpty(text)) {
            showInlineError(context, editText, "Please enter " + fieldName);
            scrollViewToVisible(editText);
            editText.requestFocus();
            return false;
        }
        removeInlineError(editText);
        return true;
    }

    public static void showInlineError(Context context, EditText editText, String errorMessage) {
        ViewParent parent = editText.getParent();
        if (parent instanceof ViewGroup) {
            ViewGroup wrapper = (ViewGroup) parent;
            ViewParent grandParent = wrapper.getParent();
            if (grandParent instanceof LinearLayout) {
                LinearLayout container = (LinearLayout) grandParent;
                int wrapperIndex = container.indexOfChild(wrapper);

                TextView errorTextView = null;
                if (wrapperIndex + 1 < container.getChildCount()) {
                    View nextChild = container.getChildAt(wrapperIndex + 1);
                    if ("INLINE_ERROR".equals(nextChild.getTag())) {
                        errorTextView = (TextView) nextChild;
                    }
                }

                if (errorTextView == null) {
                    errorTextView = new TextView(context);
                    errorTextView.setTag("INLINE_ERROR");
                    errorTextView.setTextColor(Color.parseColor("#EF4444"));
                    errorTextView.setTextSize(11f);
                    float density = context.getResources().getDisplayMetrics().density;
                    errorTextView.setPadding((int) (14 * density), (int) (2 * density), (int) (14 * density),
                            (int) (4 * density));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    int marginTop = (int) (-12 * density);
                    int marginBottom = (int) (12 * density);
                    params.setMargins(0, marginTop, 0, marginBottom);
                    container.addView(errorTextView, wrapperIndex + 1, params);
                }

                errorTextView.setText(errorMessage);
                errorTextView.setVisibility(View.VISIBLE);
            } else {
                editText.setError(errorMessage);
            }
        } else {
            editText.setError(errorMessage);
        }
        // Always remove the default popup error
        editText.setError(null);
    }

    public static void removeInlineError(EditText editText) {
        ViewParent parent = editText.getParent();
        if (parent instanceof ViewGroup) {
            ViewGroup wrapper = (ViewGroup) parent;
            ViewParent grandParent = wrapper.getParent();
            if (grandParent instanceof LinearLayout) {
                LinearLayout container = (LinearLayout) grandParent;
                int wrapperIndex = container.indexOfChild(wrapper);
                if (wrapperIndex + 1 < container.getChildCount()) {
                    View nextChild = container.getChildAt(wrapperIndex + 1);
                    if ("INLINE_ERROR".equals(nextChild.getTag())) {
                        nextChild.setVisibility(View.GONE);
                    }
                }
            }
        }
        editText.setError(null);
    }

    public static void scrollViewToVisible(final View view) {
        final Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        final boolean visible = rect.contains(0, 0, view.getWidth(), view.getHeight());
        if (!visible) {
            view.post(() -> {
                ViewParent parent = view.getParent();
                if (parent instanceof ViewGroup) {
                    scrollParentToChild((ViewGroup) parent, view);
                }
            });
        }
    }

    private static void scrollParentToChild(ViewGroup parent, View child) {
        if (parent instanceof ScrollView) {
            android.graphics.Rect rect = new android.graphics.Rect(0, 0, child.getWidth(), child.getHeight());
            parent.offsetDescendantRectToMyCoords(child, rect);
            ((ScrollView) parent).smoothScrollTo(0, rect.top - 50);
        } else if (parent instanceof androidx.core.widget.NestedScrollView) {
            android.graphics.Rect rect = new android.graphics.Rect(0, 0, child.getWidth(), child.getHeight());
            parent.offsetDescendantRectToMyCoords(child, rect);
            ((androidx.core.widget.NestedScrollView) parent).smoothScrollTo(0, rect.top - 50);
        } else {
            ViewParent grandParent = parent.getParent();
            if (grandParent instanceof ViewGroup) {
                scrollParentToChild((ViewGroup) grandParent, child);
            }
        }
    }

    public static void showPremiumFeedbackDialog(final Activity activity, String type, String title, String message,
            boolean finishOnClose) {
        showPremiumFeedbackDialog(activity, type, title, message, null, finishOnClose ? activity::finish : null);
    }

    public static void showPremiumFeedbackDialog(final Activity activity, String type, String title, String message,
            String buttonText, Runnable onDismiss) {
        if (activity == null || activity.isFinishing()) return;
        final Dialog dialog = new Dialog(activity);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_feedback_premium, null);
        dialog.setContentView(dialogView);
        dialog.setCancelable(false);

        LottieAnimationView feedbackAnim = dialog.findViewById(R.id.feedbackAnim);
        ImageView feedbackIcon = dialog.findViewById(R.id.feedbackIcon);
        TextView feedbackTitle = dialog.findViewById(R.id.feedbackTitle);
        TextView feedbackMessage = dialog.findViewById(R.id.feedbackMessage);
        Button btnFeedbackAction = dialog.findViewById(R.id.btnFeedbackAction);

        feedbackTitle.setText(title);
        feedbackMessage.setText(message);
        if (buttonText != null && !buttonText.isEmpty()) {
            btnFeedbackAction.setText(buttonText);
        }

        switch (type.toLowerCase()) {
            case "success":
                feedbackAnim.setVisibility(View.VISIBLE);
                feedbackIcon.setVisibility(View.GONE);
                try {
                    feedbackAnim.setAnimation(R.raw.success_anim);
                    feedbackAnim.playAnimation();
                } catch (Exception e) {
                    feedbackIcon.setVisibility(View.VISIBLE);
                    feedbackIcon.setImageResource(R.drawable.ic_shield_check);
                }
                feedbackTitle.setTextColor(ContextCompat.getColor(activity, R.color.status_success));
                btnFeedbackAction
                        .setBackgroundTintList(ContextCompat.getColorStateList(activity, R.color.status_success));
                break;
            case "error":
                feedbackAnim.setVisibility(View.GONE);
                feedbackIcon.setVisibility(View.VISIBLE);
                feedbackIcon.setImageResource(R.drawable.ic_alert_circle);
                feedbackIcon.setImageTintList(ContextCompat.getColorStateList(activity, R.color.status_error));
                feedbackTitle.setTextColor(ContextCompat.getColor(activity, R.color.status_error));
                btnFeedbackAction
                        .setBackgroundTintList(ContextCompat.getColorStateList(activity, R.color.status_error));
                break;
            case "warning":
            case "already":
                feedbackAnim.setVisibility(View.GONE);
                feedbackIcon.setVisibility(View.VISIBLE);
                feedbackIcon.setImageResource(R.drawable.ic_alert_circle);
                feedbackIcon.setImageTintList(ContextCompat.getColorStateList(activity, R.color.status_warning));
                feedbackTitle.setTextColor(ContextCompat.getColor(activity, R.color.status_warning));
                btnFeedbackAction
                        .setBackgroundTintList(ContextCompat.getColorStateList(activity, R.color.status_warning));
                break;
            default:
                feedbackAnim.setVisibility(View.GONE);
                feedbackIcon.setVisibility(View.VISIBLE);
                feedbackIcon.setImageResource(R.drawable.ic_alert_circle);
                feedbackIcon.setImageTintList(ContextCompat.getColorStateList(activity, R.color.status_info));
                feedbackTitle.setTextColor(ContextCompat.getColor(activity, R.color.status_info));
                btnFeedbackAction.setBackgroundTintList(ContextCompat.getColorStateList(activity, R.color.status_info));
                break;
        }

        btnFeedbackAction.setOnClickListener(v -> {
            dialog.dismiss();
            if (onDismiss != null) {
                onDismiss.run();
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    public static void showSuccessDialog(final Activity activity, String message) {
        final Dialog dialog = new Dialog(activity);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LayoutInflater inflater = activity.getLayoutInflater();
        @SuppressLint("InflateParams")
        View dialogView = inflater.inflate(R.layout.dialog_animation_with_button, null);
        dialog.setContentView(dialogView);

        AppCompatButton okButton = dialog.findViewById(R.id.okButton);
        AppCompatTextView txtMessage = dialog.findViewById(R.id.txtMessage);
        txtMessage.setText(message);
        LottieAnimationView lottieAnimationView = dialog.findViewById(R.id.lottieAnimationView);

        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            activity.finish();
        });

        dialog.show();
        lottieAnimationView.playAnimation();
    }

    public static boolean validateSpinner(Context context, Spinner spinner, TextView errorTextView,
            String errorMessage) {
        if (spinner.getSelectedItemPosition() <= 0) { // Typically 0 is the hint "Select..."
            if (errorTextView != null) {
                errorTextView.setText(errorMessage);
                errorTextView.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
            }
            scrollViewToVisible(spinner);
            spinner.requestFocus();
            return false;
        } else {
            if (errorTextView != null) {
                errorTextView.setVisibility(View.GONE);
            }
            return true;
        }
    }

    public static String removeDashes(String input) {
        return input.replaceAll("-", "");
    }

    public static void loadPicassoImage(String URL, ImageView imageView) {
        if (URL == null || URL.trim().isEmpty()) {
            imageView.setImageResource(R.drawable.dummy_profile_image);
            return;
        }
        String fullUrl = ApiManager.getFullImageUrl(URL);
        if (fullUrl.startsWith("http://") || fullUrl.startsWith("https://")) {
            Picasso.get()
                    .load(fullUrl)
                    .noFade()
                    .placeholder(R.drawable.dummy_profile_image)
                    .error(R.drawable.dummy_profile_image)
                    .into(imageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            android.util.Log.d("CommonFunctions", "Picasso successfully loaded: " + fullUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            android.util.Log.e("CommonFunctions", "Picasso failed to load: " + fullUrl, e);
                        }
                    });
        } else {
            try {
                Context context = imageView.getContext();
                int resId = context.getResources().getIdentifier(URL, "drawable", context.getPackageName());
                if (resId != 0) {
                    imageView.setImageResource(resId);
                } else {
                    imageView.setImageResource(R.drawable.dummy_profile_image);
                }
            } catch (Exception e) {
                imageView.setImageResource(R.drawable.dummy_profile_image);
            }
        }
    } // ← closing brace for loadPicassoImage

    private static String lastApiFailureMessage = "";
    private static long lastApiFailureShownAt = 0L;

    /** True when the phone has a working internet connection (Wi-Fi or mobile data). */
    public static boolean isInternetAvailable(Context context) {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return true;
        }
    }

    public static void handleApiFailure(Context context, View view, Throwable t) {
        if (context == null)
            return;

        // FIX: connection errors used to be ignored silently (assumed the NetworkMonitor snackbar would show).
        // That only covers "phone offline" — when the SERVER is down the user saw nothing at all and kept
        // retrying. Now every failure shows a clear message.
        boolean isConnectionError = t instanceof java.net.UnknownHostException ||
                t instanceof java.net.ConnectException ||
                t instanceof java.net.SocketTimeoutException ||
                t instanceof java.util.concurrent.TimeoutException ||
                t instanceof java.net.NoRouteToHostException;

        String message;
        if (isConnectionError) {
            message = isInternetAvailable(context)
                    ? "Our server is not responding right now. Please try again in a few minutes."
                    : "No internet connection. Please check your mobile data or Wi-Fi and try again.";
        } else {
            message = context.getString(R.string.str_unable_to_process_request);
        }

        // Several requests can fail at once (e.g. Home loads 3 APIs): show the same message only once per 6 s
        long now = System.currentTimeMillis();
        if (message.equals(lastApiFailureMessage) && now - lastApiFailureShownAt < 6000) {
            return;
        }
        lastApiFailureMessage = message;
        lastApiFailureShownAt = now;

        // A dialog is more noticeable than a snackbar, so prefer it whenever we have an Activity
        if (context instanceof Activity && !((Activity) context).isFinishing()) {
            showErrorDialog((Activity) context, message);
        } else if (view != null) {
            showSnakeBar(view, message);
        } else if (context instanceof Activity) {
            showErrorDialog((Activity) context, message);
        } else {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sanitizes raw technical error messages into professional user-facing
     * messages.
     * Suitable for a banking application where technical details should be hidden.
     */
    public static String getProfessionalErrorMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return "Unable to process your request at this time. Please try again later.";
        }

        String lowerMsg = rawMessage.toLowerCase();

        // Filter out common technical jargon, SQL errors, PHP errors, backend failures
        if (lowerMsg.contains("ora-") || lowerMsg.contains("sql") ||
                lowerMsg.contains("exception") || lowerMsg.contains("syntax") ||
                lowerMsg.contains("database") || lowerMsg.contains("connection") ||
                lowerMsg.contains("fatal error") || lowerMsg.contains("stack trace") ||
                lowerMsg.contains("nullpointer") || lowerMsg.contains("failed to submit") ||
                lowerMsg.contains("internal server") || lowerMsg.contains("timeout") ||
                lowerMsg.contains("undefined") || lowerMsg.contains("api error")) {

            return "We are experiencing technical difficulties processing your request. Please try again after some time.";
        }

        return rawMessage;
    }
}
