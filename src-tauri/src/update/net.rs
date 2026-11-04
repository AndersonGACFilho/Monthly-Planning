//! HTTPS clients for GitHub (POR-16). Certificates come from the bundled Mozilla roots, so nothing is read from
//! or written to the system.

use std::time::Duration;
use ureq::Agent;

const USER_AGENT: &str = concat!("PlanejamentoMensal/", env!("CARGO_PKG_VERSION"));
/// A whole API call, so an offline start stays quiet.
const API_CALL_TIMEOUT: Duration = Duration::from_secs(20);
const CONNECT_TIMEOUT: Duration = Duration::from_secs(20);
/// Time without a byte before a download counts as stalled.
const STALL_TIMEOUT: Duration = Duration::from_secs(60);

/// Client for small API answers.
pub fn api_agent() -> Agent {
    Agent::config_builder()
        .user_agent(USER_AGENT)
        .timeout_global(Some(API_CALL_TIMEOUT))
        .build()
        .into()
}

/// Client for the release zip: no limit for the whole download, only for a stalled connection.
pub fn download_agent() -> Agent {
    Agent::config_builder()
        .user_agent(USER_AGENT)
        .timeout_connect(Some(CONNECT_TIMEOUT))
        .timeout_recv_response(Some(STALL_TIMEOUT))
        .timeout_per_read(Some(STALL_TIMEOUT))
        .build()
        .into()
}
