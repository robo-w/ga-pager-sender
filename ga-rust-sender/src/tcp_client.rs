use std::io::{Read, Write};
use std::net::{TcpStream, ToSocketAddrs};
use std::time::Duration;

use crate::payload::AlphaPagingPayload;

pub fn send_and_wait_for_success<A: ToSocketAddrs>(
    address: A,
    payload: &AlphaPagingPayload,
) -> Result<(), String> {
    let mut stream = TcpStream::connect_timeout(
        &address
            .to_socket_addrs()
            .map_err(|e| format!("Failed to resolve address: {e}"))?
            .next()
            .ok_or_else(|| "No socket address resolved".to_string())?,
        Duration::from_secs(3),
    )
    .map_err(|e| format!("Failed to connect TCP socket: {e}"))?;

    stream
        .set_read_timeout(Some(Duration::from_secs(3)))
        .map_err(|e| format!("Failed to set read timeout: {e}"))?;
    stream
        .set_write_timeout(Some(Duration::from_secs(3)))
        .map_err(|e| format!("Failed to set write timeout: {e}"))?;

    let frame = payload.create_sendable_frame();
    stream
        .write_all(&frame)
        .map_err(|e| format!("Failed to send payload frame: {e}"))?;
    stream
        .flush()
        .map_err(|e| format!("Failed to flush payload frame: {e}"))?;

    let mut first = [0_u8; 1];
    stream
        .read_exact(&mut first)
        .map_err(|e| format!("Failed waiting for gateway response: {e}"))?;

    if first[0] == 0x15 {
        return Err("Gateway returned NACK (0x15).".to_string());
    }

    let mut checksum_tail = [0_u8; 3];
    let mut ack = [0_u8; 1];
    stream
        .read_exact(&mut checksum_tail)
        .map_err(|e| format!("Failed to read checksum response tail: {e}"))?;
    stream
        .read_exact(&mut ack)
        .map_err(|e| format!("Failed to read ACK byte: {e}"))?;

    let mut checksum_bytes = [0_u8; 4];
    checksum_bytes[0] = first[0];
    checksum_bytes[1..].copy_from_slice(&checksum_tail);

    let received_checksum = std::str::from_utf8(&checksum_bytes)
        .map_err(|_| "Gateway checksum response is not valid ASCII/UTF-8".to_string())?;

    let expected_checksum = payload.create_checksum();
    if received_checksum != expected_checksum {
        return Err(format!(
            "Checksum mismatch. Expected '{expected_checksum}', received '{received_checksum}'."
        ));
    }

    if ack[0] != 0x06 {
        return Err(format!(
            "Unexpected ACK byte. Expected 0x06, received 0x{:02X}.",
            ack[0]
        ));
    }

    Ok(())
}

