# Testing Documentation & Verification Suite

This document covers the test suite for **CV Aim Analysis Lab**, encompassing unit tests, physics verification, geometry tests, and tracker validation.

## Test Suite Summary

The unit tests run under standard JUnit 4 on the local JVM without requiring an emulator or physical device.

### 1. `GeometryTest.kt` (35 Tests)
- **Vector Operations**: Addition, subtraction, scalar multiplication, division, unary negation, dot product, normalization, magnitude.
- **Euclidean Distance**: Exact distance calculations, squared distance, and zero distance identity.
- **Linear Interpolation (Lerp)**: Point-to-point interpolation between bounds.
- **BoundingBox Operations**:
  - `contains()` point tests (inside, outside, boundary).
  - `overlaps()` intersection tests (disjoint, overlapping, contained).
  - `iou()` Intersection-over-Union calculation against known area ratios.
  - `fromCenter()` factory construction.
  - `scale()` uniform and anisotropic scaling.
  - `scaleBox()` with aspect-ratio preservation (`keepAspect = true`) verifying letterbox/pillarbox offsets.
- **Normalization**: Coordinate conversion to/from normalized `[0.0, 1.0]` space.
- **Collision Zones**: Verification of `classifyCircularHit` for head, body, and miss conditions.

### 2. `SimulatorCoreTest.kt` (42 Tests)
- **Shot Classification**:
  - Direct head hits, body hits, and misses.
  - Priority ordering: head hitbox takes precedence over body hitbox in overlapping regions.
  - Boundary conditions: edge hits vs. misses at `radius + epsilon`.
  - Multi-target scene classification and dead target exclusions.
- **Physics and Arena Bounds**:
  - Target velocity updates per tick.
  - Wall bouncing: reflection of `vx` on horizontal limits, reflection of `vy` on vertical limits.
  - Boundary clamping preventing targets from tunneling outside arena bounds.
- **Target Generation (`makeTargets`)**:
  - Correct target counts, unique incrementing IDs, and containment within arena safe area.
- **Session Metrics**: Accuracy percentage, head-hit ratio, and zero-shot division safety.
- **Aim Assist Calculations**:
  - Snapping to nearest head/body center within `snapRadius`.
  - Invariance when no target is within snap radius.

### 3. `TrackerTest.kt` (18+ Tests)
- **Track Lifecycle**:
  - Creation of tracks on new detections.
  - Track ID uniqueness and temporal persistence across consecutive frames.
  - Track confirmation threshold (`minHitsToConfirm`).
  - Track survival across missed detections up to `maxMissingFrames`.
  - Track expiration and pruning once miss threshold is exceeded.
- **State Estimation**:
  - Velocity estimation matching detection direction.
  - Position history recording.
- **Ground-Truth Comparison (`computePositionError`)**:
  - Error calculation against known ground-truth coordinates across multi-target scenes.
  - Association of predicted tracks to nearest ground-truth target within matching frames.
  - Zero-error identity for perfectly tracked targets.

## Running Tests

Run the full unit test suite from the terminal:
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test
```
HTML test reports will be generated at:
`app/build/reports/tests/testDebugUnitTest/index.html`
