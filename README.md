# ActivitiesPrediction
ESE 440/441 project

# useful adb commands
```bash
# Connect wearable:
adb forward tcp:4444 localabstract:/adb-hub
adb connect 127.0.0.1:4444

# adb for wearable
adb -s 127.0.0.1:4444 <command>

# show all packages
adb shell pm list packages -f

# uninstall
adb shell pm uninstall -k <package name>
```
