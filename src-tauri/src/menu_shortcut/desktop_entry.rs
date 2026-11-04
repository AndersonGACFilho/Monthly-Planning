//! Text of the app's `.desktop` file (POR-18), following the freedesktop Desktop Entry Specification 1.5.
//!
//! Values are escaped twice: `Exec` arguments are quoted (`"`, `` ` ``, `$` and `\` get a backslash, `%` is
//! doubled), then every value gets the string escapes of the format (`\\`, `\n`, `\t`, `\r`, `\s`).

use super::Launch;
use std::io;
use std::path::{Path, PathBuf};

const GROUP: &str = "[Desktop Entry]";

/// Desktop entry that runs `launch`, shown with the image at `icon`.
///
/// No `TryExec`: with the drive absent it would hide the entry until the desktop reads the folder again, and on a
/// drive that does not allow running programs it would hide it for good.
pub fn render(launch: &Launch, icon: &Path) -> io::Result<String> {
    let mut exec = vec![quote(text(&launch.program)?)];
    if let Some(script) = &launch.script {
        exec.push(quote(text(script)?));
    }
    let exec = escape(&exec.join(" ").replace('%', "%%"));
    let home = escape(text(&launch.home)?);
    let icon = escape(text(icon)?);
    Ok(format!(
        "{GROUP}
Type=Application
Version=1.5
Name=Planejamento Mensal
GenericName=Finanças da casa
GenericName[en]=Household finances
Comment=Rendas, despesas, faturas e extratos por cota
Comment[en]=Income, expenses, credit card bills and statements per member
Keywords=finanças;orçamento;despesas;fatura;cartão;
Keywords[en]=finance;budget;expenses;bill;card;
Exec={exec}
Path={home}
Icon={icon}
Terminal=false
Categories=Office;Finance;
StartupWMClass=PlanejamentoMensal
"
    ))
}

/// What the entry runs, read back from its `Exec` and `Path`; `None` when it has no command.
pub fn parse(content: &str) -> Option<Launch> {
    let exec = value(content, "Exec")?;
    let mut words = split(&unescape(exec).replace("%%", "%")).into_iter().map(PathBuf::from);
    let program = words.next()?;
    let script = words.next();
    let home = value(content, "Path").map(|home| PathBuf::from(unescape(home))).unwrap_or_default();
    Some(Launch { home, program, script })
}

fn text(path: &Path) -> io::Result<&str> {
    path.to_str()
        .ok_or_else(|| io::Error::new(io::ErrorKind::InvalidInput, format!("not UTF-8: {}", path.display())))
}

/// Value of `key` in the main group, without locale.
fn value<'a>(content: &'a str, key: &str) -> Option<&'a str> {
    let mut in_group = false;
    for line in content.lines() {
        if line.starts_with('[') {
            in_group = line.trim_end() == GROUP;
        } else if in_group {
            if let Some((name, value)) = line.split_once('=') {
                if name.trim_end() == key {
                    return Some(value.trim_start());
                }
            }
        }
    }
    None
}

/// One argument of `Exec`, always between double quotes.
fn quote(argument: &str) -> String {
    let mut quoted = String::with_capacity(argument.len() + 2);
    quoted.push('"');
    for c in argument.chars() {
        if matches!(c, '"' | '`' | '$' | '\\') {
            quoted.push('\\');
        }
        quoted.push(c);
    }
    quoted.push('"');
    quoted
}

/// Arguments of an `Exec` command line: split on spaces outside double quotes, with backslash escapes inside.
fn split(command: &str) -> Vec<String> {
    let mut words = Vec::new();
    let mut word: Option<String> = None;
    let mut quoted = false;
    let mut chars = command.chars();
    while let Some(c) = chars.next() {
        match c {
            '"' => {
                quoted = !quoted;
                word.get_or_insert_with(String::new);
            }
            '\\' if quoted => word.get_or_insert_with(String::new).extend(chars.next()),
            ' ' | '\t' if !quoted => words.extend(word.take()),
            c => word.get_or_insert_with(String::new).push(c),
        }
    }
    words.extend(word);
    words
}

/// String escapes of a value.
fn escape(value: &str) -> String {
    let mut escaped = String::with_capacity(value.len());
    for (i, c) in value.chars().enumerate() {
        match c {
            '\\' => escaped.push_str("\\\\"),
            '\n' => escaped.push_str("\\n"),
            '\t' => escaped.push_str("\\t"),
            '\r' => escaped.push_str("\\r"),
            ' ' if i == 0 => escaped.push_str("\\s"),
            c => escaped.push(c),
        }
    }
    escaped
}

fn unescape(value: &str) -> String {
    let mut plain = String::with_capacity(value.len());
    let mut chars = value.chars();
    while let Some(c) = chars.next() {
        if c != '\\' {
            plain.push(c);
            continue;
        }
        match chars.next() {
            Some('n') => plain.push('\n'),
            Some('t') => plain.push('\t'),
            Some('r') => plain.push('\r'),
            Some('s') => plain.push(' '),
            Some(other) => plain.push(other),
            None => plain.push('\\'),
        }
    }
    plain
}

#[cfg(test)]
mod tests {
    use super::*;

    fn launch(home: &str, program: &str, script: Option<&str>) -> Launch {
        Launch { home: home.into(), program: program.into(), script: script.map(PathBuf::from) }
    }

    #[test]
    fn por_18_entry_runs_the_appimage_with_the_icon() {
        let home = "/media/andy/SSD/PlanejamentoMensal";
        let entry = render(
            &launch(home, &format!("{home}/PlanejamentoMensal.AppImage"), None),
            Path::new("/home/andy/.local/share/applications/planejamento-mensal.png"),
        )
        .unwrap();
        assert!(entry.starts_with("[Desktop Entry]\nType=Application\n"));
        assert!(entry.contains("\nExec=\"/media/andy/SSD/PlanejamentoMensal/PlanejamentoMensal.AppImage\"\n"));
        assert!(entry.contains("\nPath=/media/andy/SSD/PlanejamentoMensal\n"));
        assert!(entry.contains("\nIcon=/home/andy/.local/share/applications/planejamento-mensal.png\n"));
        assert!(entry.contains("\nName=Planejamento Mensal\n"));
        assert!(!entry.contains("TryExec"));
    }

    #[test]
    fn por_18_noexec_drive_runs_the_launcher_script_through_sh() {
        let home = "/run/media/andy/SSD/PlanejamentoMensal";
        let entry = render(&launch(home, "sh", Some(&format!("{home}/start-linux.sh"))), Path::new("/i.png")).unwrap();
        assert!(entry.contains("\nExec=\"sh\" \"/run/media/andy/SSD/PlanejamentoMensal/start-linux.sh\"\n"));
    }

    #[test]
    fn por_18_special_characters_are_quoted_and_escaped() {
        let home = r#"/media/my "SSD" $HOME `x` 100% \backup"#;
        let entry = render(&launch(home, &format!("{home}/PlanejamentoMensal"), None), Path::new("/i.png")).unwrap();
        // quote: \" \$ \` \\ and %% ; then the value escape doubles every backslash
        assert!(entry.contains(r#"Exec="/media/my \\"SSD\\" \\$HOME \\`x\\` 100%% \\\\backup/PlanejamentoMensal""#));
        assert!(entry.contains(r#"Path=/media/my "SSD" $HOME `x` 100% \\backup"#));
    }

    #[test]
    fn por_18_written_entry_reads_back_as_the_same_launch() {
        for original in [
            launch("/media/SSD 1/Planejamento", "/media/SSD 1/Planejamento/PlanejamentoMensal.AppImage", None),
            launch("/media/a\"b$c`d%e\\f", "sh", Some("/media/a\"b$c`d%e\\f/start-linux.sh")),
            launch(" /lead/space", "/lead/space/PlanejamentoMensal", None),
        ] {
            let entry = render(&original, Path::new("/i.png")).unwrap();
            assert_eq!(parse(&entry), Some(original));
        }
    }

    #[test]
    fn por_18_only_the_main_group_counts() {
        let content = "[Desktop Action other]\nExec=other\n[Desktop Entry]\nExec = \"/a/app\"\nPath=/a\n";
        assert_eq!(parse(content), Some(launch("/a", "/a/app", None)));
        assert_eq!(parse("[Desktop Entry]\nName=x\n"), None);
    }
}
