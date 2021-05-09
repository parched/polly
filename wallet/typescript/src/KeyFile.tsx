import React from 'react';

interface Props {
    onFileChanged: (file: File) => void
}

export default class KeyFile extends React.Component<Props> {
  constructor(props: Props) {
    super(props);

    this.handleChange = this.handleChange.bind(this);
  }

  handleChange(event: React.ChangeEvent<HTMLInputElement>) {
    const files = event.currentTarget.files
    if (files) {
        this.props.onFileChanged(files[0]);
    }
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