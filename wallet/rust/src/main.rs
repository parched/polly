mod blockchain;
mod transaction;

use clap::{AppSettings, Clap};

/// Wallet application for polly coin on a polly blockchain
#[derive(Clap)]
#[clap(setting = AppSettings::ColoredHelp)]
struct Opts {
    /// The polly blockchain to use
    #[clap(short, long, default_value = "http://localhost:8080")]
    server: String,
    #[clap(subcommand)]
    subcmd: SubCommand,
}

#[derive(Clap)]
enum SubCommand {
    Print(Print),
    Send(Send),
}

/// Print the balances
#[derive(Clap)]
struct Print {}

/// Send some coins
#[derive(Clap)]
struct Send {}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let opts: Opts = Opts::parse();

    println!("Connecting to server at: {}", opts.server);

    match opts.subcmd {
        SubCommand::Print(_) => {
            let transactions = reqwest::get(opts.server + "/blocks")
                .await?
                .json::<Vec<blockchain::Block>>()
                .await?
                .iter()
                .filter_map(|block| transaction::parse(&block.data))
                .collect::<Vec<_>>();

            let balances = transaction::get_balances(transactions);

            println!("Balances\n{:?}", balances);
            Ok(())
        }
        SubCommand::Send(_) => {
            println!("Sending 5 polly coins to bob");
            Ok(())
        }
    }
}
