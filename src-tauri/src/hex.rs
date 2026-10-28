//! Lowercase hexadecimal text of bytes, for session tokens and SHA-256 digests.

pub fn encode(bytes: &[u8]) -> String {
    bytes.iter().map(|byte| format!("{byte:02x}")).collect()
}

#[cfg(test)]
mod tests {
    #[test]
    fn imp_04_two_lowercase_digits_per_byte() {
        assert_eq!(super::encode(&[0x00, 0x0f, 0xab]), "000fab");
    }
}
