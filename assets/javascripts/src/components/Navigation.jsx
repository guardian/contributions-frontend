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
        const paymentMethods = this.props.paymentMethodsTest && this.props.paymentMethodsTest.paymentMethods;
        const isPaymentMethodsControl = this.props.paymentMethodsTest && this.props.paymentMethodsTest.isControl();

        const showForward = !this.props.processing && this.props.page === PAGES.DETAILS;
        const showBack = !this.props.processing && this.props.page !== PAGES.CONTRIBUTION;
        const showPay = !this.props.processing && !!this.props.amount && this.props.page === PAGES.PAYMENT;
        const isFirstPage = !this.props.processing && this.props.page === PAGES.CONTRIBUTION;
        const showMobileBack = !this.props.processing && this.props.page === PAGES.PAYMENT && !(isPaymentMethodsControl && this.props.mobile);

        const showPaypal = isFirstPage && paymentMethods && paymentMethods.indexOf("PAYPAL") >= 0;
        const showCard = isFirstPage && (!paymentMethods || (paymentMethods && paymentMethods.indexOf("CARD") >= 0 && !(isPaymentMethodsControl && this.props.mobile)));
        const cardButtonLabel = !paymentMethods || isPaymentMethodsControl ? "Next" : "Contribute with debit/credit card";
        const showProcessing = this.props.processing && !(this.props.page == PAGES.CONTRIBUTION && isPaymentMethodsControl);

        return <div className={'contribute-navigation ' + this.classNameFor(this.props.page)}>
        {showBack && <Back type="button" className="action--secondary contribute-navigation__back hidden-mobile" onClick={this.props.goBack}>Back</Back>}
        {showForward && <Forward className="contribute-navigation__button contribute-navigation__next hidden-mobile">Next</Forward>}
        {showPay && <Forward className='contribute-navigation__button action--pay' onClick={this.props.payWithCard}>Contribute {this.props.currency.prefix}{this.props.currency.symbol}{this.props.amount}</Forward>}
        {showMobileBack && <Back className="action--secondary contribute-navigation__back show-mobile" onClick={this.props.jumpToFirstPage}>Back</Back>}
        {showCard &&  <Forward className="contribute-navigation__button action action--button contribute-navigation__next action--next contribute_card__button" onClick={this.props.clearPaymentFlags}>{cardButtonLabel}</Forward>}
        {showPaypal && <Forward className="contribute-navigation__button action action--button  paypal__button" onClick={this.props.payWithPaypal}>Contribute with</Forward>}
        {showProcessing && <Spinner text="Processing" />}
        </div>;
    }
}
