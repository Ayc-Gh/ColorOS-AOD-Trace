#!/system/bin/sh

FILE="$1"

if [ -z "$FILE" ]; then
  echo "Usage: sh summarize-aod-trace.sh /path/to/aod-full-trace-*.log"
  exit 1
fi

if [ ! -f "$FILE" ]; then
  echo "File not found: $FILE"
  exit 1
fi

echo "=== Sessions ==="
grep -E 'AOD_SESSION_(START|END)' "$FILE"

echo
echo "=== Structured timeline ==="
grep 'AODT|' "$FILE" |   grep -E 'PanoramicAodController|AodBlackLayout|AODDisplayUtil|BaseDisplayUtil|OplusDozeServiceExImpl|DozeService|DozeMachine|DozeScreenState|AodUpdateManager|WakeUp|Fingerprint|DreamService'

echo
echo "=== OFF / DOZE / FINISH / visibility changes ==="
grep -E 'OFF\(1\)|DOZE\(3\)|DOZE_SUSPEND\(4\)|FINISH|visibility|alpha|isShowing|mIsShowing|AOD_SESSION_END' "$FILE"
