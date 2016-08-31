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
        const showForward = !this.props.processing && this.props.page !== PAGES.PAYMENT;
        const showBack = !this.props.processing && this.props.page !== PAGES.CONTRIBUTION;
        const showPay = !this.props.processing && !!this.props.amount && this.props.page === PAGES.PAYMENT;

        return <div className={'contribute-navigation ' + this.classNameFor(this.props.page)}>
          {showBack && <Back type="button" className="action--secondary contribute-navigation__back hidden-mobile" onClick={this.props.goBack}>Back</Back>}
          {showForward && <Forward className="contribute-navigation__button contribute-navigation__next hidden-mobile">Next</Forward>}
          {showPay && <Forward className='contribute-navigation__button action--pay'>Contribute {this.props.currency.prefix}{this.props.currency.symbol}{this.props.amount}</Forward>}
          {this.props.processing && <Spinner text="Processing" />}
        </div>;
    }
}
