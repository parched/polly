import React from 'react';

interface Props {
    onFileChanged: (file: File) => void
}

export default function KeyFile(props: Props): JSX.Element {
    function handleChange(event: React.ChangeEvent<HTMLInputElement>): void {
        const files = event.currentTarget.files;
        if (files) {
            props.onFileChanged(files[0]);
        }
    }

    return (
        <label>
            Key file:
            <input type="file" onChange={handleChange} />
        </label>
    );
}