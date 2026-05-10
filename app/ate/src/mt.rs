use std::{fs, path::Path, process::Command};
use crate::umt::teUmt;
pub fn teMt(envDir: &str) -> Result<(), Box<dyn std::error::Error>> {
    if !Path::new(envDir).exists() { fs::create_dir_all(envDir)?; }
    let excluded = ["bin", "config", "data", "data_mirror", "etc", "metadata", "mnt", "pstore", "storage"];
    let mut entries: Vec<_> = fs::read_dir("/")?.filter_map(|e| e.ok()).collect();
    entries.sort_by(|a, b| a.file_name().cmp(&b.file_name()));
    for entry in entries.into_iter().filter(|e| e.path().is_dir()) {
        let name = entry.file_name().into_string().unwrap_or_default();
        if excluded.contains(&name.as_str()) { continue; }
        let (src, tgt) = (format!("/{name}"), format!("{envDir}/{name}"));
        let dest = Path::new(&tgt);
        if !dest.exists() { let _ = fs::create_dir_all(dest); }
        println!("\x1b[33m挂载中\x1b[0m {src} 至 {tgt} \x1b[38;5;252mmount.rs\x1b[0m");
        let out = Command::new("mount").args(["--rbind", &src, &tgt]).output()?;
        if out.status.success() {
            println!("\x1b[32m已挂载\x1b[0m {src} 至 {tgt} \x1b[38;5;252mmount.rs\x1b[0m");
            match name.as_str() {
                "debug" | "debug_ramdisk" => { let _ = Command::new("mount").args(["-t", "debugfs", "debugfs", &tgt]).status(); }
                "dev" => {
                    ["binderfs", "net", "pts", "shm"].iter().for_each(|s| { let _ = fs::create_dir_all(dest.join(s)); });
                    let _ = Command::new("mount").args(["-t", "binder", "binder", &format!("{tgt}/binderfs")]).status();
                    let _ = Command::new("mount").args(["-t", "devpts", "-o", "rw,nosuid,noexec,gid=5,mode=620,ptmxmode=000", "devpts", &format!("{tgt}/pts")]).status();
                    let _ = Command::new("mount").args(["-t", "tmpfs", "-o", "rw,nosuid,nodev,mode=1777", "tmpfs", &format!("{tgt}/shm")]).status();
                    [("fd", "/proc/self/fd"), ("stderr", "/proc/self/fd/2"), ("stdin", "/proc/self/fd/0"), ("stdout", "/proc/self/fd/1"), ("tty0", "/dev/null")].iter().for_each(|(l, t)| { if !dest.join(l).exists() { let _ = std::os::unix::fs::symlink(t, dest.join(l)); } });
                    let tun = dest.join("net/tun");
                    if !tun.exists() { let _ = Command::new("mknod").args([tun.to_str().unwrap(), "c", "10", "200"]).status(); }
                }
                "proc" => { let _ = Command::new("mount").args(["-t", "proc", "proc", &tgt]).status(); }
                "tmp" => { let _ = Command::new("mount").args(["-t", "tmpfs", "-o", "rw,nosuid,nodev,mode=1777", "tmpfs", &tgt]).status(); }
                _ => {}
            }
        } else {
            let errMsg = String::from_utf8_lossy(&out.stderr).trim().to_string();
            println!("\x1b[31m未挂载\x1b[0m {src} 至 {tgt} \x1b[38;5;252mmount.rs\x1b[0m\n  {errMsg}");
            if ["dev", "proc", "sys"].contains(&name.as_str()) { let _ = teUmt(Path::new(envDir)); return Err(errMsg.into()); }
        }
    }
    Ok(())
}