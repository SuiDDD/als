use std::{path::Path, process::Command};
pub fn te_on(target_root: &Path) {
    let _ = Command::new("chroot").arg(target_root).arg("/usr/bin/env").args(&["-i", "HOME=/root", "TERM=xterm-direct", "SHELL=/bin/bash", "LANG=zh_CN.UTF-8", "LC_ALL=zh_CN.UTF-8", "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin", "/bin/bash", "-c", "/bin/bash -l; /off"]).status();
}