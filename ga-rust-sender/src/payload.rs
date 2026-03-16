use std::fmt;

const MIN_RIC: u32 = 8;
const MAX_RIC: u32 = 2_097_151;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DataRate {
    Bps512,
    Bps1200,
    Bps2400,
}

impl DataRate {
    fn marker(self) -> char {
        match self {
            Self::Bps512 => '5',
            Self::Bps1200 => '1',
            Self::Bps2400 => '2',
        }
    }
}

impl std::str::FromStr for DataRate {
    type Err = String;

    fn from_str(value: &str) -> Result<Self, Self::Err> {
        match value.trim().to_ascii_uppercase().as_str() {
            "512" | "BPS_512" => Ok(Self::Bps512),
            "1200" | "BPS_1200" => Ok(Self::Bps1200),
            "2400" | "BPS_2400" => Ok(Self::Bps2400),
            other => Err(format!("Unsupported baudrate '{other}'. Use 512, 1200, or 2400.")),
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum FunctionCode {
    Default,
    Code1,
    Code2,
    Code3,
    Code4,
}

impl FunctionCode {
    fn marker(self) -> char {
        match self {
            Self::Default => '0',
            Self::Code1 => '1',
            Self::Code2 => '2',
            Self::Code3 => '3',
            Self::Code4 => '4',
        }
    }
}

impl std::str::FromStr for FunctionCode {
    type Err = String;

    fn from_str(value: &str) -> Result<Self, Self::Err> {
        match value.trim().to_ascii_uppercase().as_str() {
            "0" | "DEFAULT" => Ok(Self::Default),
            "1" | "CODE_1" => Ok(Self::Code1),
            "2" | "CODE_2" => Ok(Self::Code2),
            "3" | "CODE_3" => Ok(Self::Code3),
            "4" | "CODE_4" => Ok(Self::Code4),
            other => Err(format!("Unsupported function code '{other}'. Use 0..4.")),
        }
    }
}

#[derive(Debug, Clone)]
pub struct AlphaPagingPayload {
    pub target_ric: u32,
    pub data_rate: DataRate,
    pub function_code: FunctionCode,
    pub message: String,
}

impl AlphaPagingPayload {
    pub fn new(
        target_ric: u32,
        data_rate: DataRate,
        function_code: FunctionCode,
        message: String,
    ) -> Result<Self, String> {
        if !(MIN_RIC..=MAX_RIC).contains(&target_ric) {
            return Err(format!(
                "RIC must be in range '{}'..='{}'. Was: {target_ric}",
                MIN_RIC, MAX_RIC
            ));
        }

        Ok(Self {
            target_ric,
            data_rate,
            function_code,
            message,
        })
    }

    pub fn create_message_frame(&self) -> String {
        format!(
            "{:07}A{}{}{}",
            self.target_ric,
            self.data_rate.marker(),
            self.function_code.marker(),
            self.message
        )
    }

    pub fn create_checksum(&self) -> String {
        let message_for_checksum = format!("\x02{}", self.create_message_frame());
        calculate_checksum(&message_for_checksum)
    }

    pub fn create_sendable_frame(&self) -> Vec<u8> {
        let message_for_checksum = format!("\x02{}", self.create_message_frame());
        let mut frame = message_for_checksum;
        frame.push_str(&calculate_checksum(&frame));
        frame.push('\x04');
        to_us_ascii_bytes_lossy(&frame)
    }
}

fn calculate_checksum(message_for_checksum: &str) -> String {
    let checksum: u32 = message_for_checksum.chars().map(|c| c as u32).sum();
    format!("{checksum:04X}")
}

fn to_us_ascii_bytes_lossy(s: &str) -> Vec<u8> {
    s.chars()
        .map(|c| if c.is_ascii() { c as u8 } else { b'?' })
        .collect()
}

impl fmt::Display for DataRate {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Bps512 => write!(f, "512"),
            Self::Bps1200 => write!(f, "1200"),
            Self::Bps2400 => write!(f, "2400"),
        }
    }
}

impl fmt::Display for FunctionCode {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Default => write!(f, "DEFAULT"),
            Self::Code1 => write!(f, "CODE_1"),
            Self::Code2 => write!(f, "CODE_2"),
            Self::Code3 => write!(f, "CODE_3"),
            Self::Code4 => write!(f, "CODE_4"),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn formatter_matches_java_for_1200() {
        let payload = AlphaPagingPayload::new(
            12345,
            DataRate::Bps1200,
            FunctionCode::Code1,
            "Test Message".to_string(),
        )
        .unwrap();

        let expected = "\u{2}0012345A11Test Message0689\u{4}".as_bytes();
        assert_eq!(payload.create_sendable_frame(), expected);
    }

    #[test]
    fn formatter_matches_java_for_512_and_2400() {
        let payload_512 = AlphaPagingPayload::new(
            12345,
            DataRate::Bps512,
            FunctionCode::Code1,
            "Test Message".to_string(),
        )
        .unwrap();
        let payload_2400 = AlphaPagingPayload::new(
            12345,
            DataRate::Bps2400,
            FunctionCode::Code1,
            "Test Message".to_string(),
        )
        .unwrap();

        assert_eq!(payload_512.create_checksum(), "068D");
        assert_eq!(payload_2400.create_checksum(), "068A");
    }
}

