import React from 'react';

function setNoAmountError() {
    document.getElementById("custom-amount").setCustomValidity("Please select a contribution amount.");
}

function setMaxContributionError(prefix, symbol, max) {
    document.getElementById("custom-amount").setCustomValidity(`We are presently only able to accept contributions of ${prefix || ''}${symbol}${max} or less.`);
}

function clearError() {
    document.getElementById("custom-amount").setCustomValidity("");
}

export default class Contribution extends React.Component {
    constructor(props) {
        super(props)

        this.state = {
            highlightButton: true,
            inputAmount: this.presetSelected() ? '' : this.props.currentAmount
        };
    }

    presetSelected() {
        return this.props.amounts.some(a => a === this.props.currentAmount);
    }

    updateInputAmount(event) {
        const amount = event.target.value === '' ? event.target.value : parseInt(event.target.value);
        this.setState({ inputAmount: amount });
    }

    handleFocus() {
        this.setState({ highlightButton: false });
    }

    handleBlur() {
        this.setState({ highlightButton: true });
        this.props.setAmount(this.state.inputAmount);
    }

    handleClick(amount) {
        this.setState({
            inputAmount: '',
            highlightButton: true
        });

        this.props.setAmount(amount);
    }

    setValidation() {
        if (this.state.inputAmount > this.props.max) {
            return setMaxContributionError(this.props.currency.prefix ,this.props.currency.symbol, this.props.max);
        }

        if (!this.state.inputAmount && (!this.props.currentAmount || this.props.currentAmount === 0)) {
            return setNoAmountError();
        }

        clearError();
    }

    componentDidMount() {
        this.setValidation();
    }

    componentDidUpdate() {
        this.setValidation();
    }

    render() {
        return <div className='contribute-controls contribute-fields'>
            {this.props.amounts.map(amount =>
                <button type="button"
                        key={amount}
                        className={'contribute-controls__button option-button ' + (this.state.highlightButton && this.props.currentAmount === amount ? ' active' : '')}
                        onClick={this.handleClick.bind(this, amount)}
                        data-amount={amount}>{this.props.currency.symbol + amount}</button>
            )}

            <span className="contribute-controls__input contribute-controls__input--amount input-text">
                <span className={'symbol ' + (!!this.state.inputAmount ? 'active' : '')}>{this.props.currency.symbol}</span>
                <input type="number"
                       id="custom-amount"
                       placeholder="Other amount" maxLength="10"
                       value={this.state.inputAmount}
                       onChange={this.updateInputAmount.bind(this)}
                       onFocus={this.handleFocus.bind(this)}
                       onBlur={this.handleBlur.bind(this)} />
            </span>
        </div>;
    }
}
