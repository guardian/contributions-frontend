import React from 'react';
import MediaQuery from 'react-responsive';
import { connect } from 'react-redux';

import {
    GO_FORWARD,
    GO_BACK,
    UPDATE_DETAILS,
    UPDATE_CARD,
    SET_AMOUNT,
    JUMP_TO_PAGE,
    PAYPAL_PAY,
    CARD_PAY,
    CLEAR_PAYMENT_FLAGS,
    SET_RECURRING,
    paypalRedirect,
    submitPayment,
    trackCheckoutStep,
    setRecurring,
    showRecurringTestMessage
} from 'src/actions';

import { PAGES } from 'src/constants';

import MobileWrapper from './form-wrapper/MobileWrapper';
import DesktopWrapper from './form-wrapper/DesktopWrapper';
import Contribution from './pages/Contribution';
import Details from './pages/Details';
import Payment from './pages/Payment';
import AmountSummary from './AmountSummary';

import * as abTests from 'src/modules/abTests';

function mapStateToProps(state) {
    return {
        page: state.page.page,
        processing: state.page.processing,
        details: state.details,
        card: state.card,
        currency: state.data.currency,
        maxAmount: state.data.maxAmount,
        paypalPay: state.page.paypalPay,
        cardPay: state.page.cardPay,
        paymentError: state.page.paymentError,
        amounts: abTests.amounts(state),
        countryGroup: state.data.countryGroup,
        showRecurring: abTests.showRecurring(state.data.abTests),
        recurring: state.details.recurring,
        recurringNotified: state.data.recurringNotified
    };
}

function mapDispatchToProps(dispatch) {
    return {
        goBack: () => dispatch({ type: GO_BACK }),
        goForward: () => dispatch({ type: GO_FORWARD }),
        setAmount: a => {
            dispatch(trackCheckoutStep(1, 'checkout', 'Amount'));
            dispatch({ type: SET_AMOUNT, amount: a })
        },
        jumpToFirstPage: () => dispatch({type: JUMP_TO_PAGE, page: 1}),
        updateDetails: d => {
            dispatch(trackCheckoutStep(2, 'checkout', 'PersonalDetails'));
            dispatch({ type: UPDATE_DETAILS, details: d })
        },
        updateCard: c => dispatch({ type: UPDATE_CARD, card: c }),
        pay: () => {
            dispatch(trackCheckoutStep(3, 'checkout', 'Pay with Stripe'));
            dispatch(submitPayment)
        },
        payWithPaypal: () => {
            dispatch(trackCheckoutStep(3, 'checkout', 'Pay with Paypal'));
            dispatch({ type: PAYPAL_PAY })
        },
        payWithCard: () => dispatch({ type: CARD_PAY }),
        paypalRedirect: () => dispatch(paypalRedirect),
        clearPaymentFlags: () => dispatch({ type: CLEAR_PAYMENT_FLAGS }),
        setRecurring: enabled => dispatch(setRecurring(enabled)),
        showRecurringTestMessage: () => dispatch(showRecurringTestMessage)
    };
}

class Main extends React.Component {
    componentFor(page, mobile) {
        switch (page) {
            case PAGES.CONTRIBUTION:
                return <Contribution max={this.props.maxAmount}
                                     error={this.props.paymentError}
                                     currentAmount={this.props.card.amount}
                                     mobile={mobile}
                                     {...this.props} />;

            case PAGES.DETAILS:
                return <Details details={this.props.details}
                                updateDetails={this.props.updateDetails}/>;

            case PAGES.PAYMENT:
                return <Payment card={this.props.card}
                                updateCard={this.props.updateCard}
                                error={this.props.paymentError}
                                countryGroup={this.props.countryGroup} />;
        }
    }

    submit(event) {
        event.preventDefault(); // we never want the standard submit behaviour, which triggers a reload

        if (!event.target.checkValidity()) return;

        if (this.props.showRecurring && this.props.recurring === null) return; // disable form progress if recurring test is on and no recurring method has been selected

        if (this.props.recurring === true) {
            return this.props.showRecurringTestMessage();
        }

        if (this.props.paypalPay) {
            this.props.paypalRedirect();
        }

        else {
            if (this.props.cardPay) {
                this.props.pay();
            } else {
                this.props.goForward();
            }
        }
    }

    render() {
        const showSummary = !!this.props.card.amount && this.props.page !== PAGES.CONTRIBUTION;

        return <div>
            <MediaQuery query='(max-width: 740px)'>
                {this.renderSummary(showSummary)}
                <MobileWrapper submit={this.submit.bind(this, true)} componentFor={this.componentFor.bind(this)} {...this.props} />
            </MediaQuery>

            <MediaQuery query='(min-width: 741px)'>
                {this.renderSummary(showSummary)}
                <DesktopWrapper submit={this.submit.bind(this)} componentFor={this.componentFor.bind(this)} {...this.props} />
            </MediaQuery>
        </div>
    }

    renderSummary(visible) {
        return <AmountSummary currency={this.props.currency} amount={this.props.card.amount} visible={visible} />
    }
}


export default connect(
    mapStateToProps,
    mapDispatchToProps
)(Main);
