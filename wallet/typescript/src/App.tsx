import { useState } from 'react';
import logo from './logo.svg';
import './App.css';
import ServerForm from './ServerForm'
import KeyFile from './KeyFile'
import Balance from './Balance'

export default function App() {
    const [serverUrl, setServerUrl] = useState<string>("http://localhost:8080");
    const [keyFile, setKeyFile] = useState<File | undefined>(undefined);

    return (
        <div className="App">
            <header className="App-header">
                <img src={logo} className="App-logo" alt="logo" />
                <p>
                    <ServerForm defaultUrl={serverUrl} onUrlChange={setServerUrl} />
                </p>
                <p>
                    <KeyFile onFileChanged={setKeyFile} />
                </p>
                <p>
                    <Balance url={serverUrl} address={new Uint8Array() /* TODO */} />
                </p>
            </header>
        </div>
    );
}

