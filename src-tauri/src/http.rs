//! Minimal HTTP/1.1 client for the local backend (health and shutdown), without extra dependencies.

use std::io::{self, Read, Write};
use std::net::{Ipv4Addr, SocketAddr, TcpStream};
use std::time::Duration;

/// Sends one request to `127.0.0.1:<port>` and returns the status code and body.
pub fn request(port: u16, method: &str, path: &str, token: Option<&str>) -> io::Result<(u16, String)> {
    let address = SocketAddr::from((Ipv4Addr::LOCALHOST, port));
    let mut stream = TcpStream::connect_timeout(&address, Duration::from_millis(500))?;
    stream.set_read_timeout(Some(Duration::from_secs(5)))?;
    let token_header = token.map(|t| format!("X-Session-Token: {t}\r\n")).unwrap_or_default();
    write!(
        stream,
        "{method} {path} HTTP/1.1\r\nHost: 127.0.0.1:{port}\r\nConnection: close\r\nContent-Length: 0\r\n{token_header}\r\n"
    )?;
    let mut response = String::new();
    stream.read_to_string(&mut response)?;
    parse(&response)
}

fn parse(response: &str) -> io::Result<(u16, String)> {
    let status = response
        .split_whitespace()
        .nth(1)
        .and_then(|code| code.parse().ok())
        .ok_or_else(|| io::Error::new(io::ErrorKind::InvalidData, "no HTTP status line"))?;
    let body = response.split_once("\r\n\r\n").map(|(_, body)| body.to_string()).unwrap_or_default();
    Ok((status, body))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn por_08_parses_status_and_body() {
        let (status, body) = parse("HTTP/1.1 200 \r\nContent-Type: application/json\r\n\r\n{\"status\":\"UP\"}").unwrap();
        assert_eq!(status, 200);
        assert_eq!(body, "{\"status\":\"UP\"}");
    }

    #[test]
    fn por_08_rejects_garbage() {
        assert!(parse("garbage").is_err());
    }
}
