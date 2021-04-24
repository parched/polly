mod blockchain;
mod transaction;

use blockchain::Block;
use transaction::Transaction;

use ring::{
    rand,
    signature::{self, KeyPair},
};

use clap::{AppSettings, Clap};

use anyhow::{bail, Context, Error};
use fehler::throws;

/// Wallet application for polly coin on a polly blockchain
#[derive(Clap)]
#[clap(setting = AppSettings::ColoredHelp)]
struct Opts {
    /// The polly blockchain to use
    #[clap(short, long, default_value = "http://localhost:8080")]
    server: String,
    /// The file to read or write the private key
    #[clap(short, long, default_value = "polly-key")]
    key_file: String,
    #[clap(subcommand)]
    subcmd: SubCommand,
}

#[derive(Clap)]
enum SubCommand {
    PrintBalance(PrintBalance),
    Send(Send),
    GenKey(GenKey),
    PrintAddress(PrintAddress),
}

/// Print the balances
#[derive(Clap)]
struct PrintBalance {}

/// Print the public address for the private key
#[derive(Clap)]
struct PrintAddress {}

/// Send some coins
#[derive(Clap)]
struct Send {
    /// The address to send the polly coin to
    #[clap(index = 1)]
    to: String,
    #[clap(index = 2)]
    /// The amount of polly coin to send
    amount: u64,
}

/// Generate a key pair
#[derive(Clap)]
struct GenKey {}

#[throws]
#[tokio::main]
async fn main() {
    let opts: Opts = Opts::parse();

    match opts.subcmd {
        SubCommand::PrintBalance(_) => {
            let key_pair = get_key_pair(&opts.key_file)?;
            let address = key_pair.public_key().as_ref();

            let balances = get_balances(&opts.server).await?;

            let amount = balances.get(address);
            println!("Balance:\n{:?}", amount);
        }
        SubCommand::PrintAddress(_) => {
            let key_pair = get_key_pair(&opts.key_file)?;
            let address = base64::encode(key_pair.public_key());
            println!("Public address:\n{}", address);
        }
        SubCommand::Send(send_opts) => {
            let to = base64::decode(send_opts.to)?;
            let key_pair = get_key_pair(&opts.key_file)?;
            let transaction = Transaction::new(
                &key_pair,
                &to,
                send_opts.amount,
                &mut get_balances(&opts.server).await?,
            )?;

            let client = reqwest::Client::new();
            let res = client
                .post(opts.server + "/data")
                .body(transaction.bytes.to_vec())
                .send()
                .await?;
            if !res.status().is_success() {
                bail!("Failed to send transaction to server: {}", res.status());
            }
        }
        SubCommand::GenKey(_) => {
            // Generate a key pair in PKCS#8 (v2) format.
            let rng = rand::SystemRandom::new();
            let pkcs8_bytes = signature::Ed25519KeyPair::generate_pkcs8(&rng)?;
            std::fs::write(&opts.key_file, pkcs8_bytes)
                .with_context(|| format!("Failed to write key-file '{}'", &opts.key_file))?;
        }
    }
}

#[throws]
fn get_key_pair(key_file: &str) -> signature::Ed25519KeyPair {
    let pkcs8_bytes = std::fs::read(key_file)
        .with_context(|| format!("Failed to read key-file '{}'", key_file))?;
    signature::Ed25519KeyPair::from_pkcs8(&pkcs8_bytes)?
}

#[throws]
async fn get_balances(server: &str) -> transaction::Balances {
    println!("Connecting to server at: {}", server);
    let transactions = reqwest::get(server.to_owned() + "/blocks")
        .await?
        .json::<Vec<Block>>()
        .await?
        .iter()
        .filter_map(|block| Transaction::parse(&block.data))
        .collect::<Vec<_>>();

    transaction::get_balances(transactions)
}
