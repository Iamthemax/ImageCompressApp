# XImageCompressor

XImageCompressor` is an Android library designed to efficiently compress images from a URI or a `Bitmap` object to a smaller size, while maintaining reasonable quality. The library allows you to compress images asynchronously in the background, freeing up the main thread and optimizing memory usage.

## Features

- **Compress images** from both `Uri` and `Bitmap` objects.
- **Background compression** using a single-threaded executor to offload the task from the main thread.
- **Dynamic image scaling** to optimize memory usage before compression.
- **Support for multiple image formats** including JPEG, PNG, and WebP.
- **Adjustable quality reduction** to ensure that compressed images are under a specified size threshold.
