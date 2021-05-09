import React from 'react';

interface Props {
    defaultUrl: string
}

interface State {
    url: string
}

export default class ServerForm extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {url: props.defaultUrl};

    this.handleChange = this.handleChange.bind(this);
  }

  handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    this.setState({url: event.target.value});
  }

  render() {
    return (
        <label>
          Server URL:
          <input type="text" value={this.state.url} onChange={this.handleChange} />
        </label>
    );
  }
}