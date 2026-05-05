use std::{fs, path::Path, process::Command};
pub fn te_umt(target_root: &Path) -> Result<(), Box<dyn std::error::Error>> {
    let ate_dir = fs::canonicalize(target_root)?;
    let ate_dir_str = ate_dir.to_str().unwrap();
    let mounts_content = fs::read_to_string("/proc/mounts")?;
    let mut relevant_mounts: Vec<String> = mounts_content
        .lines()
        .filter_map(|line| {
            let parts: Vec<&str> = line.split_whitespace().collect();
            if parts.len() > 1 && parts[1].starts_with(ate_dir_str) {
                Some(parts[1].to_string())
            } else {
                None
            }
        })
        .collect();
    relevant_mounts.sort_by(|path_a, path_b| path_b.len().cmp(&path_a.len()));
    relevant_mounts.dedup();
    for mount_path in relevant_mounts {
        println!("\x1b[33m卸载中\x1b[0m {} \x1b[38;5;252mumount.rs\x1b[0m", mount_path);
        let status = Command::new("umount").arg(&mount_path).status();
        if status.is_err() || !status.as_ref().unwrap().success() {
            let _ = Command::new("umount").arg("-l").arg(&mount_path).status();
        }
        let check_mounts = fs::read_to_string("/proc/mounts")?;
        let is_still_mounted = check_mounts
            .lines()
            .any(|line| line.contains(&format!(" {} ", mount_path)));
        if !is_still_mounted {
            println!("\x1b[32m已卸载\x1b[0m {} \x1b[38;5;252mumount.rs\x1b[0m", mount_path);
        } else {
            println!("\x1b[31m未卸载\x1b[0m {} \x1b[38;5;252mumount.rs\x1b[0m", mount_path);
        }
    }
    Ok(())
}