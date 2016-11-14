import React from 'react';

import {AmountButton} from '../Buttons';
import {AmountInput} from '../InputField';

export default class Contribution extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            highlightButton: true,
            inputAmount: this.presetSelected() ? '' : this.props.currentAmount
        };
    }

    presetSelected() {
        return this.props.amounts.some(a => a === this.props.currentAmount);
    }

    updateInputAmount(event) {
        this.setState({ inputAmount: event.target.value });
        this.props.setAmount(event.target.value);
    }

    handleClick(amount) {
        this.setState({
            inputAmount: '',
            highlightButton: true
        });

        this.props.setAmount(amount);
    }

    setValidationError(message) {
        this._input.setCustomValidity(message);
    }

    clearValidationError() {
        this._input.setCustomValidity('');
    }

    validate() {
        if (this.state.inputAmount > this.props.max) {
            return this.setValidationError(`We are presently only able to accept contributions of ${this.props.currency.prefix || ''}${this.props.currency.symbol}${this.props.max} or less.`);
        }

        if (!this.state.inputAmount && !this.props.currentAmount) {
            return this.setValidationError("Please select or enter a contribution amount.");
        }

        if (this.state.inputAmount && this.state.inputAmount < 1) {
            return this.setValidationError(`Please enter at least ${this.props.currency.prefix || ''}${this.props.currency.symbol}1`);
        }

        this.clearValidationError();
    }

    componentDidMount() {
        this.validate();
    }

    componentDidUpdate() {
        this.validate();
    }

    render() {
        return <div className="contribute-controls contribute-fields">
            <h2 className="contribute-controls-title">One-off contribution</h2>

            {this.props.amounts.map(amount =>
                <AmountButton amount={amount}
                              key={amount}
                              highlight={this.state.highlightButton}
                              currentAmount={this.props.currentAmount}
                              onClick={this.handleClick.bind(this, amount)}>
                    {this.props.currency.symbol + amount}
                </AmountButton>
            )}

            <AmountInput inputAmount={this.state.inputAmount}
                         symbol={this.props.currency.symbol}
                         refFn={component => this._input = component}
                         value={this.state.inputAmount}
                         onChange={this.updateInputAmount.bind(this)}
                         small={this.props.amounts.length == 6}
            />

            {this.props.error.show && <div className="payment-error"> {this.props.error.message}</div>}
        </div>;
    }
}
