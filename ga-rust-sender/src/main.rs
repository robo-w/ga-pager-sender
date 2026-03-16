mod payload;
mod tcp_client;

use std::env;
use std::net::ToSocketAddrs;

use payload::{AlphaPagingPayload, DataRate, FunctionCode};

fn main() {
    if let Err(error) = run() {
        eprintln!("Error: {error}");
        print_usage();
        std::process::exit(1);
    }
}

fn run() -> Result<(), String> {
    let args: Vec<String> = env::args().collect();

    if args.len() == 2 {
        let flag = args[1].trim();
        if flag.eq_ignore_ascii_case("-h") || flag.eq_ignore_ascii_case("--help") {
            print_usage();
            return Ok(());
        }
    }

    if args.len() < 7 {
        return Err("Missing required arguments".to_string());
    }

    let server = args[1].trim().to_string();
    let port = args[2]
        .parse::<u16>()
        .map_err(|e| format!("Invalid port '{}': {e}", args[2]))?;
    let target_ric = args[3]
        .parse::<u32>()
        .map_err(|e| format!("Invalid target RIC '{}': {e}", args[3]))?;
    let baudrate = args[4].parse::<DataRate>()?;
    let function_code = args[5].parse::<FunctionCode>()?;

    // Allow message text with spaces even when not quoted by joining trailing args.
    let message = args[6..].join(" ");
    if message.trim().is_empty() {
        return Err("Message must not be empty".to_string());
    }

    let payload = AlphaPagingPayload::new(target_ric, baudrate, function_code, message)?;

    let address = format!("{server}:{port}");
    let _ = address
        .to_socket_addrs()
        .map_err(|e| format!("Invalid target socket '{address}': {e}"))?;

    println!(
        "Sending message to {}:{} (RIC {}, baudrate {}, function {}).",
        server, port, payload.target_ric, payload.data_rate, payload.function_code
    );

    tcp_client::send_and_wait_for_success((server.as_str(), port), &payload)?;

    println!("Gateway response validated successfully.");
    Ok(())
}

fn print_usage() {
    eprintln!("Usage:");
    eprintln!(
        "  ga-sender <server> <port> <target-ric> <baudrate> <function-code> <message>"
    );
    eprintln!("\nExamples:");
    eprintln!("  ga-sender 192.168.42.100 10300 12345 1200 1 \"Test Message\"");
    eprintln!("  ga-sender pocsag-sender.local 10300 601234 BPS_1200 CODE_1 Test Message");
}
