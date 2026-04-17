use std::fs::File;
use std::io::{BufRead, BufReader};
use std::os::unix::fs::FileExt;
use std::process::Command;
fn main() -> Result<(), Box<dyn std::error::Error>> {
    let p_out = Command::new("pidof").arg("qemu-system-aarch64").output()?;
    if !p_out.status.success() { return Ok(()); }
    let pid = String::from_utf8(p_out.stdout)?.trim().to_string();
    let maps = File::open(format!("/proc/{}/maps", pid))?;
    let mut addr_info = None;
    for line in BufReader::new(maps).lines() {
        let l = line?;
        if l.contains("virtio-gpu res") {
            let p: Vec<&str> = l.split_whitespace().collect();
            let a: Vec<&str> = p[0].split('-').collect();
            addr_info = Some((u64::from_str_radix(a[0], 16)?, u64::from_str_radix(a[1], 16)?));
            break;
        }
    }
    let (s, e) = addr_info.ok_or("REGION_NOT_FOUND")?;
    let sz = e - s;
    let mem = File::open(format!("/proc/{}/mem", pid))?;
    let mut scan_buf = vec![0u8; (sz as usize).min(1024 * 512)];
    mem.read_exact_at(&mut scan_buf, s)?;
    let mut offset = 0;
    for (i, chunk) in scan_buf.chunks_exact(4).enumerate() {
        if chunk[0] != 0 || chunk[1] != 0 || chunk[2] != 0 { offset = (i * 4) as u64; break; }
    }
    let mut w = 0; let mut h = 0; let mut stride = 0;
    let try_res = [(1920, 1080), (1280, 800), (1024, 768), (800, 600), (640, 480)];
    let pixels_sz = (sz - offset) / 4;
    for (tw, th) in try_res {
        if pixels_sz >= tw * th {
            let ts = pixels_sz / th;
            if ts >= tw { w = tw; h = th; stride = ts; break; }
        }
    }
    if w == 0 { w = 1024; h = 768; stride = 1024; }
    let mut buf = vec![0u8; (w * h * 4) as usize];
    println!("\x1b[1;32m[D]\x1b[0m Detection: {}x{} (Stride: {})", w, h, stride);
    if stride == w {
        mem.read_exact_at(&mut buf, s + offset)?;
        for c in buf.chunks_exact_mut(4) { c.swap(0, 2); c[3] = 255; }
    } else {
        println!("\x1b[1;34m[D]\x1b[0m Dynamic Cropping...");
        let full_buf_sz = (stride * h * 4) as usize;
        let mut raw_buf = vec![0u8; full_buf_sz];
        mem.read_exact_at(&mut raw_buf, s + offset)?;
        for y in 0..h {
            let row_start = (y * stride * 4) as usize;
            let row_end = row_start + (w * 4) as usize;
            if row_end <= raw_buf.len() {
                let mut row = raw_buf[row_start..row_end].to_vec();
                for c in row.chunks_exact_mut(4) { c.swap(0, 2); c[3] = 255; }
                buf[((y * w * 4) as usize)..((y * w * 4 + w * 4) as usize)].copy_from_slice(&row);
            }
        }
    }
    image::save_buffer_with_format("/data/local/tmp/db.png", &buf, w as u32, h as u32, image::ColorType::Rgba8, image::ImageFormat::Png)?;
    println!("\x1b[1;32m[✓]\x1b[0m Snapshot saved. Stride: {}", stride);
    Ok(())
}
