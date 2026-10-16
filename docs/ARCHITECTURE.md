# System Architecture

## Overview
**CV Aim Analysis Lab** is structured into distinct, modular layers adhering to modern Android architecture principles (Jetpack Compose, Kotlin Coroutines, StateFlow, Clean Architecture separation of domain logic from UI).

```
                      +-----------------------------+
                      |         UI Layer            |
                      |   (Jetpack Compose M3)      |
                      +--------------+--------------+
                                     |
              +----------------------+----------------------+
              |                      |                      |
     +--------v---------+   +--------v---------+   +--------v---------+
     | SimulatorViewMod |   | VisionViewModel  |   | TrackingViewMod  |
     +--------+---------+   +--------+---------+   +--------+---------+
              |                      |                      |
              |             +--------v---------+            |
              |             |  Detector Engine |            |
              |             |  (TFLite / Synth)|            |
              |             +--------+---------+            |
              |                      |                      |
     +--------v---------+            |             +--------v---------+
     |  SimulatorCore   |            |             | MultiObjTracker  |
     | (Physics/Hitbox) |            |             |  (Kalman/Assoc)  |
     +--------+---------+            |             +--------+---------+
              |                      |                      |
              +----------------------+----------------------+
                                     |
                            +--------v--------+
                            | Geometry Core   |
                            | (Vec2, Box, IoU)|
                            +-----------------+
```

## Packages

### 1. `com.example.cvlab.geometry`
- **`Vec2`**: 2D vector class with arithmetic operations, distance, magnitude, normalization, and linear interpolation.
- **`BoundingBox`**: Axis-aligned bounding box with intersection, IoU (Intersection-over-Union), scaling, and containment checks.
- **`scaleBox`**: Letterbox-aware scaling function for mapping coordinates between arbitrary image resolutions and screen bounds.
- **Hitbox Math**: Circular and composite head/body collision routines.

### 2. `com.example.cvlab.simulator`
- **`SimTarget`**: Represents a simulated moving target with body and head hitbox radii.
- **`SimulatorCore`**: Pure Kotlin simulation engine (collision classification, velocity reflection, aim assist calculation).
- **`SimulatorViewModel`**: Reactive game loop (~60 fps) updating state flows for targets, shots, and hit metrics.

### 3. `com.example.cvlab.detection`
- **`Detector`**: Asynchronous detector interface with contract for RGB buffer input, confidence thresholds, and detection results.
- **`DetectionResult`**: Bounding box, confidence score, class ID, and explicit source tag (`MODEL` vs. `SYNTHETIC`).
- **`TFLiteDetector`**: On-device TensorFlow Lite model loader, input tensor preprocessor, output decoder, and error handler.
- **`SyntheticDemoDetector`**: Clearly labeled fallback generator when no model file is provided.

### 4. `com.example.cvlab.tracking`
- **`MultiObjectTracker`**: Multi-target tracking algorithm matching detections to existing tracks via Euclidean distance gating.
- **`KalmanFilter1D`**: State prediction and measurement update for constant-velocity smoothing.
- **`computePositionError`**: Ground-truth validation metric comparing predicted track trajectories against true target paths.

### 5. `com.example.cvlab.ui`
- Compose screens: `SimulatorScreen`, `VisionLabScreen`, `TrackingScreen`, `AnalyticsScreen`, `SettingsScreen`, `DashboardScreen`, `DocsScreen`.
