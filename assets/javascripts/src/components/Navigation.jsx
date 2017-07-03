import React from 'react';

import Spinner from './Spinner';
import { Back, Forward } from './Buttons';
import { PAGES } from 'src/constants';

export default class Navigation extends React.Component {
    classNameFor(page) {
        switch(page) {
            case PAGES.CONTRIBUTION:
                return 'contribution';
            case PAGES.DETAILS:
                return 'details';
            case PAGES.PAYMENT:
                return 'payment';
        }
    }

    render() {
        const processing = this.props.processing;
        const stripeCheckout = this.props.stripeCheckout;
        const isFirstPage = !processing && this.props.page === PAGES.CONTRIBUTION;
        const showMobileBack = !processing && this.props.page == PAGES.PAYMENT;
        const showBack = !processing && this.props.page !== PAGES.CONTRIBUTION;
        const showForward = !processing && this.props.page == PAGES.DETAILS;
        const showPay = !processing && !!this.props.amount && this.props.page === PAGES.PAYMENT;
        const showStripePaymentOption = isFirstPage && !this.props.disableStripe;

        return <div className={'contribute-navigation ' + this.classNameFor(this.props.page)}>
        {showBack && <Back type="button" className="action--secondary contribute-navigation__back hidden-mobile" onClick={this.props.goBack}>Back</Back>}
        {(stripeCheckout && showForward) && <Forward className='contribute-navigation__button action--pay' onClick={this.props.payWithCard}>Pay {this.props.currency.prefix}{this.props.currency.symbol}{this.props.amount}</Forward>}
        {(!stripeCheckout && showForward) && <Forward className="contribute-navigation__button contribute-navigation__next hidden-mobile">Next</Forward>}
        {(!stripeCheckout && showPay) && <Forward className='contribute-navigation__button action--pay' onClick={this.props.payWithCard}>Pay {this.props.currency.prefix}{this.props.currency.symbol}{this.props.amount}</Forward>}
        {showMobileBack && <Back className="action--secondary contribute-navigation__back show-mobile" onClick={this.props.jumpToFirstPage}>Back</Back>}
        {showStripePaymentOption &&  <Forward className="contribute-navigation__button action action--button contribute-navigation__next action--next contribute_card__button" onClick={this.props.clearPaymentFlags}>{"Contribute with debit/credit card"}</Forward>}
        {isFirstPage && <Forward className="contribute-navigation__button action action--button  paypal__button" onClick={this.props.payWithPaypal}>Contribute with</Forward>}
        {processing && <Spinner text="Processing" />}
        </div>;
    }
}
