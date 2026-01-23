@echo off
echo ========================================
echo      HaramShield Auto-Launcher
echo ========================================

echo [1/4] Checking for device...
adb wait-for-device
echo Device connected.

echo [2/4] Installing APK...
call .\gradlew.bat installDebug
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Installation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo [3/4] Starting App...
adb shell am start -n com.haramshield.debug/com.haramshield.ui.MainActivity

echo [4/4] Opening Accessibility Settings...
echo Please ENABLE 'HaramShield' and 'HaramShield Brain' services.
adb shell am start -a android.settings.ACCESSIBILITY_SETTINGS

echo ========================================
echo      Launch Complete. Stay clean.
echo ========================================
pause
