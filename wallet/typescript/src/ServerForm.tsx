import React from 'react';

interface ServerProps {
    defaultUrl: string
}

interface ServerState {
    url: string
}

export default class ServerForm extends React.Component<ServerProps, ServerState> {
  constructor(props: ServerProps) {
    super(props);
    this.state = {url: props.defaultUrl};

    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    this.setState({url: event.target.value});
  }

  handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    alert('A url was submitted: ' + this.state.url);
    event.preventDefault();
  }

  render() {
    return (
      <form onSubmit={this.handleSubmit}>
        <label>
          Server URL:
          <input type="text" value={this.state.url} onChange={this.handleChange} />
        </label>
        <input type="submit" value="Submit" />
      </form>
    );
  }
}