import React from 'react';

import Spinner from './Spinner';
import {Back, Forward} from './Buttons';
import {PAGES} from 'src/constants';

export default class Navigation extends React.Component {
    classNameFor(page) {
        switch (page) {
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
        const isFirstPage = !processing && this.props.page === PAGES.CONTRIBUTION;
        const showMobileBack = !processing && this.props.page == PAGES.PAYMENT;
        const showBack = !processing && this.props.page !== PAGES.CONTRIBUTION;
        const showPay = !processing && !!this.props.amount && this.props.page === PAGES.DETAILS;
        const showNext = false && !processing && this.props.page == PAGES.DETAILS;
        const classes = {
            nav: () => {'contribute-navigation ' + this.classNameFor(this.props.page)},
            desktop: {
                back: 'action--secondary contribute-navigation__back hidden-mobile',
                next: 'contribute-navigation__button contribute-navigation__next hidden-mobile'
            },
            mobile: {back: 'action--secondary contribute-navigation__back show-mobile'},
            pay: 'contribute-navigation__button action--pay',
            ccnext: 'contribute-navigation__button action action--button contribute-navigation__next action--next contribute_card__button',
            paypal: 'contribute-navigation__button action action--button  paypal__button'
        };

        return <div className={classes.nav()}>
            {showBack && <Back type="button"
                               className={classes.desktop.back}
                               onClick={this.props.goBack}>Back</Back>
            }
            {showNext &&
            <Forward className={classes.desktop.next}>Next</Forward>
            }
            {showPay && <Forward className={classes.pay}
                                 onClick={this.props.payWithCard}>Pay {this.props.currency.prefix}{this.props.currency.symbol}{this.props.amount}</Forward>
            }
            {showMobileBack && <Back className={classes.mobile.back}
                                     onClick={this.props.jumpToFirstPage}>Back</Back>
            }
            {isFirstPage && <Forward className={classes.ccnext}
                                       onClick={this.props.clearPaymentFlags}
                                              >{"Contribute with debit/credit card"}</Forward>
            }
            {isFirstPage && <Forward className={classes.paypal}
                                     onClick={this.props.payWithPaypal}>Contribute with</Forward>
            }
            {processing && <Spinner text="Processing"/>}
        </div>;
    }
}
