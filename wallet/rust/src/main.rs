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
struct Print {
}

/// Send some coins
#[derive(Clap)]
struct Send {
}

fn main() {
    let opts: Opts = Opts::parse();

    println!("Connecting to server at: {}", opts.server);

    match opts.subcmd {
        SubCommand::Print(_) => {
            println!("You have 5 polly coins");
        },
        SubCommand::Send(_) => {
            println!("Sending 5 polly coins to bob");
        }
    }
}