import React from 'react';

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
        const amount = event.target.value == '' ? event.target.value : parseInt(event.target.value);
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

    render() {
        const boldSymbol = !!this.state.inputAmount;

        return <div className="contribute-controls">
            {this.props.amounts.map(amount =>
                <button type="button" tabIndex="8"
                        key={amount}
                        className={'contribute-controls__button option-button ' + (this.state.highlightButton && this.props.currentAmount === amount ? ' active' : '')}
                        onClick={this.handleClick.bind(this, amount)}
                        data-amount={amount}>{this.props.symbol + amount}</button>
            )}

            <span className="contribute-controls__input contribute-controls__input--amount input-text">
                <span className={'symbol ' + (boldSymbol ? 'active' : '')}>{this.props.symbol}</span>
                <input type="number"
                       placeholder="Other amount" maxLength="10" tabIndex="12"
                       value={this.state.inputAmount}
                       onChange={this.updateInputAmount.bind(this)}
                       onFocus={this.handleFocus.bind(this)}
                       onBlur={this.handleBlur.bind(this)} />
            </span>


            <div className="fieldset__note">We are presently only able to accept contributions of £2000 or less</div>
        </div>;
    }
}
