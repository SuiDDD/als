use ratatui::{
    backend::CrosstermBackend,
    layout::{Alignment, Constraint, Layout, Margin, Rect},
    style::{Color, Modifier, Style, Stylize},
    widgets::{Block, Paragraph, BorderType, Borders},
    Terminal,
};
use crossterm::{
    event::{self, Event, KeyCode, MouseButton, MouseEventKind},
    execute,
    terminal::{disable_raw_mode, enable_raw_mode, EnterAlternateScreen, LeaveAlternateScreen},
};
use std::{io::{self, Write}, time::Duration};
pub fn execute_desktop() -> bool {
    enable_raw_mode().unwrap();
    let mut stdout = io::stdout();
    execute!(stdout, EnterAlternateScreen, event::EnableMouseCapture).unwrap();
    let mut terminal = Terminal::new(CrosstermBackend::new(stdout)).unwrap();
    let mut selected = 0;
    loop {
        let mut buttons = Vec::new();
        terminal.draw(|frame| {
            let area = frame.area();
            frame.render_widget(Block::default().bg(Color::Black), area);
            let [_, mid, _, bottom] = Layout::vertical([Constraint::Fill(1), Constraint::Length(11), Constraint::Fill(1), Constraint::Length(1)]).areas(area);
            let [_, inner, _] = Layout::horizontal([Constraint::Fill(1), Constraint::Length(30), Constraint::Fill(1)]).areas(mid);
            buttons = Layout::vertical([Constraint::Length(3), Constraint::Length(1), Constraint::Length(3), Constraint::Length(1), Constraint::Length(3)])
                .split(inner).iter().step_by(2).cloned().collect();
            for (index, name) in ["UbuntuEnv", "SystemEnv", "TermuxEnv"].iter().enumerate() {
                let style = if selected == index { Style::new().fg(Color::Rgb(233, 84, 32)) } else { Style::new() };
                frame.render_widget(Block::default().borders(Borders::ALL).border_type(BorderType::Rounded).border_style(style), buttons[index]);
                frame.render_widget(Paragraph::new(*name).alignment(Alignment::Center).style(style.add_modifier(Modifier::BOLD)), buttons[index].inner(Margin { horizontal: 0, vertical: 1 }));
            }
            frame.render_widget(Paragraph::new("↑↓键选择 Enter/点击执行 ESC退出").alignment(Alignment::Center).fg(Color::DarkGray), bottom);
        }).unwrap();
        if event::poll(Duration::from_millis(50)).unwrap() {
            match event::read().unwrap() {
                Event::Key(key) => match key.code {
                    KeyCode::Esc => break,
                    KeyCode::Up => selected = (selected + 2) % 3,
                    KeyCode::Down => selected = (selected + 1) % 3,
                    KeyCode::Enter => { cleanup(&mut terminal); return handle_action(selected); }
                    _ => {}
                },
                Event::Mouse(mouse) => {
                    if let Some(index) = buttons.iter().position(|btn| btn.contains(mouse.column, mouse.row)) {
                        selected = index;
                        if mouse.kind == MouseEventKind::Up(MouseButton::Left) { cleanup(&mut terminal); return handle_action(index); }
                    }
                }
                _ => {}
            }
        }
    }
    cleanup(&mut terminal);
    false
}
fn handle_action(index: usize) -> bool {
    match index {
        0 => {
            let ate = "/data/local/tmp/als/app/chr/UbuntuEnv";
            if crate::mount::te_mt(ate).is_ok() {
                crate::on::te_on(&std::path::PathBuf::from(ate));
                let _ = crate::umount::te_umt(std::path::Path::new(ate));
            }
            true
        }
        1 => { let _ = io::stdout().write_all(b"\x1bc\x1b[2J\x1b[3J\x1b[H"); let _ = io::stdout().flush(); false }
        2 => { crate::termux::execute_termux(); true }
        _ => false,
    }
}
fn cleanup(terminal: &mut Terminal<CrosstermBackend<io::Stdout>>) {
    let _ = disable_raw_mode();
    let _ = execute!(terminal.backend_mut(), LeaveAlternateScreen, event::DisableMouseCapture);
}
trait RectExt { fn contains(&self, x: u16, y: u16) -> bool; }
impl RectExt for Rect {
    fn contains(&self, col: u16, row: u16) -> bool { col >= self.left() && col < self.right() && row >= self.top() && row < self.bottom() }
}