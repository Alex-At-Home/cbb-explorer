#!/bin/sh

sudo cp $PBP_SRC_ROOT/artefacts/macos-scripts/com.piggott.cbb_import.plist /Library/LaunchAgents 

# Old:
#launchctl remove com.piggott.cbb_import.plist
#launchctl unload  /Library/LaunchAgents/com.piggott.cbb_import.plist 
#launchctl load -w /Library/LaunchAgents/com.piggott.cbb_import.plist 

# New - this still runs as root for some reason, but that is now handled in daily_cbb_import.sh
USERID=$(id -u)
launchctl bootout gui/$USERID /Library/LaunchAgents/com.piggott.cbb_import.plist 
launchctl bootstrap gui/$USERID /Library/LaunchAgents/com.piggott.cbb_import.plist 

# Show information to confirm it's been loaded:
launchctl print gui/$USERID/com.piggott.cbb_import.plist 