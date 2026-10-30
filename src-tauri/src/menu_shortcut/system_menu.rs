//! Where the system keeps the programs it finds when the user searches the computer (POR-18).

use super::Launch;
use std::io;

/// The system menu of one user on this computer, holding at most one entry of this app.
pub trait SystemMenu: Send + Sync {
    /// The entry of this app as written, or `None` when there is none.
    fn read(&self) -> io::Result<Option<Launch>>;

    /// Writes the entry of this app, replacing the one already there.
    fn write(&self, launch: &Launch) -> io::Result<()>;

    /// Deletes every file `write` creates; succeeds when there is none.
    fn remove(&self) -> io::Result<()>;
}

/// The system menu of the current user, or `None` where the app has none to offer.
pub fn for_current_user() -> Option<Box<dyn SystemMenu>> {
    #[cfg(target_os = "linux")]
    return super::applications::Applications::for_current_user().map(|menu| Box::new(menu) as Box<dyn SystemMenu>);
    #[cfg(windows)]
    return super::start_menu::StartMenu::for_current_user().map(|menu| Box::new(menu) as Box<dyn SystemMenu>);
    #[cfg(not(any(target_os = "linux", windows)))]
    None
}
