import React from 'react';

export default class AmountSummary extends React.Component {
    render() {
        return <div className={"amount-summary " + (this.props.visible ? 'visible' : '')}>
            <span className="amount-summary__text">Your contribution</span>
            <span className="amount-summary__amount">{this.props.currency.prefix || ''}{this.props.currency.symbol}{this.props.amount}</span>
        </div>;
    }
}

