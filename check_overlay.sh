#!/bin/bash

echo "=========================================="
echo "    ANCHOR OVERLAY DIAGNOSTIC TOOL"
echo "=========================================="
echo ""

# 1. Check Package ID from capacitor.config.json or build.gradle
echo "[1/5] Checking Configured Package Name..."
APP_ID=$(grep -i '"appId"' capacitor.config.json 2>/dev/null | cut -d '"' -f 4)
if [ -z "$APP_ID" ]; then
    APP_ID=$(grep -i "applicationId" android/app/build.gradle 2>/dev/null | awk '{print $2}' | tr -d '"')
fi

if [ -n "$APP_ID" ]; then
    echo "  -> Found Application ID: $APP_ID"
else
    echo "  [!] Could not automatically detect appId in root directory."
    APP_ID="com.anchor.app" # Default fallback
    echo "  -> Using default fallback: $APP_ID"
fi
echo ""

# 2. Check AndroidManifest.xml for Overlay & Service Declarations
echo "[2/5] Checking AndroidManifest.xml..."
MANIFEST=$(find android/app/src/main -name "AndroidManifest.xml" 2>/dev/null)

if [ -f "$MANIFEST" ]; then
    echo "  -> Manifest found at: $MANIFEST"
    
    if grep -q "SYSTEM_ALERT_WINDOW" "$MANIFEST"; then
        echo "  [✓] SYSTEM_ALERT_WINDOW permission is declared."
    else
        echo "  [X] MISSING: <uses-permission android:name=\"android.permission.SYSTEM_ALERT_WINDOW\" />"
    fi

    if grep -q "FOREGROUND_SERVICE" "$MANIFEST"; then
        echo "  [✓] FOREGROUND_SERVICE permission is declared."
    else
        echo "  [!] WARNING: FOREGROUND_SERVICE permission not found in manifest."
    fi
else
    echo "  [!] AndroidManifest.xml not found in standard directory."
fi
echo ""

# 3. Check Java/Kotlin files for WindowManager Overlay Type
echo "[3/5] Checking Java/Kotlin Overlay Window Parameters..."
JAVA_FILES=$(find android/app/src/main/java -type f \( -name "*.java" -o -name "*.kt" \) 2>/dev/null)

if [ -n "$JAVA_FILES" ]; then
    if grep -r "TYPE_APPLICATION_OVERLAY" android/app/src/main/java/ >/dev/null 2>&1; then
        echo "  [✓] TYPE_APPLICATION_OVERLAY is used for Android 8.0+ compatibility."
    else
        echo "  [X] WARNING: TYPE_APPLICATION_OVERLAY not detected in Java/Kotlin sources."
        echo "      Older types like TYPE_PHONE or TYPE_SYSTEM_ALERT fail on newer Android versions."
    fi
else
    echo "  [!] No Java/Kotlin source files found under android/app/src/main/java."
fi
echo ""

# 4. Check Installed Package Status
echo "[4/5] Checking if App is Installed on Device..."
INSTALLED=$(pm list packages | grep "$APP_ID")

if [ -n "$INSTALLED" ]; then
    echo "  [✓] Package $APP_ID is installed on this device."
else
    echo "  [X] Package $APP_ID is NOT installed on this device."
    echo "      Did you build and install the updated APK?"
fi
echo ""

# 5. Check Active Running Services
echo "[5/5] Checking Active Background Services..."
ACTIVE_SERVICES=$(dumpsys activity services "$APP_ID" 2>/dev/null | grep -i "ServiceRecord")

if [ -n "$ACTIVE_SERVICES" ]; then
    echo "  [✓] Background service is active:"
    echo "$ACTIVE_SERVICES"
else
    echo "  [!] No active background services found running for $APP_ID."
fi

echo ""
echo "=========================================="
echo "          DIAGNOSTICS COMPLETE"
echo "=========================================="
