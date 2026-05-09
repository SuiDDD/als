use std::{fs, path::Path, process::Command};
use crate::umount::teUmt;
pub fn teMt(envDir: &str) -> Result<(), Box<dyn std::error::Error>> {
    let rootDir = Path::new(envDir);
    if !rootDir.exists() { fs::create_dir_all(rootDir)?; }
    let excludedFolders = ["bin", "data", "data_mirror", "etc", "metadata", "mnt", "storage"];
    let mut entries: Vec<_> = fs::read_dir("/")?.filter_map(|e| e.ok()).collect();
    entries.sort_by(|a, b| a.file_name().cmp(&b.file_name()));
    for entry in entries {
        let entryDir = entry.path();
        if entryDir.is_dir() {
        let folderName = entryDir.file_name().and_then(|s| s.to_str()).ok_or("err")?;
            if excludedFolders.contains(&folderName) { continue; }
            let (sourceDir, targetDir) = (format!("/{folderName}"), format!("{envDir}/{folderName}"));
            let destinationDir = Path::new(&targetDir);
            if !destinationDir.exists() { let _ = fs::create_dir_all(destinationDir); }
            println!("\x1b[33m挂载中\x1b[0m {sourceDir} 至 {targetDir} \x1b[38;5;252mmount.rs\x1b[0m");
            let output = Command::new("mount").args(["--rbind", "--make-rslave", &sourceDir, &targetDir]).output()?;
            if output.status.success() {
                println!("\x1b[32m已挂载\x1b[0m {sourceDir} 至 {targetDir} \x1b[38;5;252mmount.rs\x1b[0m");
            } else {
                let errMsg = String::from_utf8_lossy(&output.stderr).trim().to_string();
                println!("\x1b[31m未挂载\x1b[0m {sourceDir} 至 {targetDir} \x1b[38;5;252mmount.rs\x1b[0m\n  {errMsg}");
                let _ = teUmt(rootDir);
                return Err(errMsg.into());
            }
        }
    }
    Ok(())
}