#!/system/bin/sh

OUT_DIR="/storage/emulated/0/Documents/ColorOS-AOD-Trace/log"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT_FILE="$OUT_DIR/aod-full-trace-$STAMP.log"
LOG_PID=""

mkdir -p "$OUT_DIR" || exit 1
logcat -c

write_header() {
  {
    echo "ColorOS AOD Trace full-chain capture"
    echo "Started: $(date)"
    echo "Output: $OUT_FILE"
    echo "Device: $(getprop ro.product.manufacturer) $(getprop ro.product.model)"
    echo "Android: $(getprop ro.build.version.release) SDK=$(getprop ro.build.version.sdk)"
    echo "Build: $(getprop ro.build.display.id)"
    echo "Fingerprint: $(getprop ro.build.fingerprint)"
    echo "--- initial power snapshot ---"
    dumpsys power 2>/dev/null | grep -E 'mWakefulness|mWakefulnessChanging|mIsPowered|mPlugType|Display Power' | head -80
    echo "--- initial display snapshot ---"
    dumpsys display 2>/dev/null | grep -E 'mDisplayState|mState=|DisplayDeviceInfo|modeId|renderFrameRate' | head -120
    echo "--- trace begins ---"
  } >> "$OUT_FILE"
}

cleanup() {
  trap - INT TERM EXIT
  if [ -n "$LOG_PID" ]; then
    kill "$LOG_PID" 2>/dev/null
    wait "$LOG_PID" 2>/dev/null
  fi
  {
    echo "--- trace ends ---"
    echo "Stopped: $(date)"
    echo "--- final power snapshot ---"
    dumpsys power 2>/dev/null | grep -E 'mWakefulness|mWakefulnessChanging|mIsPowered|mPlugType|Display Power' | head -80
    echo "--- final display snapshot ---"
    dumpsys display 2>/dev/null | grep -E 'mDisplayState|mState=|DisplayDeviceInfo|modeId|renderFrameRate' | head -120
  } >> "$OUT_FILE"
  echo
  echo "Saved: $OUT_FILE"
}

trap cleanup INT TERM EXIT
write_header

echo "Capturing. Reproduce one AOD session, then press Ctrl+C."
echo "Output: $OUT_FILE"

logcat -v threadtime   AOD_Trace:*   AOD_Enhance:*   DozeMachine:*   DozeService:*   DozeScreenState:*   AODDisplayUtil:*   BaseDisplayUtil:*   OplusDozeServiceExImpl:*   '*:S' >> "$OUT_FILE" &
LOG_PID=$!
wait "$LOG_PID"
