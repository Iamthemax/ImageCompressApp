# XImageCompressor

`XImageCompressor` is an Android library designed to efficiently compress images from a URI or a `Bitmap` object to a smaller size, while maintaining reasonable quality. The library allows you to compress images asynchronously in the background, freeing up the main thread and optimizing memory usage.

## Features

- **Compress images** from both `Uri` and `Bitmap` objects.
- **Background compression** using a single-threaded executor to offload the task from the main thread.
- **Dynamic image scaling** to optimize memory usage before compression.
- **Support for multiple image formats** including JPEG, PNG, and WebP.
- **Adjustable quality reduction** to ensure that compressed images are under a specified size threshold.

## Installation

To use `XImageCompressor` in your Android project, follow these steps:

1. **Add the necessary permissions** to your `AndroidManifest.xml` file:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />


Clone or download the repository into your Android project or import it as a module.

Add the library to your project:

If you're using it as a module in your project, add the following line to your settings.gradle:
gradle
Copy code
include ':XImageCompressor'
If you want to use the library as an external dependency, you can build the library into an .aar file and import it into your project.
Usage
Here’s how you can use the XImageCompressor class to compress images.

Compressing an image from a URI
java
Copy code
// Create an instance of the compressor
XImageCompressor imageCompressor = new XImageCompressor(context);

// Define a threshold for compression (in bytes)
long compressionThreshold = 500 * 1024; // 500 KB

// Compress the image from URI
Uri imageUri = Uri.parse("path_to_your_image");
try {
    byte[] compressedImage = imageCompressor.compressImage(imageUri, compressionThreshold);
    // Use the compressed image bytes as needed
} catch (Exception e) {
    e.printStackTrace();
}
Compressing a Bitmap object directly
java
Copy code
// Create an instance of the compressor
XImageCompressor imageCompressor = new XImageCompressor(context);

// Define a threshold for compression (in bytes)
long compressionThreshold = 500 * 1024; // 500 KB

// Compress the bitmap directly
Bitmap originalBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.image);
try {
    byte[] compressedImage = imageCompressor.compressImage(originalBitmap, compressionThreshold);
    // Use the compressed image bytes as needed
} catch (Exception e) {
    e.printStackTrace();
}
Shutting down the Compressor
Once you’re done with the image compression, you can shut down the background executor to clean up resources:

java
Copy code
imageCompressor.shutdown();
How It Works
Scaling: Before compressing, the image is scaled to a maximum size (1024x1024) to avoid memory overload when dealing with large images.

Compression: The image is compressed in a loop, gradually decreasing its quality by 10 points until it fits within the specified size threshold.

Background Execution: Compression happens in a background thread using ExecutorService to ensure that the main UI thread remains responsive.

Supported Formats: The library supports JPEG, PNG, and WebP formats. The format is automatically detected based on the MIME type of the image.

Performance Considerations
The XImageCompressor library aims to compress images to a smaller size while maintaining the best quality possible. However, the quality reduction can be adjusted based on the needs of your application.
The background execution ensures that the main UI thread isn't blocked during compression, improving user experience.
