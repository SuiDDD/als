use ratatui::{
    backend::CrosstermBackend,
    layout::{Constraint, Direction, Layout, Rect},
    style::{Color, Modifier, Style},
    widgets::{Block, Borders, Paragraph, BorderType},
    Terminal,
};
use crossterm::{
    event::{self, Event, MouseButton, MouseEventKind},
    execute,
    terminal::{disable_raw_mode, enable_raw_mode, EnterAlternateScreen, LeaveAlternateScreen},
};
use std::{io, time::Duration};
pub fn execute_desktop() -> bool {
    enable_raw_mode().unwrap();
    let mut stdout = io::stdout();
    execute!(stdout, EnterAlternateScreen, event::EnableMouseCapture).unwrap();
    let backend = CrosstermBackend::new(stdout);
    let mut terminal = Terminal::new(backend).unwrap();
    loop {
        terminal.draw(|frame| {
            let area = frame.area();
            frame.render_widget(Block::default().style(Style::default().bg(Color::Black)), area);
            let chunks = Layout::default()
                .direction(Direction::Vertical)
                .margin(2)
                .constraints([
                    Constraint::Length(7),
                    Constraint::Length(3),
                    Constraint::Min(0),
                ])
                .split(area);
            let termux_block = Block::default()
                .borders(Borders::ALL)
                .border_type(BorderType::Rounded)
                .border_style(Style::default().fg(Color::Cyan))
                .title(" 应用启动 ");
            let termux_rect = Rect::new(chunks[0].x, chunks[0].y, 30, 7);
            frame.render_widget(termux_block, termux_rect);
            frame.render_widget(
                Paragraph::new("\n  [ TERMUX ]\n\n  点击进入终端环境")
                    .style(Style::default().fg(Color::White).add_modifier(Modifier::BOLD)),
                termux_rect.inner(ratatui::layout::Margin { horizontal: 2, vertical: 1 })
            );
            let exit_block = Block::default()
                .borders(Borders::ALL)
                .border_type(BorderType::Rounded)
                .border_style(Style::default().fg(Color::Red))
                .title(" 系统 ");
            let exit_rect = Rect::new(chunks[1].x, chunks[1].y, 30, 3);
            frame.render_widget(exit_block, exit_rect);
            frame.render_widget(
                Paragraph::new("  退出程序 (EXIT)")
                    .style(Style::default().fg(Color::Red)),
                exit_rect.inner(ratatui::layout::Margin { horizontal: 1, vertical: 1 })
            );
        }).unwrap();
        if event::poll(Duration::from_millis(100)).unwrap() {
            if let Event::Mouse(mouse) = event::read().unwrap() {
                if mouse.kind == MouseEventKind::Down(MouseButton::Left) {
                    let (c, r) = (mouse.column, mouse.row);
                    if c >= 2 && c <= 32 && r >= 2 && r <= 9 {
                        disable_raw_mode().unwrap();
                        execute!(terminal.backend_mut(), LeaveAlternateScreen, event::DisableMouseCapture).unwrap();
                        return true;
                    }
                    if c >= 2 && c <= 32 && r >= 11 && r <= 14 {
                        disable_raw_mode().unwrap();
                        execute!(terminal.backend_mut(), LeaveAlternateScreen, event::DisableMouseCapture).unwrap();
                        return false;
                    }
                }
            }
        }
    }
}
