#!/usr/bin/env bash
START_TIME=$(date -u +%s)
HEX_TIME=$(printf '%x\n' $START_TIME)
GUID=$(dd if=/dev/random bs=12 count=1 2>/dev/null | od -An -tx1 | tr -d ' \t\n')
TRACE_ID="1-$HEX_TIME-$GUID"
SEGMENT_ID=$(dd if=/dev/random bs=8 count=1 2>/dev/null | od -An -tx1 | tr -d ' \t\n')
sleep 1s
END_TIME=$(date -u +%s)

SEGMENT_DOC="{\"trace_id\": \"$TRACE_ID\", \"id\": \"$SEGMENT_ID\", \"start_time\": $START_TIME, \"end_time\": $END_TIME, \"in_progress\": false, \"name\": \"sample client\"}"
echo "trace id: $TRACE_ID"
echo "segement doc: $SEGMENT_DOC"
aws xray put-trace-segments --trace-segment-documents "$SEGMENT_DOC"