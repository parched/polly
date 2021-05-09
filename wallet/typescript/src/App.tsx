import React from 'react';
import logo from './logo.svg';
import './App.css';
import ServerForm from './ServerForm'
import KeyFile from './KeyFile'

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <p>
          <ServerForm defaultUrl="http://localhost:8080" />
        </p>
        <p>
          <KeyFile />
        </p>
      </header>
    </div>
  );
}

export default App;
