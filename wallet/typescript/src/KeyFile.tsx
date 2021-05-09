import React from 'react';

interface Props {
}

interface State {
    file?: File
}

export default class KeyFile extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {};

    this.handleChange = this.handleChange.bind(this);
  }

  handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    const files = event.currentTarget.files
    this.setState({file: (files ? files[0] : undefined)});
  }

  render() {
    return (
    <label>
        Key file:
        <input type="file" onChange={this.handleChange} />
    </label>
    );
  }
}