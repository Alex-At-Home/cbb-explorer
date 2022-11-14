#!/bin/sh

launchctl remove com.piggott.cbb_import.plist
cp $PBP_SRC_ROOT/artefacts/macos-scripts/com.piggott.cbb_import.plist /Library/LaunchAgents 
launchctl load com.piggott.cbb_import.plist 