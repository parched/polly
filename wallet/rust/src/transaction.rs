use std::collections::HashMap;
use std::convert::TryInto;

use ring::signature::{self, KeyPair};

use anyhow::{anyhow, Context, Result};

#[derive(Debug)]
pub struct Balance {
    pub amount: u64,
    pub last_nonce: u32,
}

impl Balance {
    pub fn new(initial_amount: u64) -> Self {
        Balance {
            amount: initial_amount,
            last_nonce: 0,
        }
    }
}

pub struct Balances(HashMap<Vec<u8>, Balance>);

impl Balances {
    pub fn new() -> Self {
        Self(HashMap::new())
    }

    pub fn get(&self, address: &[u8]) -> u64 {
        self.0.get(address).map(|b| b.amount).unwrap_or(0u64)
    }

    pub fn transfer(
        &mut self,
        from: &[u8],
        to: &[u8],
        amount: u64,
        nonce: Option<u32>,
    ) -> Result<u32> {
        // The first sender starts with all the coins
        if self.0.is_empty() {
            self.0.insert(from.to_vec(), Balance::new(u64::MAX));
        }

        let from_balance = self
            .0
            .get_mut(from)
            .ok_or_else(|| anyhow!("Unknown sender (balance zero)"))
            .and_then(|b| {
                if b.amount < amount {
                    Err(anyhow!("Insufficient balance: {}", b.amount))
                } else {
                    match nonce {
                        Some(nonce) if nonce <= b.last_nonce => {
                            Err(anyhow!("Invalid nonce: {}", nonce))
                        }
                        _ => Ok(b),
                    }
                }
            })?;

        let new_nonce = nonce.unwrap_or_else(|| from_balance.last_nonce + 1);
        from_balance.amount -= amount;
        from_balance.last_nonce = new_nonce;

        match self.0.get_mut(to) {
            Some(to_balance) => to_balance.amount += amount,
            None => {
                self.0.insert(to.to_vec(), Balance::new(amount));
            }
        }

        Ok(new_nonce)
    }
}

/// A transfer of polly coin from one address to another
pub struct Transaction {
    pub bytes: [u8; Self::TRANSACTION_SIZE],
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
    const SIGNATURE_OFFSET: usize = Self::NONCE_OFFSET + Self::NONCE_SIZE;
    const TRANSACTION_SIZE: usize = Self::SIGNATURE_OFFSET + Self::SIGNATURE_SIZE;

    pub fn new(
        from: &signature::Ed25519KeyPair,
        to: &[u8],
        amount: u64,
        balances: &mut Balances,
    ) -> Result<Self> {
        let to: &[u8; Self::ADDRESS_SIZE] = to
            .try_into()
            .context("Destination address length incorrect")?;
        let address = from.public_key().as_ref();
        let nonce = balances.transfer(address, to, amount, None)?;

        Ok(Self::new_unchecked(from, to, amount, nonce))
    }

    fn new_unchecked(
        from: &signature::Ed25519KeyPair,
        to: &[u8; Self::ADDRESS_SIZE],
        amount: u64,
        nonce: u32,
    ) -> Self {
        let address = from.public_key().as_ref();

        let mut bytes = [0u8; Self::TRANSACTION_SIZE];
        bytes[Self::FROM_OFFSET..Self::TO_OFFSET].clone_from_slice(address);
        bytes[Self::TO_OFFSET..Self::AMOUNT_OFFSET].clone_from_slice(to);
        bytes[Self::AMOUNT_OFFSET..Self::NONCE_OFFSET].clone_from_slice(&amount.to_le_bytes());
        bytes[Self::NONCE_OFFSET..Self::SIGNATURE_OFFSET].clone_from_slice(&nonce.to_le_bytes());

        let sig = from.sign(&bytes[..Self::SIGNATURE_OFFSET]);

        bytes[Self::SIGNATURE_OFFSET..Self::TRANSACTION_SIZE].clone_from_slice(sig.as_ref());

        Self { bytes: bytes }
    }

    /// Ed25519 public key that the amount is sent from
    pub fn from(&self) -> &[u8] {
        &self.bytes[Self::FROM_OFFSET..Self::TO_OFFSET]
    }

    /// Ed25519 public key that the amount is sent to
    pub fn to(&self) -> &[u8] {
        &self.bytes[Self::TO_OFFSET..Self::AMOUNT_OFFSET]
    }

    /// Must be less than or equal to balance amount of this from address
    pub fn amount(&self) -> u64 {
        u64::from_le_bytes(
            self.bytes[Self::AMOUNT_OFFSET..Self::NONCE_OFFSET]
                .try_into()
                .unwrap(),
        )
    }

    /// Must be greater than the last nonce used by this from address
    pub fn nonce(&self) -> u32 {
        u32::from_le_bytes(
            self.bytes[Self::NONCE_OFFSET..Self::SIGNATURE_OFFSET]
                .try_into()
                .unwrap(),
        )
    }

    /// Ed25519 signature of the content using the from address
    pub fn signature(&self) -> &[u8] {
        &self.bytes[Self::SIGNATURE_OFFSET..Self::TRANSACTION_SIZE]
    }

    /// The transaction without the signature
    pub fn content(&self) -> &[u8] {
        &self.bytes[..Self::SIGNATURE_OFFSET]
    }

    pub fn has_valid_signature(&self) -> bool {
        let public_key_bytes = self.from();
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

pub fn get_balances(transactions: impl IntoIterator<Item = Transaction>) -> Balances {
    let mut balances = Balances::new();
    transactions
        .into_iter()
        .filter(|t| t.has_valid_signature())
        .for_each(|t| {
            // Invalid transfers are ignored
            let _ = balances.transfer(t.from(), t.to(), t.amount(), Some(t.nonce()));
        });
    balances
}
