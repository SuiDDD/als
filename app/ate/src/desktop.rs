use ratatui::{prelude::*, widgets::*};
use crossterm::{event::{self, *}, execute, terminal::*};
use std::{io::{self, Write}, path::*, process::{Command, exit}, thread, time::Duration};
pub fn executeDesktop() -> bool {
    thread::spawn(|| loop {
        if Command::new("pidof").arg("sui.k.als").output().map_or(true, |o| o.stdout.is_empty()) {
            exit(0)
        }
        thread::sleep(Duration::from_millis(90))
    });
    enable_raw_mode().ok();
    let mut terminal = Terminal::new(CrosstermBackend::new(io::stdout())).unwrap();
    execute!(io::stdout(), EnterAlternateScreen, EnableMouseCapture).ok();
    let (mut selected, mut buttons, mut redraw) = (0, vec![], true);
    let clean = || {
        disable_raw_mode().ok();
        execute!(io::stdout(), LeaveAlternateScreen, DisableMouseCapture).ok()
    };
    loop {
        if redraw {
            terminal.draw(|f| {
                let [_, mid, _, bot] = Layout::vertical([Constraint::Fill(1), Constraint::Length(11), Constraint::Fill(1), Constraint::Length(1)]).areas(f.area());
                let [_, inr, _] = Layout::horizontal([Constraint::Fill(1), Constraint::Length(30), Constraint::Fill(1)]).areas(mid);
                f.render_widget(Block::new().bg(Color::Black), f.area());
                buttons = Layout::vertical([Constraint::Length(3), Constraint::Length(1), Constraint::Length(3), Constraint::Length(1), Constraint::Length(3)]).split(inr).iter().step_by(2).copied().collect();
                for (i, n) in ["UbuntuEnv", "SystemEnv", "TermuxEnv"].iter().enumerate() {
                    let s = if selected == i { Style::new().fg(Color::Rgb(233, 84, 32)) } else { Style::new() };
                    f.render_widget(Block::bordered().border_type(BorderType::Rounded).border_style(s), buttons[i]);
                    f.render_widget(Paragraph::new(*n).alignment(Alignment::Center).style(s.add_modifier(Modifier::BOLD)), buttons[i].inner(Margin { horizontal: 0, vertical: 1 }));
                }
                f.render_widget(Paragraph::new("↑↓键选择 Enter/点击执行 ESC退出").alignment(Alignment::Center).fg(Color::DarkGray), bot);
            }).ok();
            redraw = false;
        }
        match event::read().unwrap() {
            Event::Key(k) => match k.code {
                KeyCode::Esc => break,
                KeyCode::Up => { selected = (selected + 2) % 3; redraw = true }
                KeyCode::Down => { selected = (selected + 1) % 3; redraw = true }
                KeyCode::Enter => { clean(); if selected == 1 { handleAction(1); exit(0) } return handleAction(selected) }
                _ => {}
            },
            Event::Mouse(m) => if let Some(i) = buttons.iter().position(|b| m.column >= b.left() && m.column < b.right() && m.row >= b.top() && m.row < b.bottom()) {
                if selected != i { selected = i; redraw = true }
                if m.kind == MouseEventKind::Up(MouseButton::Left) { clean(); if i == 1 { handleAction(1); exit(0) } return handleAction(i) }
            },
            Event::Resize(..) => redraw = true,
            _ => {}
        }
    }
    clean();
    false
}
fn handleAction(i: usize) -> bool {
    match i {
        0 => {
            let p = "/data/local/tmp/als/app/chr/UbuntuEnv";
            if crate::mt::teMt(p).is_ok() {
                crate::on::teOn(&PathBuf::from(p));
                let _ = crate::umt::teUmt(Path::new(p));
            }
            true
        }
        1 => {
            print!("\x1bc\x1b[2J\x1b[3J\x1b[H");
            io::stdout().flush().ok();
            false
        }
        2 => {
            crate::txc::executeTermux();
            true
        }
        _ => false,
    }
}