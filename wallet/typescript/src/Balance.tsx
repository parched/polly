import React, { useEffect, useState } from 'react';
import { decode } from './blockchain';
import { getBalances } from './transactions';

interface Props {
    url: String
    address: Uint8Array
}

export default function Balance(props: Props): JSX.Element {
    const [balance, setBalance] = useState<any>("loading");

    async function fetchBalance(): Promise<number> {
        const response = await fetch(props.url + "/blocks");
        const blocks = decode(response.json());
        const balances = getBalances(blocks.map(b => b.data));
        const balance = balances.get(props.address);
        if (typeof balance === "undefined") {
            return 0;
        }
        return balance;
    }

    let isUnmounted: boolean = false;
    useEffect(() => {
        fetchBalance()
            .catch(reason => reason.message) // just print the error instead of balance
            .then(b => {
                if (!isUnmounted) {
                    setBalance(b)
                }
            });

        return () => {
            isUnmounted = true;
        }
    });


    return (
        <label>
            Balance: {balance}
        </label>
    );
}