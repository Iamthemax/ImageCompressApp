package vish.imagecompressapp;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity {

    private EditText thresholdInput;
    private ImageView imageView;
    private TextView tvCompressedSize, tvOriginalSize;
    private Button btnPickImage;
    private ImageButton btnShare;
    private Bitmap bitmap;
    private int compressThreshold = 200;
    private byte[] byteArrayImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCompressedSize = findViewById(R.id.tvCompressedSize);
        tvOriginalSize = findViewById(R.id.tvOriginalSize);
        btnPickImage = findViewById(R.id.btnPickImage);
        imageView = findViewById(R.id.imageView);
        btnShare = findViewById(R.id.btnShare);
        thresholdInput = findViewById(R.id.thresholdInput);

        btnPickImage.setOnClickListener(view -> handleImagePick());
        btnShare.setOnClickListener(view -> shareBitmap(byteArrayImage));
    }

    private void handleImagePick() {
        String thresholdText = thresholdInput.getText().toString().trim();
        if (isValidInteger(thresholdText)) {
            compressThreshold = Integer.parseInt(thresholdText);
            pickImage();
            Toast.makeText(MainActivity.this, "Threshold: " + compressThreshold + " KB ", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Please enter a valid integer.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidInteger(String input) {
        if (TextUtils.isEmpty(input)) return false;
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void pickImage() {
        pickVisualMediaLauncher.launch(new PickVisualMediaRequest.Builder().build());
    }

    private final ActivityResultLauncher<PickVisualMediaRequest> pickVisualMediaLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    result -> {
                        if (result != null) {
                            Log.d("MyTag", "Selected URI: " + result.toString());
                            handlePickedMedia(result);
                        } else {
                            Log.d("MyTag", "No media selected");
                        }
                    });

    private void handlePickedMedia(Uri mediaUri) {
        Log.d("MyTag", "Picked media URI: " + mediaUri.toString());

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Compressing Image");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        CompletableFuture<Void> compressionFuture = CompletableFuture.runAsync(() -> {
            try {
                long originalSize = getImageFileSize(mediaUri);
                Log.d("MyTag", "Original Size: " + originalSize / 1024 + " KB");

                XImageCompressor imageCompressor = new XImageCompressor(MainActivity.this);
                byte[] compressedByteArray = imageCompressor.compressImage(mediaUri, compressThreshold * 1024);

                byteArrayImage = compressedByteArray;
                Bitmap compressedBitmap = BitmapFactory.decodeByteArray(compressedByteArray, 0, compressedByteArray.length);

                long compressedSize = compressedByteArray.length;
                Log.d("MyTag", "Compressed Size: " + compressedSize / 1024 + " KB");

                runOnUiThread(() -> {
                    if (compressedBitmap != null) {
                        imageView.setImageBitmap(compressedBitmap);
                        bitmap = compressedBitmap;
                        tvCompressedSize.setText("Compressed Size: " + formatFileSize(compressedSize));
                        tvOriginalSize.setText("Original Size: " + formatFileSize(originalSize));
                    } else {
                        Log.e("MyTag", "Failed to decode the compressed byte array.");
                    }
                    progressDialog.dismiss();
                });
            } catch (Exception e) {
                Log.e("MyTag", "Compression error: " + e.getMessage());
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Error during compression", Toast.LENGTH_SHORT).show();
                });
            }
        });

        compressionFuture.exceptionally(ex -> {
            Log.e("MyTag", "Error during image compression", ex);
            runOnUiThread(() -> progressDialog.dismiss());
            return null;
        });
    }

    private long getImageFileSize(Uri imageUri) {
        long size = -1;
        try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
            if (inputStream != null) {
                size = inputStream.available();
            }
        } catch (IOException e) {
            Log.e("MyTag", "Error getting image file size", e);
        }
        return size;
    }

    private String formatFileSize(long sizeInBytes) {
        if (sizeInBytes <= 0) return "0 B";

        final long KB = 1024;
        final long MB = KB * 1024;
        final long GB = MB * 1024;

        if (sizeInBytes < KB) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < MB) {
            return String.format("%.1f KB", sizeInBytes / (float) KB);
        } else if (sizeInBytes < GB) {
            return String.format("%.1f MB", sizeInBytes / (float) MB);
        } else {
            return String.format("%.1f GB", sizeInBytes / (float) GB);
        }
    }
    private void shareBitmap(byte[] compressedByteArray) {
        try {
            Log.d("MyTag", "Sharing Bitmap");

            File file = new File(getExternalCacheDir(), "shared_image.png");
            try (OutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(compressedByteArray);
            }

            Uri imageUri = FileProvider.getUriForFile(this, "vish.imagecompressapp.fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this image!");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (shareIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(shareIntent, "Share Image"));
            } else {
                Toast.makeText(this, "No app found to share image", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e("MyTag", "Error while sharing image: " + e.getMessage());
            Toast.makeText(this, "Error while sharing image", Toast.LENGTH_SHORT).show();
        }
    }
}
