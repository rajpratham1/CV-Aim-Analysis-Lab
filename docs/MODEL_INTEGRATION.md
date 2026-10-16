# Model Integration Guide

This document describes how to integrate and run on-device machine learning models for computer vision object detection in **CV Aim Analysis Lab**.

## Supported Model Architecture

The laboratory supports standard **TensorFlow Lite (TFLite)** object detection models (e.g., MobileNet SSD, EfficientDet-Lite).

### Input Tensor Specifications
- **Tensor 0**: Image input
  - **Dimensions**: `[1, height, width, 3]` (commonly `[1, 300, 300, 3]` or `[1, 320, 320, 3]`)
  - **Data Type**: `Float32` (normalized to `[0.0, 1.0]` or `[-1.0, 1.0]`) or `UInt8` (`[0, 255]`)

### Output Tensor Specifications (Standard SSD Detection Format)
- **Tensor 0 (`boxes`)**:
  - **Dimensions**: `[1, num_detections, 4]`
  - **Format**: `[ymin, xmin, ymax, xmax]` normalized coordinates in range `[0.0, 1.0]`
- **Tensor 1 (`classes`)**:
  - **Dimensions**: `[1, num_detections]`
  - **Format**: `Float32` or `Int` class index (e.g., 0 = person in COCO)
- **Tensor 2 (`scores`)**:
  - **Dimensions**: `[1, num_detections]`
  - **Format**: `Float32` confidence score in range `[0.0, 1.0]`
- **Tensor 3 (`num_detections`)**:
  - **Dimensions**: `[1]`
  - **Format**: `Float32` scalar representing the number of valid detections returned

## Setup Instructions

1. Obtain a compatible `.tflite` model (such as `ssd_mobilenet_v1_coco.tflite` under Apache 2.0 license).
2. Place the file into the Android assets directory:
   `app/src/main/assets/<your_model>.tflite`
3. Launch the app and go to **Settings > Model Integration**.
4. Set the model asset filename to `<your_model>.tflite`.
5. Go to **CV Lab**, import a test image, and tap **RUN DETECTION PIPELINE**.

## Handling Unavailable Models & Graceful Degradation

If no model is configured or the asset file is missing:
- The detector clearly indicates an **Unavailable / Not Loaded** state.
- The pipeline provides clear diagnostic instructions rather than crashing or fabricating synthetic results.
- When synthetic mode is active, all detections are prominently watermarked and badged with `[SYN]`.
