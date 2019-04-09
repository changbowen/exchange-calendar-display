## Android Tablet Setup
#### The guide only applies to the setup of the automation script on Android devices. The display of the web content does not rely on the script.
#### The setup is not tested on many different Android versions or models.

### Preparation
- Download android-platform-tools from [the official release page](https://developer.android.com/studio/releases/platform-tools). The package provides the adb and fastboot binaries.
- If you are using Windows, you need to get the Android USB drivers from the OEM site. A list of the sites can be found [here](https://developer.android.com/studio/run/oem-usb#Drivers). When your device does not exist in the list, remember **there is always a way to find the drivers**.
- Stock ROM for the tablet from the OEM either by downloading from the website or directly contact.
- Get the APKs: Chrome, Magisk Manager (if you are using Magisk), and optionally a terminal emulator like Terminal Emulator or Termux.
- Get the project files: `MRCD.sh` and the Magisk module zip file. (if you are using Magisk).
- On the PC and account that will be remotely managing the tablet, get the content of `~/.android/adbkey.pub` and replace the echo content with it under the line `## add adb keys for remote debugging`.
- Get the local html file for the meeting room. For e.g. `Sample_Room.html`.

### Android Setup (Recommended with Magisk)
1. Root the tablet with Magisk.
    1. Get Magisk Manager apk.
    2. On your PC, find the `boot.img` file inside the stock ROM and extract it from the ROM file.
    3. Connect to the tablet by USB and copy the Magisk Manager APK and the `boot.img` file to the tablet's local storage.
    4. Install Magisk Manager to the tablet. Connect the tablet to Internet if not already. Magisk Manager needs internet the first time to work.
    5. Open Magisk Manager on the tablet, update if prompted, choose Install, and then choose Patch Boot Image File. Select the `boot.img` file and wait.
    6. Copy the `patched_boot.img` file back to your PC. If you cannot find the patched file on the tablet, use the command adb pull to pull the file from the tablet.
        > `boot.img` is device-specific. You can use the patched file on other tablets that are the exact same model, saving the trouble of going through the patching steps.
    7. On the tablet, enable Developer Options, OEM Unlock and USB Debugging. Accept any prompts that follow.
    8. On your PC, open terminal / command prompt, cd to the adb tools and type these commands:
        ```bash
        adb reboot bootloader
        # wait for tablet till you see FASTBOOT mode on screen
        fastboot flash boot [path_to_your_patched_boot.img_file]
        # wait for it to complete
        fastboot reboot
        ```
    9. After reboot, verify with `adb shell su` to see if the tablet is rooted. You should get a prompt about super user on the tablet if it is working. You need to accept the prompt so the shell can have root access in the future for maintenance purposes.
2. Install customizations.
    1. (If not already done) Transfer all necessary files to `/sdcard/MRCD` on the tablet: Chrome APK, Magisk Manager APK, the Magisk module zip file, `MRCD.sh` and the html file.
        >Please mind the name of the downloaded HTML file. For e.g. MeetingRoom_1A.html. The name will be used in the script to update the tablet's hostname.
    2. Install Chrome and Magisk Manager if not already installed.
    3. Connect the tablet to the network where the hosting server is accessible. It is recommended to use cable with fixed IP address. It is important that this step is done before the next.
        - If you are using WiFi anyway, make sure it is the only available network on the tablet (forget other networks if needed) and can automatically connect.
        - If you are using cable, disable other types of networks such as WiFi.
    4. Open Magisk Manager, open menu → Modules, install the Magisk module zip and reboot.
    5. After reboot, the script will run and display the calendar automatically.

### Android Setup (when Magisk does not work)
1. Root the tablet with your own magic.
    > When Magisk fails, you need to try other ways to root the tablet. While there are numerous ways to do it (such as SuperSU, or your ROM may even provides an option to root), different ways succeed with different ROMs. That's why there is not a universal method.
2. Configure a service manually by modifying the `boot.img`.
    > May not work under SELinux.
    1. Get the `boot.img` file that is being used on the tablet. The recommended way is to extract from the stock ROM. You can also try extracting from the tablet by following Google.
    2. Expand the `boot.img` file to a directory where you will find a file named `init.rc`.
    3. Add the below lines to the `init.rc` file in the expanded directory. It defines a custom service that runs the `MRCD.sh` file on sdcard once the system is booted.
        ```bash
        service mrcd /system/bin/sh -c ". $EXTERNAL_STORAGE/MRCD/MRCD.sh &> $EXTERNAL_STORAGE/MRCD/MRCD_$(date +%Y-%m-%dT%T%z).log"
            user root
            group root
            disabled
            oneshot
         
        on property:sys.boot_completed=1
            start mrcd
         
        on property:dev.bootcomplete=1
            start mrcd
        ```
    4. Re-pack the `boot.img` file and flash the tablet with it.
3. Configure customization.
    1. Install Chrome.
    2. Copy `MRCD.sh` and the HTML file to `/sdcard/MRCD/` on the tablet.
    3. Connect power & network and reboot to see the script working.

### Automation Script Behavior
The monitoring bash script runs on the background to try to always keep the calendar displayed fullscreen. Additionally the script will do the following:

- Apply customization for the purpose of the tablet.
- Generate device hostname based on the name of the HTML file.
- Keep the tablet awake.
- Check the top level app and if it is not Chrome or Magisk, exit and relaunch Chrome and open the calendar.
- If the battery is below 10% and not charging, shut down the tablet.
- Log actions to external storage.
- Lock all inputs after 30 mins as a security feature. Rebooting the device clears the lock.
- If the script file is deleted or inaccessible, shut down the tablet after 30 mins.

### Maintenance
While the initial setup of the Android device (rooting etc.) can be troublesome, the solution is designed to be maintenance-free.

30 minutes after boot, all existing inputs (buttons, touch screen, keyboard...) will be locked and the only way to manage the device is ADB over TCP/IP. New input devices can still be connected and used such as external keyboards and mouse via OTG (USB).

Also some devices may provide a reset key that can be used to reboot the device locally when all the inputs are locked.

It is recommended to install a terminal emulator on the tablet. When all inputs are disabled, you can still use a USB keyboard to reboot the tablet from a terminal.

If Magisk is used, it is possible to disable the script from Magisk Manager (needs reboot) for maintenance activities to be done locally on the device.

#### Connecting to the tablet via ADB over TCP/IP
To connect to the ADB service on the tablet, from the PC and user whose public adb key is already added to `/data/misc/adb/adb_keys` on the tablet to be managed, assuming the IP of the tablet is 10.0.0.100, use the below commands.
```bash
adbusr@WebSrvTest:~$ adb connect 10.0.0.100
connected to 10.0.0.100:5555
adbusr@WebSrvTest:~$ adb devices
List of devices attached
10.0.0.100:5555 device

# if you see the above 2 lines, you are successfully connected.
# then you can connect to the shell on the tablet and switch to super user.
adbusr@WebSrvTest:~$ adb shell
a10s_m3h3:/ $ 
a10s_m3h3:/ $ su
a10s_m3h3:/ # 

# if you see the above line you are already inside the tablet.
# various commands can be executed to manipulate the tablet.
# inputs can also be simulated even if the inputs are locked.
```

Some useful adb commands can be found [here](https://gist.github.com/Pulimet/5013acf2cd5b28e55036c82c91bd56d8).

#### Updating the Automation Scripts
The automation consists of two scripts. The first script is installed with the Magisk module (the zip file) and cannot be updated remotely. In case an update is needed, it needs to be done by first pushing the zip file to the tablet and then reinstall the module from Magisk Manager app. This script contains only few basic customization actions to enable remote debugging and then it calls the second script.

The second script contains everything else as described in the previous section and can be updated remotely by pushing the `MRCD.sh` file to the tablet and reboot.
