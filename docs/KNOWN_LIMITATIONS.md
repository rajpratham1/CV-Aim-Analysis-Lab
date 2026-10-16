# Known Limitations & Research Boundaries

This document outlines architectural constraints, hardware limitations, and operational boundaries of the CV Aim Analysis Lab.

## 1. Operating Environment & Simulation Scope
- **Standalone Simulation**: The app operates entirely within its own Android process. It does not monitor, inject, overlay, or interact with external mobile games.
- **2D Hitbox Simplification**: Hitboxes in the training simulator use 2D Euclidean circular approximations (head radius, body radius, and relative vertical offsets). While fast and mathematically pure for 60 fps simulation on mobile devices, they do not account for full 3D skeletal meshes or occluded posture deformations.

## 2. On-Device Computer Vision Constraints
- **Inference Latency**: Real-time object detection depends heavily on the device SoC. On mid-range devices without hardware acceleration (NNAPI or GPU delegates), CPU inference on float32 models can range from 40ms to 150ms per frame.
- **Model Bundling**: To comply with distribution policies and avoid bloated repository sizes, full multi-megabyte binary `.tflite` model files are not bundled by default. Users must provide their own compatible model file via the assets folder.
- **Static Image Pipeline vs. Live Camera**: The current CV Lab supports high-resolution static image analysis and simulated video frame sequences. Direct CameraX streaming can be integrated with TFLite image analysis delegates in future expansions.

## 3. Multi-Object Tracking Limitations
- **Occlusion Handling**: The 1-D Kalman filter and greedy Hungarian-distance association handle short-term track loss up to `maxMissingFrames`. Extended occlusions or intersecting trajectories with identical kinematics may cause identity switches.
- **Linear Motion Assumption**: Constant-velocity motion models assume targets do not execute extreme erratic high-frequency jerks within sub-frame intervals.
