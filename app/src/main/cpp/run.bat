@echo off

set APP=EventReader
set ROOT=../../../build/intermediates/ndkBuild/debug/obj/local/armeabi-v7a
set INSTALL_DIR=/data/local/tmp

adb push %ROOT%/%APP% %INSTALL_DIR%/%APP%
adb shell chmod 777 %INSTALL_DIR%/%APP%

adb shell %INSTALL_DIR%/%APP% "/dev/input/event8" "/dev/input/event2"
pause
rem adb shell rm %INSTALL_DIR%/%APP%
