use std::{fs, path::Path, process::Command};
pub fn te_mt(target_root: &Path) -> Result<(), Box<dyn std::error::Error>> {
    let ate_dir = target_root.to_str().unwrap();
    let sub_directories = ["dev/shm", "dev/pts", "dev/net", "proc", "sys", "tmp", "sdcard", "system", "vendor", "apex", "linkerconfig"];
    for directory in &sub_directories {
        fs::create_dir_all(target_root.join(directory))?;
    }
    let mut all_tasks: Vec<(Vec<String>, String, String)> = vec![
        (vec!["--rbind".to_string(), "/dev".to_string(), format!("{}/dev", ate_dir)], "/dev".to_string(), format!("{}/dev", ate_dir)),
        (vec!["-t".to_string(), "proc".to_string(), "proc".to_string(), format!("{}/proc", ate_dir)], "proc".to_string(), format!("{}/proc", ate_dir)),
        (vec!["-t".to_string(), "sysfs".to_string(), "sys".to_string(), format!("{}/sys", ate_dir)], "sys".to_string(), format!("{}/sys", ate_dir)),
        (vec!["-t".to_string(), "tmpfs".to_string(), "-o".to_string(), "rw,nosuid,nodev,mode=1777".to_string(), "tmpfs".to_string(), format!("{}/tmp", ate_dir)], "tmpfs".to_string(), format!("{}/tmp", ate_dir)),
        (vec!["-t".to_string(), "tmpfs".to_string(), "-o".to_string(), "rw,nosuid,nodev,mode=1777".to_string(), "tmpfs".to_string(), format!("{}/dev/shm", ate_dir)], "tmpfs".to_string(), format!("{}/dev/shm", ate_dir)),
        (vec!["-t".to_string(), "devpts".to_string(), "-o".to_string(), "rw,nosuid,noexec,gid=5,mode=620,ptmxmode=000".to_string(), "devpts".to_string(), format!("{}/dev/pts", ate_dir)], "devpts".to_string(), format!("{}/dev/pts", ate_dir)),
    ];
    for mount_point in &["system", "vendor", "apex", "linkerconfig", "sdcard"] {
        let source = format!("/{}", mount_point);
        let target = format!("{}/{}", ate_dir, mount_point);
        all_tasks.push((vec!["--rbind".to_string(), source.clone(), target.clone()], source, target));
    }
    for (arguments, source, target) in all_tasks {
        println!("\x1b[33m挂载中\x1b[0m {} 至 {} \x1b[38;5;252mmount.rs\x1b[0m", source, target);
        let output = Command::new("mount").args(&arguments).output()?;
        if output.status.success() {
            println!("\x1b[32m已挂载\x1b[0m {} 至 {} \x1b[38;5;252mmount.rs\x1b[0m", source, target);
        } else {
            let error_message = String::from_utf8_lossy(&output.stderr);
            println!("\x1b[31m未挂载\x1b[0m {} 至 {} \x1b[38;5;252mmount.rs\x1b[0m", source, target);
            println!("  {}", error_message.trim());
            return Err(Box::from(error_message.trim()));
        }
    }
    let dev_path = target_root.join("dev");
    let device_links = [("fd", "/proc/self/fd"), ("stdin", "/proc/self/fd/0"), ("stdout", "/proc/self/fd/1"), ("stderr", "/proc/self/fd/2"), ("tty0", "/dev/null")];
    for (link_name, link_target) in &device_links {
        let path = dev_path.join(link_name);
        if !path.exists() { let _ = std::os::unix::fs::symlink(link_target, path); }
    }
    let tun_device = dev_path.join("net/tun");
    if !tun_device.exists() {
        let _ = Command::new("mknod").args(&[tun_device.to_str().unwrap(), "c", "10", "200"]).status();
    }
    Ok(())
}