#!/system/bin/sh

OUT_DIR="/storage/emulated/0/Documents/ColorOS-AOD-Trace"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT_FILE="$OUT_DIR/aod-trace-$STAMP.log"

mkdir -p "$OUT_DIR" || exit 1
logcat -c

{
  echo "ColorOS AOD Trace capture"
  echo "Started: $(date)"
  echo "Output: $OUT_FILE"
  echo "Device: $(getprop ro.product.manufacturer) $(getprop ro.product.model)"
  echo "Android: $(getprop ro.build.version.release) SDK=$(getprop ro.build.version.sdk)"
  echo "Build: $(getprop ro.build.display.id)"
  echo "ColorOS/Oplus ROM: $(getprop ro.build.version.oplusrom)"
  echo "--- power snapshot ---"
  dumpsys power 2>/dev/null | grep -E 'mWakefulness|mIsPowered|mPlugType|Display Power' | head -60
  echo "--- display snapshot ---"
  dumpsys display 2>/dev/null | grep -E 'mDisplayState|mOverrideState|state=|DisplayDeviceInfo' | head -80
  echo "--- trace begins ---"
} | tee "$OUT_FILE"

echo "Press Ctrl+C after the AOD session finishes."

exec logcat -v threadtime   AOD_Trace:*   DozeMachine:*   DozeService:*   DozeScreenState:*   AODDisplayUtil:*   BaseDisplayUtil:*   OplusDozeServiceExImpl:*   PanoramicAodController:*   AodUpdateManager:*   '*:S' >> "$OUT_FILE"
