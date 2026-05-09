use std::io::{self, Write};
use std::process::Command;
use std::thread;
use std::time::Duration;
use std::fs;
pub fn executeTermux() {
    let _ = Command::new("appops").args(["set", "com.termux.window", "SYSTEM_ALERT_WINDOW", "allow"]).status();
    let _ = Command::new("am").args(["start", "-n", "com.termux.window/com.termux.window.TermuxFloatActivity"]).status();
    thread::sleep(Duration::from_millis(90));
    let mut stdout = io::stdout();
    let _ = stdout.write_all(b"\x1bc\x1b[2J\x1b[3J\x1b[H");
    let _ = stdout.flush();
    let pidOutput = Command::new("pidof").arg("com.termux.window").output().unwrap();
    let pidString = String::from_utf8_lossy(&pidOutput.stdout).trim().to_string();
    let environData = fs::read(format!("/proc/{pidString}/environ")).unwrap();
    let mut dexClasspath = String::new();
    let mut bootClasspath = String::new();
    for entryBytes in environData.split(|&byte| byte == 0) {
        let entry = String::from_utf8_lossy(entryBytes);
        if entry.starts_with("DEX2OATBOOTCLASSPATH=") { dexClasspath = entry.to_string(); }
        else if entry.starts_with("BOOTCLASSPATH=") { bootClasspath = entry.to_string(); }
    }
    let uidOutput = Command::new("sh").arg("-c").arg("grep '^com.termux ' /data/system/packages.list | awk '{print $2}'").output().unwrap();
    let termuxUid = String::from_utf8_lossy(&uidOutput.stdout).trim().to_string();
    let environmentVars = format!("export SHELL=/data/data/com.termux/files/usr/bin/bash COLORTERM=truecolor HISTCONTROL=ignoreboth PREFIX=/data/data/com.termux/files/usr TERMUX_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE=false PWD=/data/data/com.termux/files/home TERMUX__HOME=/data/data/com.termux/files/home EXTERNAL_STORAGE=/sdcard LD_PRELOAD=/data/data/com.termux/files/usr/lib/libtermux-exec-ld-preload.so HOME=/data/data/com.termux/files/home LANG=en_US.UTF-8 {dexClasspath} TMPDIR=/data/data/com.termux/files/usr/tmp ANDROID_DATA=/data TERMUX__PREFIX=/data/data/com.termux/files/usr TERMUX_APP__SE_FILE_CONTEXT=u:object_r:app_data_file:s0:c170,c256,c512,c768 ANDROID_STORAGE=/storage TERM=xterm-direct TERMUX_APP__IS_DEBUGGABLE_BUILD=true ASEC_MOUNTPOINT=/mnt/asec TERMUX_APP__LEGACY_DATA_DIR=/data/data/com.termux ANDROID_I18N_ROOT=/apex/com.android.i18n SHLVL=1 SHELL_CMD__APP_TERMINAL_SESSION_NUMBER_SINCE_APP_START=0 ANDROID_ROOT=/system {bootClasspath} TERMUX_APP__APK_RELEASE=GITHUB ANDROID_TZDATA_ROOT=/apex/com.android.tzdata SHELL_CMD__PACKAGE_NAME=com.termux PATH=/data/data/com.termux/files/usr/bin TERMUX_APP__DATA_DIR=/data/user/0/com.termux ANDROID_ART_ROOT=/apex/com.android.art ANDROID_ASSETS=/system/app SHELL_CMD__APP_TERMINAL_SESSION_NUMBER_SINCE_BOOT=0; /data/data/com.termux/files/usr/bin/login");
    let _ = Command::new("su").args(["-M", &termuxUid, "-G", "3003", "-G", "3004", "-G", "3005", "-c", &environmentVars]).status();
}