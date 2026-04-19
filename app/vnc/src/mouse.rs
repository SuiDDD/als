use std::io::{self, BufRead};
use tokio::sync::mpsc;
use vnc::{ClientMouseEvent, X11Event};

pub fn start_input_handler(tx: mpsc::Sender<X11Event>) {
    std::thread::spawn(move || {
        let stdin = io::stdin();
        for line in stdin.lock().lines() {
            if let Ok(l) = line {
                if l.starts_with('M') {
                    let p: Vec<&str> = l.split_whitespace().collect();
                    if p.len() == 4 {
                        let x = p[1].parse().unwrap_or(0);
                        let y = p[2].parse().unwrap_or(0);
                        let m = p[3].parse().unwrap_or(0);
                        let _ = tx.blocking_send(X11Event::PointerEvent(ClientMouseEvent {
                            position_x: x,
                            position_y: y,
                            bottons: m,
                        }));
                    }
                }
            }
        }
    });
}
