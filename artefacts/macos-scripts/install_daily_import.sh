#!/bin/sh

launchctl remove com.piggott.cbb_import.plist
sudo cp $PBP_SRC_ROOT/artefacts/macos-scripts/com.piggott.cbb_import.plist /Library/LaunchAgents 
launchctl load -w /Library/LaunchAgents/com.piggott.cbb_import.plist 