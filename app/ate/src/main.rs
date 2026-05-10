#![allow(non_snake_case)]
mod mt;
mod umt;
mod on;
mod txc;
mod desktop;
use std::{env, path::PathBuf};
fn main() {
    let args: Vec<String> = env::args().collect();
    if let Some(envDir) = args.get(1).filter(|&dir| dir != "txc") {
        if mt::teMt(envDir).is_ok() {
            let path = PathBuf::from(envDir);
            on::teOn(&path);
            let _ = umt::teUmt(&path);
        } else {
            std::process::exit(1);
        }
    } else {
        while desktop::executeDesktop() {}
    }
}