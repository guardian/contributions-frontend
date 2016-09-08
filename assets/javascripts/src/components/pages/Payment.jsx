import React from 'react';

import InputField from '../InputField';
import CardIcon from '../CardIcon';

export default class Payment extends React.Component {
    formatExpiry(expiry) {
        if (expiry.length >= 3 && expiry.indexOf('/') === -1) {
            return expiry.slice(0, 2) + '/' + expiry.slice(2);
        }

        return expiry;
    }

    validateCardNumber(event) {
        if (this.props.card.number && !Stripe.card.validateCardNumber(this.props.card.number)) {
            event.target.setCustomValidity('Please enter a valid card number.');
        } else {
            this.clearValidation(event.target);
        }
    }

    validateExpiry(event) {
        if (this.props.card.expiry && !Stripe.card.validateExpiry(this.props.card.expiry)) {
            event.target.setCustomValidity('The expiry date must be in the future.');
        } else {
            this.clearValidation(event.target);
        }
    }

    validateCVC(event) {
        if (this.props.card.cvc && !Stripe.card.validateCVC(this.props.card.cvc)) {
            event.target.setCustomValidity('Please enter a valid verification code.');
        } else {
            this.clearValidation(event.target);
        }
    }

    clearValidation(el) {
        el.setCustomValidity('');
    }

    render() {
        return <div className='contribute-payment contribute-fields'>

            <InputField label="Card number" type="text" value={this.props.card.number}
                        onChange={event => this.props.updateCard({ number: event.target.value })}
                        onKeyDown={event => this.clearValidation(event.target)}
                        onBlur={this.validateCardNumber.bind(this)}
                        tabIndex="13" size="20" id="cc-num" data-stripe="number"
                        pattern="[0-9 ]*" placeholder="0000 0000 0000 0000" maxLength="19" autoComplete="off"
                        outerClassName="with-card-icon" required>
                <CardIcon number={this.props.card.number} />
            </InputField>

            <div className="flex-horizontal">
                <InputField label="Expiry date"
                            type="text"
                            value={this.props.card.expiry}
                            onChange={event => this.props.updateCard({ expiry: this.formatExpiry(event.target.value) })}
                            onKeyDown={event => this.clearValidation(event.target)}
                            onBlur={this.validateExpiry.bind(this)}
                            className="center-text"
                            outerClassName="half-width"
                            placeholder="MM/YY"
                            maxLength="5"
                            tabIndex="14"
                            required />

                <InputField label="Security code"
                            type="text"
                            value={this.props.card.cvc}
                            onChange={event => this.props.updateCard({ cvc: event.target.value })}
                            onKeyDown={event => this.clearValidation(event.target)}
                            onBlur={this.validateCVC.bind(this)}
                            className="center-text"
                            outerClassName="half-width"
                            tabIndex="15"
                            required />
            </div>

            {this.props.error.show &&
                <div className="payment-error">
                    {this.props.error.kind === 'card' ? this.props.error.message : 'Sorry, an error occurred, please try again.'}
                </div>
            }

        </div>
    }
}
