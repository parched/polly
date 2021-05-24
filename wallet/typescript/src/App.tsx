import React from 'react';
import logo from './logo.svg';
import './App.css';
import ServerForm from './ServerForm'
import KeyFile from './KeyFile'
import Balance from './Balance'

interface State {
    serverUrl: string
    keyFile?: File
}

class App extends React.Component<{}, State> {
    state = { serverUrl: "http://localhost:8080" }
    render() {
        return (
            <div className="App">
                <header className="App-header">
                    <img src={logo} className="App-logo" alt="logo" />
                    <p>
                        <ServerForm defaultUrl={this.state.serverUrl} onUrlChange={url => this.setState({ serverUrl: url })} />
                    </p>
                    <p>
                        <KeyFile onFileChanged={file => this.setState({ keyFile: file })} />
                    </p>
                    <p>
                        <Balance url={this.state.serverUrl} address={new Uint8Array() /* TODO */}/>
                    </p>
                </header>
            </div>
        );
    }
}

export default App;
