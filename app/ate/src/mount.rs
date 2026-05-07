use std::{fs, path::Path, process::Command};
use crate::umount::te_umt;
pub fn te_mt(ate_dir: &str) -> Result<(), Box<dyn std::error::Error>> {
    let folders = ["als", "apex", "data_mirror", "dev", "linkerconfig", "metadata", "mnt", "odm", "odm_dlkm", "oem", "postinstall", "product", "proc", "sdcard", "storage", "sys", "system", "system_dlkm", "system_ext", "tmp", "vendor", "vendor_dlkm"];
    for name in &folders {
        let path = format!("{ate_dir}/{name}");
        if !Path::new(&path).exists() {
            fs::create_dir(&path).map_err(|err| { let _ = te_umt(Path::new(ate_dir)); err })?;
        }
    }
    let points = [
        ("/data/local/tmp/als", "als"), ("/apex", "apex"), ("/data_mirror", "data_mirror"),
        ("/dev", "dev"), ("/linkerconfig", "linkerconfig"), ("/metadata", "metadata"),
        ("/mnt", "mnt"), ("/odm", "odm"), ("/odm_dlkm", "odm_dlkm"), ("/oem", "oem"),
        ("/postinstall", "postinstall"), ("/product", "product"), ("/proc", "proc"),
        ("/sdcard", "sdcard"), ("/storage", "storage"), ("/sys", "sys"),
        ("/system", "system"), ("/system_dlkm", "system_dlkm"), ("/system_ext", "system_ext"),
        ("/vendor", "vendor"), ("/vendor_dlkm", "vendor_dlkm"),
    ];
    for (source, name) in points {
        let target = format!("{ate_dir}/{name}");
        if !Path::new(source).exists() { continue; }
        println!("\x1b[33m挂载中\x1b[0m {source} 至 {target} \x1b[38;5;252mmount.rs\x1b[0m");
        let output = Command::new("mount").args(["--rbind", source, &target]).output()?;
        if output.status.success() {
            println!("\x1b[32m已挂载\x1b[0m {source} 至 {target} \x1b[38;5;252mmount.rs\x1b[0m");
        } else {
            let msg = String::from_utf8_lossy(&output.stderr).trim().to_string();
            println!("\x1b[31m未挂载\x1b[0m {source} 至 {target} \x1b[38;5;252mmount.rs\x1b[0m\n  {msg}");
            if name == "dev" || name == "proc" || name == "sys" {
                let _ = te_umt(Path::new(ate_dir));
                return Err(msg.into());
            }
        }
    }
    let shm = format!("{ate_dir}/dev/shm");
    if !Path::new(&shm).exists() { let _ = fs::create_dir(&shm); }
    println!("\x1b[33m挂载中\x1b[0m tmpfs 至 {shm} \x1b[38;5;252mmount.rs\x1b[0m");
    if let Ok(output) = Command::new("mount").args(["-t", "tmpfs", "-o", "rw,nosuid,nodev,mode=1777", "tmpfs", &shm]).output() {
        if output.status.success() {
            println!("\x1b[32m已挂载\x1b[0m tmpfs 至 {shm} \x1b[38;5;252mmount.rs\x1b[0m");
        }
    }
    let filesystems = vec![
        (vec!["-t".to_string(), "bpf".to_string(), "bpf".to_string(), format!("{ate_dir}/sys/fs/bpf")], "bpf"),
        (vec!["-t".to_string(), "configfs".to_string(), "configfs".to_string(), format!("{ate_dir}/sys/kernel/config")], "configfs"),
        (vec!["-t".to_string(), "debugfs".to_string(), "debugfs".to_string(), format!("{ate_dir}/sys/kernel/debug")], "debugfs"),
        (vec!["-t".to_string(), "devpts".to_string(), "-o".to_string(), "rw,nosuid,noexec,gid=5,mode=620,ptmxmode=000".to_string(), "devpts".to_string(), format!("{ate_dir}/dev/pts")], "devpts"),
        (vec!["-t".to_string(), "mqueue".to_string(), "mqueue".to_string(), format!("{ate_dir}/dev/mqueue")], "mqueue"),
        (vec!["-t".to_string(), "securityfs".to_string(), "securityfs".to_string(), format!("{ate_dir}/sys/kernel/security")], "securityfs"),
        (vec!["-t".to_string(), "tmpfs".to_string(), "-o".to_string(), "rw,nosuid,nodev,mode=1777".to_string(), "tmpfs".to_string(), format!("{ate_dir}/tmp")], "tmp")
    ];
    for (args, source) in filesystems {
        let target = args.last().unwrap().clone();
        if !Path::new(&target).exists() { let _ = fs::create_dir_all(&target); }
        println!("\x1b[33m挂载中\x1b[0m {source} 至 {target} \x1b[38;5;252mmount.rs\x1b[0m");
        let output = Command::new("mount").args(&args).output()?;
        if output.status.success() {
            println!("\x1b[32m已挂载\x1b[0m {source} 至 {target} \x1b[38;5;252mmount.rs\x1b[0m");
        } else {
            let msg = String::from_utf8_lossy(&output.stderr).trim().to_string();
            println!("\x1b[31m未挂载\x1b[0m {source} 至 {target} \x1b[38;5;252mmount.rs\x1b[0m\n  {msg}");
        }
    }
    for (name, source) in [("fd", "/proc/self/fd"), ("stderr", "/proc/self/fd/2"), ("stdin", "/proc/self/fd/0"), ("stdout", "/proc/self/fd/1"), ("tty0", "/dev/null")] {
        let link = Path::new(ate_dir).join("dev").join(name);
        if !link.exists() { let _ = std::os::unix::fs::symlink(source, link); }
    }
    let net = format!("{ate_dir}/dev/net");
    if !Path::new(&net).exists() { let _ = fs::create_dir(&net); }
    let tun = format!("{net}/tun");
    if !Path::new(&tun).exists() { let _ = Command::new("mknod").args([&tun, "c", "10", "200"]).status(); }
    Ok(())
}