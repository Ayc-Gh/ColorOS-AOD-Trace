#!/system/bin/sh

INPUT="$1"
if [ -z "$INPUT" ] || [ ! -f "$INPUT" ]; then
  echo "Usage: sh summarize-aod-trace.sh /path/to/aod-trace-*.log"
  exit 1
fi

OUT="${INPUT%.log}-timeline.txt"

{
  echo "ColorOS AOD Trace timeline"
  echo "Source: $INPUT"
  echo "Generated: $(date)"
  echo
  echo "=== Session / state timeline ==="
  grep -E 'AOD_Trace: (SESSION_START|SESSION_END|TRACE_ENTER|FIELD_DIFF|TRACE_THROW)' "$INPUT" |
    grep -E 'SESSION_|Panoramic|AodBlackLayout|AODDisplayUtil|BaseDisplayUtil|OplusDozeServiceExImpl|DozeMachine|DozeScreenState|DreamService|Wake|Fingerprint|OFF\(1\)|DOZE_SUSPEND\(4\)|FIELD_DIFF'
  echo
  echo "=== First OFF / FINISH candidates ==="
  grep -n -E 'OFF\(1\)|State\.FINISH|FINISH|onDreamingStopped|finish\(' "$INPUT" | head -80
  echo
  echo "=== Errors ==="
  grep -n -E 'TRACE_THROW|INSTALL_FAILED| E AOD_Trace:' "$INPUT" || true
} > "$OUT"

echo "$OUT"
