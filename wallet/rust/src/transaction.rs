use std::collections::HashMap;
use std::convert::TryInto;

use ring::signature;

/// A transfer of polly coin from one address to another
pub struct Transaction {
    bytes: [u8; Self::TRANSACTION_SIZE],
}

impl Transaction {
    const ADDRESS_SIZE: usize = 32;
    const AMOUNT_SIZE: usize = 8;
    const NONCE_SIZE: usize = 4;
    const SIGNATURE_SIZE: usize = 64;

    const FROM_OFFSET: usize = 0;
    const TO_OFFSET: usize = Self::FROM_OFFSET + Self::ADDRESS_SIZE;
    const AMOUNT_OFFSET: usize = Self::TO_OFFSET + Self::ADDRESS_SIZE;
    const NONCE_OFFSET: usize = Self::AMOUNT_OFFSET + Self::AMOUNT_SIZE;
    const SIGNATURE_OFFSET: usize = Self::NONCE_SIZE + Self::NONCE_SIZE;
    const TRANSACTION_SIZE: usize = Self::SIGNATURE_OFFSET + Self::SIGNATURE_SIZE;

    pub fn from(&self) -> &[u8] {
        &self.bytes[Self::FROM_OFFSET..Self::TO_OFFSET]
    }

    pub fn to(&self) -> &[u8] {
        &self.bytes[Self::TO_OFFSET..Self::AMOUNT_OFFSET]
    }

    /// Must be less than or equal to
    pub fn amount(&self) -> u64 {
        u64::from_le_bytes(
            self.bytes[Self::AMOUNT_OFFSET..Self::NONCE_OFFSET]
                .try_into()
                .unwrap(),
        )
    }

    /// Must be greater than the last nonce used by this from
    pub fn nonce(&self) -> u32 {
        u32::from_le_bytes(
            self.bytes[Self::NONCE_OFFSET..Self::SIGNATURE_OFFSET]
                .try_into()
                .unwrap(),
        )
    }

    /// Must be the signature using from public key of the above fields concatenated
    pub fn signature(&self) -> &[u8] {
        &self.bytes[Self::SIGNATURE_OFFSET..Self::TRANSACTION_SIZE]
    }
    pub fn content(&self) -> &[u8] {
        &self.bytes[..Self::SIGNATURE_OFFSET]
    }

    pub fn verify_signature(&self) -> bool {
        let public_key_bytes = &self.from();
        let public_key = signature::UnparsedPublicKey::new(&signature::ED25519, public_key_bytes);
        public_key
            .verify(&self.content(), &self.signature())
            .is_ok()
    }

    /// Parses a blob of data into a polly coin transaction
    ///
    /// Data on the polly blockchain is arbitrary, so not all will be transactions
    pub fn parse(data: &[u8]) -> Option<Transaction> {
        data.try_into()
            .map(|array| Transaction { bytes: array })
            .ok()
    }
}

pub fn get_balances(transactions: impl IntoIterator<Item = Transaction>) -> HashMap<Vec<u8>, u64> {
    HashMap::new()
}
