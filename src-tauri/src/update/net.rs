//! HTTPS clients for GitHub (POR-16). Certificates come from the bundled Mozilla roots, so nothing is read from
//! or written to the system.

use std::time::Duration;
use ureq::Agent;

const USER_AGENT: &str = concat!("PlanejamentoMensal/", env!("CARGO_PKG_VERSION"));

/// Client for small API answers: the whole call must end within 20 seconds, so an offline start stays quiet.
pub fn api_agent() -> Agent {
    Agent::config_builder()
        .user_agent(USER_AGENT)
        .timeout_global(Some(Duration::from_secs(20)))
        .build()
        .into()
}

/// Client for the release zip: no limit for the whole download, but a stalled connection fails after 60 seconds.
pub fn download_agent() -> Agent {
    Agent::config_builder()
        .user_agent(USER_AGENT)
        .timeout_connect(Some(Duration::from_secs(20)))
        .timeout_recv_response(Some(Duration::from_secs(60)))
        .timeout_per_read(Some(Duration::from_secs(60)))
        .build()
        .into()
}
