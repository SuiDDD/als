mod mount;
mod umount;
mod on;
mod termux;
mod desktop;
use std::{env, path::PathBuf};
fn main() {
    let args: Vec<String> = env::args().collect();
    if let Some(ate_dir) = args.get(1).filter(|&dir| dir != "termux") {
        if mount::te_mt(ate_dir).is_ok() {
            let path = PathBuf::from(ate_dir);
            on::te_on(&path);
            let _ = umount::te_umt(&path);
        } else {
            std::process::exit(1);
        }
    } else {
        while desktop::execute_desktop() {}
    }
}