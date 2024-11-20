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

    public byte[] compressImage(Uri contentUri, long compressionThreshold) throws Exception {
        Future<byte[]> future = executorService.submit(new Callable<byte[]>() {
            @Override
            public byte[] call() throws IOException {
                String mimeType = context.getContentResolver().getType(contentUri);
                InputStream inputStream = context.getContentResolver().openInputStream(contentUri);

                if (inputStream == null) {
                    return null;
                }

                byte[] inputBytes = new byte[inputStream.available()];
                inputStream.read(inputBytes);
                inputStream.close();

                Bitmap bitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.length);

                Bitmap.CompressFormat compressFormat;

                switch (mimeType) {
                    case "image/png":
                        compressFormat = Bitmap.CompressFormat.PNG;
                        break;
                    case "image/jpeg":
                        compressFormat = Bitmap.CompressFormat.JPEG;
                        break;
                    case "image/webp":
                        if (Build.VERSION.SDK_INT >= 30) {
                            compressFormat = Bitmap.CompressFormat.WEBP_LOSSLESS;
                        } else {
                            compressFormat = Bitmap.CompressFormat.WEBP;
                        }
                        break;
                    default:
                        compressFormat = Bitmap.CompressFormat.JPEG;
                }

                byte[] outputBytes;
                int quality = 90;

                do {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    bitmap.compress(compressFormat, quality, outputStream);
                    outputBytes = outputStream.toByteArray();
                    quality -= (int) (quality * 0.1f);
                } while (outputBytes.length > compressionThreshold && quality > 5 && compressFormat != Bitmap.CompressFormat.PNG);

                return outputBytes;
            }
        });

        return future.get();  // Blocks until the task is done and returns the result
    }

    public byte[] compressImage(Bitmap  bitmap, long compressionThreshold) throws Exception {
        Future<byte[]> future = executorService.submit(new Callable<byte[]>() {
            @Override
            public byte[] call() throws IOException {
                byte[] outputBytes;
                int quality = 90;

                do {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                    outputBytes = outputStream.toByteArray();
                    quality -= (int) (quality * 0.1f);
                } while (outputBytes.length > compressionThreshold && quality > 5 );

                return outputBytes;
            }
        });

        return future.get();  // Blocks until the task is done and returns the result
    }

    public void shutdown() {
        executorService.shutdown();  // Shut down the executor when done
    }
}