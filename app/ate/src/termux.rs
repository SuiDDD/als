use std::io::{self, Write};
use std::process::Command;
use std::thread;
use std::time::Duration;
use std::fs;
pub fn execute_termux() {
    let _ = Command::new("appops").args(["set", "com.termux.window", "SYSTEM_ALERT_WINDOW", "allow"]).status();
    let _ = Command::new("am").args(["start", "-n", "com.termux.window/com.termux.window.TermuxFloatActivity"]).status();
    thread::sleep(Duration::from_millis(90));
    let mut stdout = io::stdout();
    let _ = stdout.write_all(b"\x1bc\x1b[2J\x1b[3J\x1b[H");
    let _ = stdout.flush();
    let pid_output = Command::new("pidof").arg("com.termux.window").output().unwrap();
    let pid_string = String::from_utf8_lossy(&pid_output.stdout).trim().to_string();
    let environ_data = fs::read(format!("/proc/{pid_string}/environ")).unwrap();
    let mut dex_classpath = String::new();
    let mut boot_classpath = String::new();
    for entry_bytes in environ_data.split(|&byte| byte == 0) {
        let entry = String::from_utf8_lossy(entry_bytes);
        if entry.starts_with("DEX2OATBOOTCLASSPATH=") { dex_classpath = entry.to_string(); }
        else if entry.starts_with("BOOTCLASSPATH=") { boot_classpath = entry.to_string(); }
    }
    let uid_output = Command::new("sh").arg("-c").arg("grep '^com.termux ' /data/system/packages.list | awk '{print $2}'").output().unwrap();
    let termux_uid = String::from_utf8_lossy(&uid_output.stdout).trim().to_string();
    let environment_vars = format!("export SHELL=/data/data/com.termux/files/usr/bin/bash COLORTERM=truecolor HISTCONTROL=ignoreboth PREFIX=/data/data/com.termux/files/usr TERMUX_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE=false PWD=/data/data/com.termux/files/home TERMUX__HOME=/data/data/com.termux/files/home EXTERNAL_STORAGE=/sdcard LD_PRELOAD=/data/data/com.termux/files/usr/lib/libtermux-exec-ld-preload.so HOME=/data/data/com.termux/files/home LANG=en_US.UTF-8 {dex_classpath} TMPDIR=/data/data/com.termux/files/usr/tmp ANDROID_DATA=/data TERMUX__PREFIX=/data/data/com.termux/files/usr TERMUX_APP__SE_FILE_CONTEXT=u:object_r:app_data_file:s0:c170,c256,c512,c768 ANDROID_STORAGE=/storage TERM=xterm-direct TERMUX_APP__IS_DEBUGGABLE_BUILD=true ASEC_MOUNTPOINT=/mnt/asec TERMUX_APP__LEGACY_DATA_DIR=/data/data/com.termux ANDROID_I18N_ROOT=/apex/com.android.i18n SHLVL=1 SHELL_CMD__APP_TERMINAL_SESSION_NUMBER_SINCE_APP_START=0 ANDROID_ROOT=/system {boot_classpath} TERMUX_APP__APK_RELEASE=GITHUB ANDROID_TZDATA_ROOT=/apex/com.android.tzdata SHELL_CMD__PACKAGE_NAME=com.termux PATH=/data/data/com.termux/files/usr/bin TERMUX_APP__DATA_DIR=/data/user/0/com.termux ANDROID_ART_ROOT=/apex/com.android.art ANDROID_ASSETS=/system/app SHELL_CMD__APP_TERMINAL_SESSION_NUMBER_SINCE_BOOT=0; /data/data/com.termux/files/usr/bin/login");
    let _ = Command::new("su").args(["-M", &termux_uid, "-G", "3003", "-G", "3004", "-G", "3005", "-c", &environment_vars]).status();
}