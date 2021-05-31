import { decode } from './blockchain';
import { getBalances } from './transactions';
import {useAsyncFn} from './async';

interface Props {
    url: String
    address: Uint8Array
}

async function fetchBalance(propz: Props): Promise<number> {
    const response = await fetch(propz.url + "/blocks");
    const blocks = decode(response.json());
    const balances = getBalances(blocks.map(b => b.data));
    const balance = balances.get(propz.address);
    if (typeof balance === "undefined") {
        return 0;
    }
    return balance;
}

export default function Balance(props: Props): JSX.Element {
    const { result, error, isPending } = useAsyncFn(fetchBalance, props);

    return (
        <label>
            Balance: {
                isPending ? "Loading" : error ? error.message : result
            }
        </label>
    );
}