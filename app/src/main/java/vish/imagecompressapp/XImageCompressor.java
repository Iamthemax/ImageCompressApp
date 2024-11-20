package vish.imagecompressapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class XImageCompressor {

    private final Context context;
    private final ExecutorService executorService;

    public XImageCompressor(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();  // Creates a single-threaded executor
    }

    // Compress image from URI
    public byte[] compressImage(Uri contentUri, long compressionThreshold) throws Exception {
        Future<byte[]> future = executorService.submit(new Callable<byte[]>() {
            @Override
            public byte[] call() throws IOException {
                // Read mime type and input stream
                String mimeType = context.getContentResolver().getType(contentUri);
                InputStream inputStream = context.getContentResolver().openInputStream(contentUri);

                if (inputStream == null) {
                    return null;
                }

                // Load the image as Bitmap, scaled down to avoid excessive memory usage
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                // Reduce image dimensions if large (scaling by 50% to start)
                bitmap = scaleImage(bitmap);

                Bitmap.CompressFormat compressFormat = determineCompressFormat(mimeType);

                // Start compressing
                return compressBitmap(bitmap, compressFormat, compressionThreshold);
            }
        });

        return future.get();  // Blocks until the task is done and returns the result
    }

    // Compress image from Bitmap directly
    public byte[] compressImage(Bitmap bitmap, long compressionThreshold) throws Exception {
        Future<byte[]> future = executorService.submit(new Callable<byte[]>() {
            @Override
            public byte[] call() throws IOException {
                // Scale the Bitmap before compressing
                final Bitmap scaledBitmap = scaleImage(bitmap); // 'scaledBitmap' is effectively final

                // Compression format is JPEG by default
                Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.JPEG;

                // Compress and return the result
                return compressBitmap(scaledBitmap, compressFormat, compressionThreshold);
            }
        });

        return future.get();  // Blocks until the task is done and returns the result
    }


    // Helper method to determine the appropriate compression format based on MIME type
    private Bitmap.CompressFormat determineCompressFormat(String mimeType) {
        Bitmap.CompressFormat compressFormat;

        switch (mimeType) {
            case "image/png":
                compressFormat = Bitmap.CompressFormat.PNG;
                break;
            case "image/jpeg":
                compressFormat = Bitmap.CompressFormat.JPEG;
                break;
            case "image/webp":
                compressFormat = Build.VERSION.SDK_INT >= 30 ? Bitmap.CompressFormat.WEBP_LOSSLESS : Bitmap.CompressFormat.WEBP;
                break;
            default:
                compressFormat = Bitmap.CompressFormat.JPEG;
        }

        return compressFormat;
    }

    // Helper method to scale down the image for memory optimization
    private Bitmap scaleImage(Bitmap originalBitmap) {
        int maxWidth = 1024;  // Max width for scaling
        int maxHeight = 1024; // Max height for scaling
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        // Calculate scale factor
        float scaleFactor = Math.min((float) maxWidth / width, (float) maxHeight / height);

        // Apply scaling if needed
        if (scaleFactor < 1) {
            width = Math.round(width * scaleFactor);
            height = Math.round(height * scaleFactor);
            return Bitmap.createScaledBitmap(originalBitmap, width, height, true);
        }

        return originalBitmap;
    }

    // Helper method to compress Bitmap to byte array
    private byte[] compressBitmap(Bitmap bitmap, Bitmap.CompressFormat compressFormat, long compressionThreshold) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int quality = 90; // Start with high quality

        // Compress image, iterating quality reduction until it fits within the threshold
        do {
            outputStream.reset();  // Clear previous output
            bitmap.compress(compressFormat, quality, outputStream);
            byte[] outputBytes = outputStream.toByteArray();

            // If we have achieved the desired size, return
            if (outputBytes.length <= compressionThreshold) {
                return outputBytes;
            }

            // Gradually reduce quality
            quality -= 10;  // Decrease quality by 10 each iteration

        } while (quality > 5);  // Stop when the quality drops too low

        return new byte[0];  // Return an empty byte array if compression failed
    }

    public void shutdown() {
        executorService.shutdown();  // Shut down the executor when done
    }
}
