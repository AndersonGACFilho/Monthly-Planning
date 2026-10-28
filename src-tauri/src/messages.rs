//! Texts of the native dialogs shown before the window can show anything itself, in pt-BR or en-US
//! following the system language (I18N-03).

pub struct Messages {
    pub title: &'static str,
    pub lock_held: &'static str,
    pub lock_orphaned: &'static str,
    pub start_failed: &'static str,
    /// The new version did not start and the previous one comes back; `{version}` is the version undone.
    pub update_undone: &'static str,
    pub update_failed: &'static str,
}

const PT_BR: Messages = Messages {
    title: "Planejamento Mensal",
    lock_held: "O Planejamento Mensal já está aberto com estes dados neste computador. Feche a outra janela antes de abrir de novo.",
    lock_orphaned: "Os dados ficaram travados por uma sessão que não foi fechada corretamente (talvez em outro computador). Liberar a trava e abrir agora?",
    start_failed: "Não foi possível iniciar o serviço local. Detalhes em:",
    update_undone: "A versão {version} não conseguiu abrir, então o app vai voltar para a versão anterior. Seus dados continuam em data/, com uma cópia de segurança em data/backups/.",
    update_failed: "Não foi possível instalar a atualização baixada. O app continua na versão atual. Detalhes:",
};

const EN_US: Messages = Messages {
    title: "Planejamento Mensal",
    lock_held: "Planejamento Mensal is already open with this data on this computer. Close the other window first.",
    lock_orphaned: "The data is locked by a session that was not closed properly (maybe on another computer). Release the lock and open now?",
    start_failed: "The local service could not start. Details in:",
    update_undone: "Version {version} could not open, so the app goes back to the previous version. Your data stays in data/, with a backup copy in data/backups/.",
    update_failed: "The downloaded update could not be installed. The app stays on the current version. Details:",
};

/// Messages in the system language: Portuguese for `pt*`, English otherwise.
pub fn current() -> &'static Messages {
    for_locale(sys_locale::get_locale().as_deref().unwrap_or("pt-BR"))
}

fn for_locale(locale: &str) -> &'static Messages {
    if locale.to_ascii_lowercase().starts_with("pt") {
        &PT_BR
    } else {
        &EN_US
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn i18n_03_dialog_language_follows_system() {
        assert!(for_locale("pt-BR").lock_held.starts_with("O Planejamento"));
        assert!(for_locale("en-US").lock_held.starts_with("Planejamento Mensal is"));
        assert!(for_locale("de-DE").lock_held.starts_with("Planejamento Mensal is"));
    }
}
