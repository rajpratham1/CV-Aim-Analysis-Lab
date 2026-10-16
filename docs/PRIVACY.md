# Privacy Policy & Ethical Boundaries

**CV Aim Analysis Lab** is an independent, offline research tool and simulated laboratory designed for computer vision study, target tracking analysis, and shooting accuracy evaluation.

## 1. Zero External Data Collection
- **100% Offline**: The application does not connect to any remote server, backend API, analytics platform, crash reporter, or ad network.
- **No Telemetry**: No identifiers, usage metrics, IP addresses, or device fingerprints are recorded or transmitted.
- **Local Storage Only**: Any session statistics, experiment configurations, or logs remain strictly on the local device and are destroyed when the app is uninstalled or user data is cleared.

## 2. On-Device Image Processing
- **Photo Import**: When a user selects an image via Android's photo picker (`ActivityResultContracts.GetContent()`), the image is processed in memory on the device.
- **No Uploads**: Imported images and video frames are never transmitted over the internet or uploaded to any third-party service.
- **Temporary In-Memory Buffers**: Bitmap objects and normalized pixel arrays are held in RAM only for the duration of the pipeline execution and discarded by the garbage collector.

## 3. Strict Simulation Boundary & Game Integrity
- **No Game Injection**: The application does not hook into, inject code into, overlay on, or interact with external games such as Free Fire, PUBG Mobile, Call of Duty: Mobile, or any other title.
- **No Input Manipulation / Accessibility Abuse**: The app does not request or use Android Accessibility Services, root access, device administration, or ADB input emulation to manipulate third-party software.
- **No Memory Reading**: The app does not inspect or tamper with foreign process memory spaces or Linux `/proc/[pid]/mem`.
- **Simulator-Only Aim Assist**: The aim assist feature is purely mathematical snap-to-target logic calculated exclusively inside the built-in 2D training arena (`SimulatorCanvas`). It has zero capability or intention to affect external apps.

## 4. Permissions
- `android.permission.READ_MEDIA_IMAGES` (API 33+) / `READ_EXTERNAL_STORAGE` (API ≤ 32): Used exclusively to allow the user to select local images for the CV image analysis pipeline.

## 5. Security & Transparency
- All source code and computer-vision algorithms are open and inspectable within this repository.
