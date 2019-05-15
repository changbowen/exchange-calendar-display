#!/system/bin/sh
#  --------------------------------------
# | MRCD Monitoring Script by Carl Chang |
# |           Ver. 20190515.2            |
#  --------------------------------------

## debugging lines

# -----------------------------
# - BASE FUNCTION DEFINITIONS -
# -----------------------------
## function to echo with date
logging() {
    echo "$(date +%Y-%m-%dT%T%z)\t$1"
}

## function to return to home screen
## ugly but easy and reliable
go_home() {
    input keyevent KEYCODE_ESCAPE
    input keyevent KEYCODE_BACK
    input keyevent KEYCODE_ESCAPE
    input keyevent KEYCODE_BACK
    input keyevent KEYCODE_ESCAPE
    input keyevent KEYCODE_BACK
    input keyevent KEYCODE_ESCAPE
    input keyevent KEYCODE_BACK
    input keyevent KEYCODE_ESCAPE
    input keyevent KEYCODE_BACK
    input keyevent KEYCODE_HOME
}


# ------------------
# - INITIALIZATION -
# ------------------
## wait till boot complete
while sleep 5; do
    if [ "$(getprop sys.boot_completed)" -eq "1" ] || [ "$(getprop dev.bootcomplete)" -eq "1" ]; then break; fi
done

## set root folder
MRCD_ROOT=$EXTERNAL_STORAGE/MRCD
mkdir -p "$MRCD_ROOT"

## add adb keys for remote debugging
echo "CONTENTS OF THE ~/.android/adbkey.pub FILE UNDER THE USER PROFILE THAT WILL BE USED TO MANAGE THE PAD" > /data/misc/adb/adb_keys
sleep 10

## optionally set NTP server if internet access is limited
#settings put global ntp_server server-in-your-org

## update hostname from html file name
#find $MRCD_ROOT/ -name *.html -exec basename {} \;
logging 'Updating hostname based on HTML file name...'
for file in $MRCD_ROOT/*.html; do
    meeting_room_name=$(basename $file)
    meeting_room_name=${meeting_room_name%.*}
    break
done
#local old_hostname=$(getprop net.hostname)
#local new_hostname=MRCD_${meeting_room_name}_${old_hostname: -4}
local new_hostname=MRCD_$meeting_room_name
setprop net.hostname $new_hostname
for i in $(ls /sys/class/net/) ; do
    local link_status=$(ip link show $i)
    link_status=${link_status##*state\ }
    link_status=${link_status%%\ *}
    logging "  - Link $i status is $link_status"
    if [ $link_status != "DOWN" ]; then
        logging "    - Flushing and restarting link $i"
        (ip addr flush $i; ip link set $i down; ip link set $i up) &
    fi
done
## wait a bit for ip renewal
sleep 10

## daily routine
(while true; do
    ### enable debugging
    settings put global development_settings_enabled 1
    settings put global adb_enabled 1

    ### toggle auto time to trigger NTP update
    settings put global auto_time_zone 1
    settings put global auto_time 0
    settings put global auto_time 1

    ### restart adbd on tcpip
    setprop service.adb.tcp.port 5555
    stop adbd
    start adbd

    ### enable install from unknown source
    settings put secure install_non_market_apps 1
    
    ### check for script updates (how...)
    
    ### wait a day
    sleep 86400
done) &


# ---------------------------------
# - EXTENDED FUNCTION DEFINITIONS -
# ---------------------------------
## function to reset chrome
reset_chrome() {
    logging 'Resetting Chrome...'
    pm clear com.android.chrome
    echo "chrome --no-default-browser-check --no-first-run --disable-fre" > /data/local/tmp/chrome-command-line
    chmod 777 /data/local/tmp/chrome-command-line
    am set-debug-app --persistent com.android.chrome
    pm grant com.android.chrome android.permission.READ_EXTERNAL_STORAGE
    am start -n com.android.chrome/com.google.android.apps.chrome.Main -a android.intent.action.VIEW -d file://$MRCD_ROOT/${meeting_room_name}.html --activity-clear-task
    sleep 20
    input tap 300 300
}

## function to get battery status
get_battery_status() {
    local btry_dump=$(dumpsys battery)
    btry_level=$(echo $btry_dump | sed -nE 's/.*level: *([[:digit:]]+).*/\1/p')
    btry_status=$(echo $btry_dump | sed -nE 's/.*status: *([[:digit:]]).*/\1/p')
    case $btry_status in
        1) btry_status='BATTERY_STATUS_UNKNOWN';;
        2) btry_status='BATTERY_STATUS_CHARGING';;
        3) btry_status='BATTERY_STATUS_DISCHARGING';;
        4) btry_status='BATTERY_STATUS_NOT_CHARGING';;
        5) btry_status='BATTERY_STATUS_FULL';;
    esac
}


# -----------------------
# - POST CUSTOMIZATIONS -
# -----------------------
## disable all existing inputs after 30 mins
## still work after the calling script exits
## resetting chrome after is because removing touchscreen cause chrome to exit from fullscreen mode
(sleep 1800; logging 'Disabling inputs...'; rm /dev/input/event*; reset_chrome) &

## disable sleep, rotate landscape, hide status and navigation bars
## setenforce to 0 otherwise pm list packages gives DeadObjectException
logging 'Changing system settings...'
setenforce 0
settings put system screen_off_timeout 172800000
#settings put system accelerometer_rotation 0
#settings put system user_rotation 1; different devices need different values
settings put global policy_control immersive.full=*
settings put system screen_brightness 50

## unlock if sleeping, mute
logging 'Unlock and mute...'
input keyevent KEYCODE_MENU
input keyevent KEYCODE_VOLUME_MUTE
sleep 2
go_home
sleep 2

## terminate if chrome is not installed
logging 'Checking Chrome installation...'
if [ "$(pm list packages | grep -q com.android.chrome; echo $?)" -eq "1" ]; then
    input text 'Please install Chrome...'
    logging 'Chrome not installed. Exiting...'
    exit 2
fi

## monitoring loop to check battery status and the top level package
## if it's not chrome, close it and launch chrome
logging 'Starting monitoring loop...'
while true; do
    ### keep screen on
    input keyevent mouse

    ### monitor battery status
    get_battery_status
    if [ "$btry_level" -lt "10" ] && [ "$btry_status" != 'BATTERY_STATUS_CHARGING' ]; then
        logging 'Low battery. Shutting down in 5 sec...'
        (sleep 5; reboot -p) &
        exit 2
    fi

    ### monitor chrome
    if [ "$(dumpsys window windows | grep mCurrentFocus | grep -qE 'com.android.chrome|com.topjohnwu.magisk'; echo $?)" -eq "1" ]; then
        input keyevent KEYCODE_MENU
        input keyevent KEYCODE_MENU
        sleep 2
        go_home
        sleep 2
        reset_chrome
    fi
	
    sleep 20
done
