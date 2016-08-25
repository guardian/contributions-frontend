import React from 'react';
import MediaQuery from 'react-responsive';
import { connect } from 'react-redux';

import { GO_FORWARD, GO_BACK, UPDATE_DETAILS, UPDATE_CARD, SET_AMOUNT, submitPayment, PAYPAL_PAY, CARD_PAY, paypalRedirect} from 'src/actions';
import { PAGES } from 'src/constants';

import MobileWrapper from './form-wrapper/MobileWrapper';
import DesktopWrapper from './form-wrapper/DesktopWrapper';
import Contribution from './pages/Contribution';
import Details from './pages/Details';
import Payment from './pages/Payment';
import AmountSummary from './AmountSummary';

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
        paymentError: state.page.paymentError
    };
}

function mapDispatchToProps(dispatch) {
    return {
        goBack: () => dispatch({ type: GO_BACK }),
        goForward: () => dispatch({ type: GO_FORWARD }),
        setAmount: a => dispatch({ type: SET_AMOUNT, amount: a }),
        updateDetails: d => dispatch({ type: UPDATE_DETAILS, details: d }),
        updateCard: c => dispatch({ type: UPDATE_CARD, card: c }),
        pay: () => dispatch(submitPayment),
        payWithPaypal: () => dispatch({ type: PAYPAL_PAY }),
        payWithCard: () => dispatch({ type: CARD_PAY }),
        paypalRedirect: () => dispatch(paypalRedirect)
    };
}

class Main extends React.Component {
    componentFor(page) {
        switch (page) {
            case PAGES.CONTRIBUTION:
                return <Contribution amounts={[25, 50, 100, 250]}
                                     max={this.props.maxAmount}
                                     currency={this.props.currency}
                                     setAmount={this.props.setAmount}
                                     currentAmount={this.props.card.amount}/>;

            case PAGES.DETAILS:
                return <Details details={this.props.details}
                                updateDetails={this.props.updateDetails}/>;

            case PAGES.PAYMENT:
                return <Payment card={this.props.card}
                                updateCard={this.props.updateCard}
                                error={this.props.paymentError} />;
        }
    }

    submit(event) {
        event.preventDefault(); // we never want the standard submit behaviour, which triggers a reload

        if (!event.target.checkValidity()) return;

        if(this.props.paypalPay) {
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
            <AmountSummary currency={this.props.currency} amount={this.props.card.amount} visible={showSummary} />

            <MediaQuery query='(max-width: 740px)'>
                <MobileWrapper submit={this.submit.bind(this)} componentFor={this.componentFor.bind(this)} {...this.props} />
            </MediaQuery>

            <MediaQuery query='(min-width: 741px)'>
                <DesktopWrapper submit={this.submit.bind(this)} componentFor={this.componentFor.bind(this)} {...this.props} />
            </MediaQuery>
        </div>
    }
}


export default connect(
    mapStateToProps,
    mapDispatchToProps
)(Main);

