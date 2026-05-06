use ratatui::{
    backend::CrosstermBackend,
    layout::{Alignment, Constraint, Layout, Margin, Rect},
    style::{Color, Modifier, Style},
    widgets::{Block, Paragraph, BorderType, Borders},
    Terminal,
};
use crossterm::{
    event::{self, Event, KeyCode, MouseButton, MouseEventKind},
    execute,
    terminal::{disable_raw_mode, enable_raw_mode, EnterAlternateScreen, LeaveAlternateScreen},
};
use std::{io::{self, Write}, time::Duration, path::PathBuf};

pub fn execute_desktop() -> bool {
    enable_raw_mode().unwrap();
    let mut stdout = io::stdout();
    execute!(stdout, EnterAlternateScreen, event::EnableMouseCapture).unwrap();
    let mut terminal = Terminal::new(CrosstermBackend::new(stdout)).unwrap();
    let mut selected = 0;

    loop {
        let mut btns = Vec::new();
        terminal.draw(|f| {
            let area = f.area();
            f.render_widget(Block::default().style(Style::new().bg(Color::Black)), area);
            
            let [_, mid, _, bottom] = Layout::vertical([
                Constraint::Fill(1),
                Constraint::Length(11),
                Constraint::Fill(1),
                Constraint::Length(1),
            ]).areas(area);

            let [_, inner, _] = Layout::horizontal([
                Constraint::Fill(1),
                Constraint::Length(30),
                Constraint::Fill(1),
            ]).areas(mid);

            btns = Layout::vertical([
                Constraint::Length(3),
                Constraint::Length(1),
                Constraint::Length(3),
                Constraint::Length(1),
                Constraint::Length(3),
            ]).split(inner).iter().step_by(2).cloned().collect();

            let txt = ["DebianEnv", "SystemEnv", "TermuxEnv"];
            for i in 0..3 {
                let style = Style::new().fg(if selected == i { Color::Cyan } else { Color::White });
                f.render_widget(Block::default().borders(Borders::ALL).border_type(BorderType::Rounded).border_style(style), btns[i]);
                f.render_widget(Paragraph::new(txt[i]).alignment(Alignment::Center).style(style.add_modifier(Modifier::BOLD)), btns[i].inner(Margin { horizontal: 0, vertical: 1 }));
            }
            f.render_widget(Paragraph::new("方向键选择，Enter/点击执行，ESC退出").alignment(Alignment::Center).style(Style::new().fg(Color::DarkGray)), bottom);
        }).unwrap();

        if event::poll(Duration::from_millis(50)).unwrap() {
            match event::read().unwrap() {
                Event::Key(k) => match k.code {
                    KeyCode::Esc => break,
                    KeyCode::Up => selected = (selected + 2) % 3,
                    KeyCode::Down => selected = (selected + 1) % 3,
                    KeyCode::Enter => { cleanup(&mut terminal); return handle_action(selected); }
                    _ => {}
                },
                Event::Mouse(m) => {
                    let (c, r) = (m.column, m.row);
                    if let Some(i) = btns.iter().position(|b| b.contains(c, r)) {
                        selected = i;
                        if m.kind == MouseEventKind::Up(MouseButton::Left) { cleanup(&mut terminal); return handle_action(i); }
                    }
                }
                _ => {}
            }
        }
    }
    cleanup(&mut terminal);
    false
}

fn handle_action(i: usize) -> bool {
    match i {
        0 => {
            let path = PathBuf::from("/data/local/tmp/als/app/chr/ate");
            if crate::mount::te_mt(&path).is_ok() {
                crate::on::te_on(&path);
                let _ = crate::umount::te_umt(&path);
            }
            true
        }
        1 => {
            let _ = io::stdout().write_all(b"\x1bc\x1b[2J\x1b[3J\x1b[H");
            let _ = io::stdout().flush();
            false
        }
        2 => {
            crate::termux::execute_termux();
            true
        }
        _ => false,
    }
}

fn cleanup(terminal: &mut Terminal<CrosstermBackend<io::Stdout>>) {
    let _ = disable_raw_mode();
    let _ = execute!(terminal.backend_mut(), LeaveAlternateScreen, event::DisableMouseCapture);
}

trait RectExt { fn contains(&self, x: u16, y: u16) -> bool; }
impl RectExt for Rect {
    fn contains(&self, x: u16, y: u16) -> bool { x >= self.left() && x < self.right() && y >= self.top() && y < self.bottom() }
}