# **Product Requirements Document (PRD): Minimalist Clock Overlay**

## **1. Executive Summary**

**Project Name:** ChronoFloat

**Objective:** To provide an ultra-minimalistic, floating date and time overlay for Android that mimics the system status bar style but remains visible across all applications.

**Platform:** Android (React Native)

## **2. Problem Statement**

Users in full-screen applications (games, video players, or immersive readers) often lose track of time because the Android system status bar is hidden. Pulling down the notification shade is disruptive. This app provides a persistent, non-obtrusive time reference.

## **3. User Personas**

* **The Gamer:** Needs to keep track of time during full-screen gameplay without exiting the app.  
* **The Content Consumer:** Watches long-form videos or reads e-books and wants a subtle clock in the corner.  
* **The Efficiency Enthusiast:** Prefers a custom-formatted date/time string that is always visible.

## **4. Feature Requirements**

### **4.1 Core Functionality (MVP)**

* **Persistent Overlay:** A single-line text element that floats over other apps.  
* **Real-time Update:** Time must update every minute (or second, if configured) with high accuracy.  
* **Single-Line Format:** Displays as \[Day\], \[Month\] \[Date\] | \[Time\] (e.g., *Mon, Oct 12 | 14:30*).  
* **Draggable Positioning:** Users can drag the overlay to any corner or edge of the screen to prevent it from obstructing UI elements in other apps.

### **4.2 UI/UX Requirements**

* **Mimic System Aesthetics:** Use a sans-serif font (Roboto/Inter) with a slight text shadow or semi-transparent background to ensure legibility on any background.  
* **Minimal Main App:** The main application interface only contains:  
  * A toggle switch to Start/Stop the service.  
  * A "Grant Permission" button (for SYSTEM\_ALERT\_WINDOW).  
  * Basic formatting options (12h vs 24h).

### **4.3 Technical Requirements**

* **React Native Bridges:** Since React Native doesn't natively support system-level overlays, a **Native Module** or library (like react-native-can-measure or react-native-floating-bubble logic) is required to interact with Android's WindowManager.  
* **Android Permissions:** Must handle the android.permission.SYSTEM\_ALERT\_WINDOW (Draw over other apps) permission.  
* **Foreground Service:** Implementation of a Foreground Service to ensure the overlay isn't killed by Android's battery optimization while the user is in another app.

## **5. User Flow**

1. **Onboarding:** User opens the app; if permission is missing, the app explains why it's needed and provides a shortcut to system settings.  
2. **Activation:** User toggles "Enable Overlay."  
3. **Interaction:** The clock appears at the top right (default). User can long-press and drag it to a new location.  
4. **Deactivation:** User returns to the main app to toggle it off, or taps a "Close" button in the persistent notification associated with the foreground service.

## **6. Design Specifications**

* **Font Size:** 12pt \- 14pt (standard status bar height).  
* **Opacity:** 85% opacity for text to feel "integrated."  
* **Interaction:** Pass-through touch (clicks should go to the app *behind* the clock) except when the user intentionally drags it.

## **7. Success Metrics**

* **Retention:** Users keeping the service active for more than 24 hours.  
* **Performance:** App consuming less than 1% of battery life per 12-hour cycle.  
* **Latency:** Time update occurs within \+/- 500ms of the system clock.

## **8. Risks & Constraints**

* **OEM Restrictions:** Some Android skins (MIUI, ColorOS) have aggressive background restrictions that may require the user to manually "Lock" the app in the task switcher.  
* **Security:** Users are often wary of "Draw over other apps" permissions; the PRD emphasizes a "Privacy First" policy (no internet permission required).