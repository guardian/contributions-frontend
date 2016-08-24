import React from 'react';

import Spinner from './Spinner.jsx';
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
        const showForward = !this.props.processing && this.props.page !== PAGES.PAYMENT;
        const showBack = !this.props.processing && this.props.page !== PAGES.CONTRIBUTION;
        const showPay = !this.props.processing && !!this.props.amount && this.props.page === PAGES.PAYMENT;
        const showPaypal = !this.props.processing && this.props.page === PAGES.CONTRIBUTION;

        return <div className={'contribute-navigation ' + this.classNameFor(this.props.page)}>
          {showBack && <a className="contribute-navigation__back hidden-mobile" onClick={this.props.goBack}>Back</a> }
          {showForward && <button className="contribute-navigation__button contribute-navigation__next action action--button action--next hidden-mobile">Next</button>}
          {showPay && <button className={'contribute-navigation__button contribute-navigation__pay action action--button action--pay'}>Contribute {this.props.currency.prefix}{this.props.currency.symbol}{this.props.amount}</button>}
          {showPaypal && <button className="contribute-navigation__button action action--button  paypal__button" onClick={this.props.payWithPaypal}>Contribute with</button>}

          {this.props.processing && <Spinner text="Processing" />}
        </div>;
    }
}
