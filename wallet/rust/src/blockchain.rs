use serde::{Deserialize, Serialize};

use base64_serde::base64_serde_type;

base64_serde_type!(Base64Standard, base64::STANDARD);

#[derive(Serialize, Deserialize, Debug)]
pub struct Block {
    #[serde(with = "Base64Standard")]
    pub prev_hash: Vec<u8>,
    #[serde(with = "Base64Standard")]
    pub data: Vec<u8>,
    pub modifier: u32,
}
