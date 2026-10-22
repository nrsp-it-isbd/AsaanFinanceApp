package com.assan.finance.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.TextureView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;

import com.assan.finance.R;
import com.assan.finance.base.BaseActivity;

public class CustomCameraActivity extends BaseActivity {

    private static final int STORAGE_PERMISSION_CODE = 101;
    private TextureView textureView;
    private AppCompatImageView guidelinesImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_custom_camera);
        textureView = findViewById(R.id.textureView);
        guidelinesImageView = findViewById(R.id.guidelinesImageView);

        // Initialize camera and start preview
        initializeCamera();
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, STORAGE_PERMISSION_CODE);
    }

    private void initializeCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        } else {
            // Start camera preview with guidelines overlay
          /*  CommonFunctions.captureImageFromCameraForCNIC(CustomCameraActivity.this, REQUEST_IMAGE_CAPTURE_CNIC_FRONT);
            textureView.setTransform(new Matrix());
            textureView.setRotation(90); // Adjust rotation as needed*/
        }
    }


}