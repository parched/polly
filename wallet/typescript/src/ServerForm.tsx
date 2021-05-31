interface Props {
    defaultUrl: string
    onUrlChange: (url: string) => void
}

export default function ServerForm(props: Props): JSX.Element {
    return (
        <label>
            Server URL:
            <input type="text" value={props.defaultUrl} onChange={event => props.onUrlChange(event.target.value)} />
        </label>
    );
}
