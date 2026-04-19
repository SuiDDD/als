mod mouse;
use std::path::Path;
use std::sync::Arc;
use tokio::io::{self, AsyncWriteExt};
use tokio::net::UnixStream;
use tokio::sync::mpsc;
use tokio::time::{Duration, Instant, sleep};
use vnc::{PixelFormat, VncEncoding, VncEvent, X11Event};
#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let sock = "/data/local/tmp/als/app/qvm/vnc.sock";
    while !Path::new(sock).exists() {
        sleep(Duration::from_millis(10)).await;
    }
    let stream = UnixStream::connect(sock).await?;
    let vnc = vnc::VncConnector::new(stream)
        .set_auth_method(async move { Ok(String::new()) })
        .add_encoding(VncEncoding::Raw)
        .add_encoding(VncEncoding::CopyRect)
        .allow_shared(true)
        .set_pixel_format(PixelFormat::rgba())
        .build()?
        .try_start()
        .await?
        .finish()?;
    let (tx, mut rx) = mpsc::channel::<(Arc<Vec<u8>>, u16, u16)>(3);
    let (back_tx, mut back_rx) = mpsc::channel::<Arc<Vec<u8>>>(2);
    let bt_render = back_tx.clone();
    tokio::spawn(async move {
        let mut out = io::BufWriter::with_capacity(1024 * 1024 * 15, io::stdout());
        let magic = [0xAA, 0x55, 0xAA, 0x55];
        while let Some(mut frame) = rx.recv().await {
            while let Ok(newer) = rx.try_recv() {
                let _ = bt_render.try_send(frame.0);
                frame = newer;
            }
            let (data, cw, ch) = frame;
            let mut header = [0u8; 16];
            header[0..4].copy_from_slice(&magic);
            header[4..8].copy_from_slice(&(cw as i32).to_le_bytes());
            header[8..12].copy_from_slice(&(ch as i32).to_le_bytes());
            header[12..16].copy_from_slice(&(cw as i32 * 4).to_le_bytes());
            let _ = out.write_all(&header).await;
            let _ = out.write_all(&data).await;
            let _ = out.flush().await;
            let _ = bt_render.try_send(data);
        }
    });
    let (itx, mut irx) = mpsc::channel::<X11Event>(32);
    mouse::start_input_handler(itx);
    let (mut w, mut h, mut current_buf) = (0u16, 0u16, Arc::new(Vec::new()));
    let mut last_req = Instant::now();
    let min_interval = Duration::from_millis(10);
    let mut has_update = false;
    let _ = vnc.input(X11Event::Refresh).await;
    loop {
        tokio::select! {
            biased;
            Some(ev) = irx.recv() => {
                let _ = vnc.input(ev).await;
            }
            res = vnc.poll_event() => {
                match res? {
                    Some(VncEvent::SetResolution(s)) => {
                        w = s.width; h = s.height;
                        let size = w as usize * h as usize * 4;
                        current_buf = Arc::new(vec![0u8; size]);
                        while back_rx.try_recv().is_ok() {}
                        let _ = back_tx.send(Arc::new(vec![0u8; size])).await;
                        has_update = true;
                    }
                    Some(VncEvent::RawImage(r, d)) if w > 0 => {
                        has_update = true;
                        let s = w as usize * 4;
                        let rs = r.width as usize * 4;
                        if let Some(buf_mut) = Arc::get_mut(&mut current_buf) {
                            let p = buf_mut.as_mut_ptr();
                            let sp = d.as_ptr();
                            unsafe { for y in 0..r.height as usize {
                                std::ptr::copy_nonoverlapping(sp.add(y * rs), p.add(((r.y as usize + y) * s) + (r.x as usize * 4)), rs);
                            }}
                        }
                    }
                    Some(VncEvent::Copy(d, s_rect)) if w > 0 => {
                        has_update = true;
                        let s = w as usize * 4;
                        let rl = s_rect.width as usize * 4;
                        if let Some(buf_mut) = Arc::get_mut(&mut current_buf) {
                            let p = buf_mut.as_mut_ptr();
                            unsafe { for y in 0..s_rect.height as usize {
                                let sy = ((s_rect.y as usize + y) * s) + (s_rect.x as usize * 4);
                                let dy = ((d.y as usize + y) * s) + (d.x as usize * 4);
                                std::ptr::copy(p.add(sy), p.add(dy), rl);
                            }}
                        }
                    }
                    None => {
                        if w > 0 && has_update && last_req.elapsed() >= min_interval {
                            if tx.capacity() == 0 {
                                while back_rx.try_recv().is_ok() {}
                                let _ = vnc.input(X11Event::Refresh).await;
                                has_update = false;
                            } else {
                                if let Ok(next_buf) = back_rx.try_recv() {
                                    let finished_buf = std::mem::replace(&mut current_buf, next_buf);
                                    if let Some(new_curr) = Arc::get_mut(&mut current_buf) {
                                        unsafe {
                                            std::ptr::copy_nonoverlapping(
                                                finished_buf.as_ptr(),
                                                new_curr.as_mut_ptr(),
                                                finished_buf.len(),
                                            );
                                        }
                                    }
                                    if let Err(e) = tx.try_send((finished_buf, w, h)) {
                                        if let tokio::sync::mpsc::error::TrySendError::Full((buf, _, _)) = e {
                                            let _ = back_tx.try_send(buf);
                                        }
                                    }
                                    let _ = vnc.input(X11Event::Refresh).await;
                                    last_req = Instant::now();
                                    has_update = false;
                                }
                            }
                        }
                    }
                    _ => {}
                }
            }
            _ = sleep(min_interval) => {
                if w > 0 && last_req.elapsed() > Duration::from_secs(1) {
                    let _ = vnc.input(X11Event::Refresh).await;
                }
            }
        }
    }
}
