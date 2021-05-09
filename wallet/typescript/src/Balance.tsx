import React from 'react';
import {decode} from './blockchain';
import {getBalances} from './transactions';

interface Props {
    url: String
    address: Uint8Array
}

interface State {
    balance: any
}

export default class Balance extends React.Component<Props, State> {
  isUnmounted: boolean = false;

  constructor(props: Props) {
    super(props);
    this.state = {balance: "loading"};
  }

  async fetchBalance(): Promise<number> {
    const response = await fetch(this.props.url + "/blocks");
    const blocks = decode(response.json());
    const balances = getBalances(blocks.map(b => b.data));
    const balance = balances.get(this.props.address);
    if (typeof balance === "undefined") {
        return 0;
    }
    return balance;
  }

  componentDidMount() {
      this.fetchBalance()
      .catch(reason => reason) // just print the error instead of balance
      .then(b => {
          if (!this.isUnmounted) {
              this.setState({balance: b})
          }
      });
  }

  componentWillUnmount() {
    this.isUnmounted = true;
  }

  render() {
    return (
    <label>
        Balance: {this.state.balance}
    </label>
    );
  }
}