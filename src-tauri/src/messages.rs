//! Texts of the native dialogs shown before the window can show anything itself, in pt-BR or en-US
//! following the system language (I18N-03).

pub struct Messages {
    pub title: &'static str,
    pub lock_held: &'static str,
    pub lock_orphaned: &'static str,
    pub start_failed: &'static str,
}

const PT_BR: Messages = Messages {
    title: "Planejamento Mensal",
    lock_held: "O Planejamento Mensal já está aberto com estes dados neste computador. Feche a outra janela antes de abrir de novo.",
    lock_orphaned: "Os dados ficaram travados por uma sessão que não foi fechada corretamente (talvez em outro computador). Liberar a trava e abrir agora?",
    start_failed: "Não foi possível iniciar o serviço local. Detalhes em:",
};

const EN_US: Messages = Messages {
    title: "Planejamento Mensal",
    lock_held: "Planejamento Mensal is already open with this data on this computer. Close the other window first.",
    lock_orphaned: "The data is locked by a session that was not closed properly (maybe on another computer). Release the lock and open now?",
    start_failed: "The local service could not start. Details in:",
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
