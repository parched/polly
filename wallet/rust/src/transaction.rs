use std::collections::HashMap;

pub struct Transaction {
    // TODO:
}

/// Parses a blob of data into a polly coin transaction
///
/// Data on the polly blockchain is arbitrary, so not all will be transactions
pub fn parse(data: &[u8]) -> Option<Transaction> {
    None // TODO:
}

pub fn get_balances(transactions: impl IntoIterator<Item = Transaction>) -> HashMap<String, u64> {
    HashMap::new()
}
