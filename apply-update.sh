#!/usr/bin/env bash
# Deletes everything that belongs to the removed wake-word code (Vosk, DSP, tile, widget, model, crash/diagnostics screen).
set -e
W=app/src/main/java/com/arkitak/lite/wake
rm -f $W/DspHotwordEngine.kt $W/DspCallback.kt $W/CrashReporter.kt $W/Diagnostics.kt $W/ManualTriggerEngine.kt \
      $W/TriggerComponents.kt $W/VoskEngine.kt $W/VoskModelStore.kt $W/VoskWakeService.kt $W/WakeEngine.kt \
      $W/WakeEngineManager.kt $W/WakeSettingsActivity.kt
rm -f app/src/main/res/layout/widget_trigger.xml app/src/main/res/xml/arkitak_widget_info.xml
rm -rf app/src/main/assets/model-hi
rm -f setup-vosk-model.sh WAKE_README.md
echo "old wake-word files removed"
