use std::{fs, path::Path, process::Command};
pub fn teUmt(envDir: &Path) -> Result<(), Box<dyn std::error::Error>> {
    let envStr = fs::canonicalize(envDir)?.to_str().unwrap().to_string();
    let mut mounts: Vec<_> = fs::read_to_string("/proc/mounts")?
        .lines()
        .filter_map(|line| line.split_whitespace().nth(1))
        .filter(|path| path.starts_with(&envStr))
        .map(String::from)
        .collect();
    mounts.sort_by_key(|path| std::cmp::Reverse(path.len()));
    mounts.dedup();
    for path in mounts {
        println!("\x1b[33m卸载中\x1b[0m {path} \x1b[38;5;252mumount.rs\x1b[0m");
        let _ = Command::new("umount").arg(&path).status();
        let stillMounted = fs::read_to_string("/proc/mounts")?
            .lines()
            .any(|line| line.split_whitespace().nth(1) == Some(&path));
        if !stillMounted {
            println!("\x1b[32m已卸载\x1b[0m {path} \x1b[38;5;252mumount.rs\x1b[0m");
        } else {
            println!("\x1b[31m未卸载\x1b[0m {path} \x1b[38;5;252mumount.rs\x1b[0m");
        }
    }
    Ok(())
}