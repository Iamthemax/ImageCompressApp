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

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity {

    private EditText thresholdInput;
    ImageView imageView;
    TextView tvCompressedSize,tvOriginalSize;
    Button btnPickImage;
    ImageButton btnShare;
    Bitmap bitmap;
    int compressThreshold=200;
    byte[] byteArrayImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        tvCompressedSize=findViewById(R.id.tvCompressedSize);
        tvOriginalSize=findViewById(R.id.tvOriginalSize);
        btnPickImage=findViewById(R.id.btnPickImage);
        imageView=findViewById(R.id.imageView);
        btnShare=findViewById(R.id.btnShare);
        thresholdInput = findViewById(R.id.thresholdInput);
        btnShare.setOnClickListener(view -> checkPermissions());
        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {




                String thresholdText = thresholdInput.getText().toString().trim();

                // Validate if the input is an integer
                if (isValidInteger(thresholdText)) {
                    compressThreshold  = Integer.parseInt(thresholdText);
                    // Do something with the threshold value
                    pickImage();
                    Toast.makeText(MainActivity.this, "Threshold: " + compressThreshold + " KB ", Toast.LENGTH_SHORT).show();
                } else {
                    // Show error if input is not a valid integer
                    Toast.makeText(MainActivity.this, "Please enter a valid integer.", Toast.LENGTH_SHORT).show();
                }
            }
        });



    }
    private void checkPermissions() {
//        Log.d("MyTag","MainActivity.checkPermissions=>");
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            // Check for permissions
//            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                // Request permission
//                ActivityCompat.requestPermissions(this, new String[]{ android.Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
//            } else {
//                // If permission is already granted, proceed
//                shareBitmap(bitmap);
//            }
//        } else {
//            // No need to request permission on devices below Android 6.0 (API 23)
//            shareBitmap(bitmap);
//        }
        shareBitmap(byteArrayImage);
    }
private boolean isValidInteger(String input) {
    if (TextUtils.isEmpty(input)) {
        return false;
    }

    try {
        Integer.parseInt(input); // Try to parse as an integer
        return true;
    } catch (NumberFormatException e) {
        return false; // If parsing fails, it's not a valid integer
    }
}

    private void pickImage() {
        // Launch the media picker to select an image
        pickVisualMediaLauncher.launch(new PickVisualMediaRequest.Builder().build());
    }



    private final ActivityResultLauncher<PickVisualMediaRequest> pickVisualMediaLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    result -> {
                        if (result != null) {
                            // Successfully picked an image/video, handle the URI
                            Log.d("MyTag", "Selected URI: " + result.toString());
                            handlePickedMedia(result);
                        } else {
                            // The user didn't pick anything or an error occurred
                            Log.d("MyTag", "No media selected");
                        }
                    });


    private void handlePickedMedia(Uri mediaUri) {
        // Here, mediaUri is the URI of the picked image or video.
        Log.d("MyTag", "Picked media URI: " + mediaUri.toString());

        // Create and show a progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Compressing Image");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false); // Don't let the user cancel the progress dialog
        progressDialog.show();

        // Use CompletableFuture to handle image compression asynchronously
        CompletableFuture<Void> compressionFuture = CompletableFuture.runAsync(() -> {
            try {
                // Get the file size before compression
                Log.d("MyTag", "File Size Original : " + getImageFileSize(mediaUri) / 1024 + " kb");

                // Create an instance of XImageCompressor
                XImageCompressor imageCompressor = new XImageCompressor(MainActivity.this);
                byte[] compressedByteArray = imageCompressor.compressImage(mediaUri, compressThreshold * 1024); // Compress to 200 KB
                byteArrayImage=compressedByteArray;
                // Get the size of the compressed image
                Log.d("MyTag", "File Size Compressed : " + compressedByteArray.length / 1024 + " kb");

                // Decode the compressed byte array into a Bitmap
                Bitmap compressedBitmap = BitmapFactory.decodeByteArray(compressedByteArray, 0, compressedByteArray.length);

                // After compression is done, update the UI
                runOnUiThread(() -> {
                    if (compressedBitmap != null) {
                        // Set the compressed Bitmap to the ImageView
                        imageView.setImageBitmap(compressedBitmap);
                        bitmap=compressedBitmap;
                        tvCompressedSize.setText("CompressedSize : "+formatFileSize(compressedByteArray.length));
                        tvOriginalSize.setText("Original Size : "+formatFileSize(getImageFileSize(mediaUri)));
                    } else {
                        Log.e("MyTag", "Failed to decode the compressed byte array into a Bitmap.");
                    }

                    // Dismiss the progress dialog
                    progressDialog.dismiss();
                });

            } catch (Exception e) {
                // Handle exceptions
                Log.e("MyTag", "Error during image compression: " + e.getMessage());
                runOnUiThread(() -> progressDialog.dismiss()); // Dismiss progress dialog in case of error
            }
        });

        // Optional: Handle completion or error of the CompletableFuture
        compressionFuture.exceptionally(ex -> {
            Log.e("MyTag", "Error during image compression", ex);
            runOnUiThread(() -> progressDialog.dismiss()); // Dismiss progress dialog in case of error
            return null;
        });
    }

    private long getImageFileSize(Uri imageUri) {
        long size = -1;
        ContentResolver contentResolver = getContentResolver();

        // Open the content provider and query for the file size
        try (InputStream inputStream = contentResolver.openInputStream(imageUri)) {
            if (inputStream != null) {
                // Get file size using the InputStream
                size = inputStream.available(); // This gives you the size in bytes
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return size;
    }
    private String formatFileSize(long sizeInBytes) {
        if (sizeInBytes <= 0) return "0 B";

        // Conversion constants
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

    private void shareBitmap(Bitmap bitmap) {
        try {
            
            Log.d("MyTag","MainActivity.shareBitmap=>");
            // Save the Bitmap to a temporary file
            File file = new File(getExternalCacheDir(), "shared_image.png"); // Use cache directory for temp storage
            OutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream); // Compress bitmap and write to file
            outputStream.flush();
            outputStream.close();

            // Get the Uri for the file using FileProvider
            Uri imageUri = FileProvider.getUriForFile(MainActivity.this, "vish.imagecompressapp.fileprovider", file);

            // Create an Intent to share the image
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri); // Attach the image URI to the Intent

            // Optionally, add extra text to share
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this image!");

            // Grant permission to other apps to access the file URI
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Check if any app can handle the intent before starting the activity
            if (shareIntent.resolveActivity(getPackageManager()) != null) {
                // Start the share intent and allow the user to choose the app to share with
                startActivity(Intent.createChooser(shareIntent, "Share Image"));
            } else {
                Toast.makeText(this, "No app found to share image", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MyTag", "Error while sharing image: " + e.getMessage());
            Toast.makeText(this, "Error while sharing image", Toast.LENGTH_SHORT).show();
        }
    }
    private void shareBitmap(byte[] compressedByteArray) {
        try {
            Log.d("MyTag", "MainActivity.shareBitmap=>");

            // Create a temporary file to save the byte array
            File file = new File(getExternalCacheDir(), "shared_image.png");

            // Use FileOutputStream to save the compressed byte array to the file
            try (OutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(compressedByteArray); // Write the byte array directly
            }

            // Get the Uri for the file using FileProvider
            Uri imageUri = FileProvider.getUriForFile(MainActivity.this, "vish.imagecompressapp.fileprovider", file);

            // Create an Intent to share the image
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri); // Attach the image URI to the Intent

            // Optionally, add extra text to share
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this image!");

            // Grant permission to other apps to access the file URI
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Check if any app can handle the intent before starting the activity
            if (shareIntent.resolveActivity(getPackageManager()) != null) {
                // Start the share intent and allow the user to choose the app to share with
                startActivity(Intent.createChooser(shareIntent, "Share Image"));
            } else {
                Toast.makeText(this, "No app found to share image", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MyTag", "Error while sharing image: " + e.getMessage());
            Toast.makeText(this, "Error while sharing image", Toast.LENGTH_SHORT).show();
        }
    }






}