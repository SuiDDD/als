mod mount;
mod umount;
mod on;
mod off;
mod termux;
mod desktop;
use std::{env, path::PathBuf};
fn main() {
    let arguments: Vec<String> = env::args().collect();
    if let Some(te_dir) = arguments.get(1) {
        if te_dir != "termux" {
            let target_root = PathBuf::from(te_dir);
            if mount::te_mt(&target_root).is_ok() {
                on::te_on(&target_root);
                off::te_off(&target_root);
                let _ = umount::te_umt(&target_root);
            } else {
                std::process::exit(1);
            }
            return;
        }
    }
    loop {
        if desktop::execute_desktop() {
            termux::execute_termux();
        } else {
            break;
        }
    }
}