import React from 'react';

import {RecurringSelection} from '../tests/RecurringPayments';
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
        const props = Object.assign({}, this.props, {
            inputAmount: this.state.inputAmount,
            updateInputAmount: this.updateInputAmount.bind(this),
            handleClick: this.handleClick.bind(this),
            highlightButton: this.state.highlightButton,
            refFn: component => this._input = component // create a reference to this element for validation (see: https://facebook.github.io/react/docs/more-about-refs.html)
        });

        return (this.props.showRecurring && !this.props.mobile)
            ? <FormWithRecurring {...props} />
            : <FormWithoutRecurring {...props} />;
    }
}

class FormWithRecurring extends React.Component {
    render() {
        const disabled = this.props.recurring === null;
        const inputHeading = disabled
            ? 'Amount'
            : this.props.recurring === true
                ? 'Monthly amount'
                : 'One-off amount';


        return <div className={'contribute-controls contribute-fields contribute-controls--recurring ' + (this.props.amounts.length % 3 ? 'option-button__fours ' : 'option-button__three ')}>
            <RecurringSelection setRecurring={this.props.setRecurring} recurring={this.props.recurring} recurringNotified={this.props.recurringNotified} />

            <h2 className="full-row">{inputHeading}</h2>

            <div className={'opacity-wrapper opacity-wrapper--contribute-controls'}>
                {this.props.amounts.map(amount =>
                    <AmountButton amount={amount}
                                  key={amount}
                                  highlight={this.props.highlightButton}
                                  currentAmount={this.props.currentAmount}
                                  onClick={() => !disabled && this.props.handleClick(amount)}>
                        {this.props.currency.symbol + amount}
                    </AmountButton>
                )}

                <AmountInput inputAmount={this.props.inputAmount}
                             symbol={this.props.currency.symbol}
                             refFn={this.props.refFn}
                             value={this.props.inputAmount}
                             disabled={disabled}
                             onChange={this.props.updateInputAmount}/>
            </div>

            {this.props.error.show && <div className="payment-error"> {this.props.error.message}</div>}
        </div>;
    }
}

class FormWithoutRecurring extends React.Component {
    render() {
        return <div className={'contribute-controls contribute-fields ' + (this.props.amounts.length % 3 ? 'option-button__fours' : 'option-button__three')}>
            {this.props.amounts.map(amount =>
                <AmountButton amount={amount}
                              key={amount}
                              highlight={this.props.highlightButton}
                              currentAmount={this.props.currentAmount}
                              onClick={() => this.props.handleClick(amount)}>
                    {this.props.currency.symbol + amount}
                </AmountButton>
            )}

            <AmountInput inputAmount={this.props.inputAmount}
                         symbol={this.props.currency.symbol}
                         refFn={this.props.refFn}
                         value={this.props.inputAmount}
                         onChange={this.props.updateInputAmount}/>

            {this.props.error.show && <div className="payment-error"> {this.props.error.message}</div>}
        </div>;
    }
}

