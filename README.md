<div align="center">

<br/>

```
██╗   ██╗ ██████╗ ██╗     ██╗   ██╗███╗   ███╗███████╗
██║   ██║██╔═══██╗██║     ██║   ██║████╗ ████║██╔════╝
██║   ██║██║   ██║██║     ██║   ██║██╔████╔██║█████╗  
╚██╗ ██╔╝██║   ██║██║     ██║   ██║██║╚██╔╝██║██╔══╝  
 ╚████╔╝ ╚██████╔╝███████╗╚██████╔╝██║ ╚═╝ ██║███████╗
  ╚═══╝   ╚═════╝ ╚══════╝ ╚═════╝ ╚═╝     ╚═╝╚══════╝
S  L  I  D  E  R
```

### A sleek, glassmorphism-inspired floating volume controller for Android.

<br/>

[![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Gradle](https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white)](https://gradle.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)

<br/>

</div>

---

<br/>

## ✦ Overview

**VolumeSlider** is a floating overlay widget that lives on top of your apps, giving you instant, tactile control over media volume — without ever leaving what you're doing. Built with a glassmorphism aesthetic and smart edge-snapping behavior, it stays out of your way until you need it.

I personally use this because my phone's volume button is broken.

<br/>

## Features

<br/>

> **Glassmorphism UI**
>
> Modern frosted-glass aesthetics with semi-transparent surfaces, soft blur layers, and fluid rounded corners — designed to feel native on any wallpaper.

<br/>

> **Intellihide — Quick Ball**
>
> Inspired by MIUI's Quick Ball, VolumeSlider automatically snaps to the nearest screen edge and collapses into a subtle peek tab when idle. It's always there, never in the way.

<br/>

> **Zippy Dragging**
>
> Custom touch physics engineered for instant responsiveness. Zero lag, zero stutter — optimized for high-density displays and fast fingers.

<br/>

> **Native Volume Control**
>
> Directly integrates with Android's `AudioManager` API for seamless, low-latency media volume adjustments — no shell commands, no workarounds.

<br/>

---

<br/>

## Installation & Setup

### Prerequisites

- Android device or emulator running **API 26+**
- **USB Debugging** enabled (`Settings → Developer Options → USB Debugging`)
- [Android SDK](https://developer.android.com/studio) installed and configured

### Steps

**1. Clone the repository**

```bash
git clone https://github.com/YOUR_USERNAME/VolumeSlider.git
cd VolumeSlider
```

**2. Connect your device**

Plug in your Android device via USB and verify it's recognized:

```bash
adb devices
```

**3. Build & install**

```bash
./gradlew installDebug
```

**4. Grant overlay permission**

On first launch, VolumeSlider will prompt you to grant the **Display over other apps** permission. This is required for the floating overlay to function.

<br/>

---

<br/>

##  Architecture

```
VolumeSlider/
├── app/
│   ├── build/
│   ├── src/main/
│   │   ├── java/com/example/volumeslider/
│   │   │   ├── FloatingVolumeService.kt   # Core overlay service & touch logic
│   │   │   └── MainActivity.kt            # Entry point & permission launcher
│   │   └── res/
│   │       ├── drawable/                  # Shape & blur assets
│   │       ├── layout/                    # View layouts
│   │       ├── mipmap-anydpi-v26/         # Adaptive launcher icons
│   │       └── values/                    # Themes, strings, colors
│   ├── AndroidManifest.xml
│   └── build.gradle
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties          # Pins Gradle version
├── build.gradle
├── gradle.properties
├── gradlew                                # Unix build script
├── gradlew.bat                            # Windows build script
└── settings.gradle
```

<br/>

---

<br/>

## AI Disclaimer

This project was built and refined in close collaboration with **Gemini AI** — from architecting the floating service logic and nailing the edge-snapping physics, to polishing the glassmorphism UI layer and squashing stubborn bugs. Gemini served as a true co-pilot throughout the entire development process. If you find something fishy, feel free to contact me!

<br/>

---

<br/>

## License

```
MIT License — free to use, modify, and distribute.
See LICENSE for full terms.
```

<br/>

---

<div align="center">

Made with 🖤 and a lot of `AudioManager` calls.

<br/>

*If VolumeSlider saved your wrist from diving into the notification shade — leave a ⭐*

</div>