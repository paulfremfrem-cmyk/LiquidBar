<h1 align="center"><strong>LiquidBar</strong></h1>

<p align="center">
  <strong>Next-Generation Floating Capsule & iOS Liquid Glass Navigation Bars for Android</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=flat-square&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Root-LSPosed-purple?style=flat-square" alt="LSPosed">
  <img src="https://img.shields.io/badge/License-AGPL--3.0-yellow?style=flat-square" alt="License">
</p>

---

## Overview

**LiquidBar** is a lightweight, dedicated LSPosed module that injects modern, fluid bottom navigation bars into your everyday Android applications. 

Unlike bloated system modification suites, **LiquidBar** focuses exclusively on delivering stunning, silky smooth navigation bars without touching the Android System Server, SystemUI, or Launcher. This ensures **100% system stability** and completely eliminates the risk of triggering LSPosed Safe Mode.

---

## ✨ Features

- **Dual Navigation Styles**:
  - 💊 **HyperOS Floating Capsule**: Minimalist, elevated floating pill with smooth touch feedback and theme adaptation.
  - 🪟 **iOS Liquid Glass**: Realistic frosted glass effect with real-time backdrop blur, specular refraction, and fluid interaction physics.
- **Supported Applications**:
  - 💬 **WhatsApp**: Replaces the native navigation bar with an interactive capsule / frosted glass bar for Chats, Updates, Communities, and Calls.
  - 🛍️ **Google Play Store**: Replaces standard tabs with a modern floating capsule bar.
  - ✉️ **Gmail**: Complete navigation overhaul including **Mail**, floating **Compose (Rédiger)**, and **Meet** tabs. Features zero-latency instant transitions and seamless unified dark/light headers between activities.
- **Per-App Customization**:
  - Switch styles independently for each app.
  - Accent color picker with dynamic Monet tinting or custom hex colors.
  - Configurable blur radius and material intensity.
  - Display modes: Icons & Text, Icons only, or Text only.
- **Ultra-Safe & Lightweight**:
  - Isolated application scope (no hooks on `system_server`, `com.android.systemui`, or launcher).
  - Fast, modular compilation with clean Kotlin view hooks.

---

## 🛠️ Installation & Usage

1. **Prerequisites**:
   - Android 14 / Android 15 (HyperOS, AOSP, OneUI, Pixel OS, etc.)
   - Root access via Magisk, KernelSU, or APatch.
   - LSPosed (Zygisk or modern fork) installed and active.

2. **Installation**:
   - Clone the repository:
     ```bash
     git clone https://github.com/paulfremfrem-cmyk/LiquidBar.git
     cd LiquidBar
     ```
   - Build the debug APK:
     ```bash
     ./gradlew assembleDebug
     ```
   - Install the APK on your device:
     ```bash
     adb install -r app/build/outputs/apk/debug/app-debug.apk
     ```
   - Open **LSPosed**, enable the **LiquidBar** module, and ensure your target apps (WhatsApp, Play Store, Gmail) are checked in the module scope.
   - Launch **LiquidBar** from your app drawer to pick your preferred styles and colors!

---

## 🤝 Credits & Acknowledgements

- **[HyperChanger](https://github.com/ColdP/HyperChanger)**: Built upon the remarkable foundational navigation architecture, MIUIX Compose components, and Xposed hook patterns originally created by **ColdP** and the HyperChanger community under the AGPL-3.0 license.
- **Antigravity (Google DeepMind)**: Co-developed, refined, and optimized in pair programming with **Antigravity**, an agentic AI coding assistant created by the Google DeepMind team.

---

## 📄 License

This project is licensed under the [GNU Affero General Public License v3.0 (AGPL-3.0)](LICENSE).
