#![allow(non_snake_case)]
mod mount;
mod umount;
mod on;
mod txc;
mod desktop;
use std::{env, path::PathBuf};
fn main() {
    let args: Vec<String> = env::args().collect();
    if let Some(ateDir) = args.get(1).filter(|&dir| dir != "txc") {
        if mount::teMt(ateDir).is_ok() {
            let path = PathBuf::from(ateDir);
            on::teOn(&path);
            let _ = umount::teUmt(&path);
        } else {
            std::process::exit(1);
        }
    } else {
        while desktop::executeDesktop() {}
    }
}