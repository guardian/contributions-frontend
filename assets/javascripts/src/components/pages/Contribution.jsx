import React from 'react';

export default class Contribution extends React.Component {
    constructor(props) {
        super(props)
        this.state = { inputAmount: '' };
    }

    updateInputAmount(event) {
        const amount = event.target.value == '' ? event.target.value : parseInt(event.target.value);
        return this.setState({ inputAmount: amount });
    }

    setCustomAmount() {
        this.props.setAmount(this.state.inputAmount);
    }

    handleClick(amount) {
        this.setState({ inputAmount: '' });
        this.props.setAmount(amount);
    }

    render() {
        const amounts = [25, 50, 100, 250];

        return <div className="contribute-controls">
            {amounts.map(amount => <button type="button" tabIndex="8"
                                           key={amount}
                                           className={'contribute-controls__button option-button ' + (this.props.currentAmount === amount ? ' active' : '')}
                                           onClick={this.handleClick.bind(this, amount)}
                                           data-amount={amount}>£{amount}</button>)}

            <input type="number" className="contribute-controls__input input-text"
                   placeholder="Other amount" maxLength="10" tabIndex="12"
                   value={this.state.inputAmount}
                   onChange={this.updateInputAmount.bind(this)}
                   onBlur={this.setCustomAmount.bind(this)} />
        </div>;
    }
}
