import React from 'react';
import MediaQuery from 'react-responsive';
import { connect } from 'react-redux';

import { GO_FORWARD, GO_BACK, UPDATE_DETAILS, UPDATE_CARD, SET_AMOUNT, submitPayment } from 'src/actions';
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
        paymentError: state.page.paymentError,
        amounts: abTests.amounts(state.data.abTests),
        countryGroup: state.data.countryGroup
    };
}

function mapDispatchToProps(dispatch) {
    return {
        goBack: () => dispatch({ type: GO_BACK }),
        goForward: () => dispatch({ type: GO_FORWARD }),
        setAmount: a => dispatch({ type: SET_AMOUNT, amount: a }),
        updateDetails: d => dispatch({ type: UPDATE_DETAILS, details: d }),
        updateCard: c => dispatch({ type: UPDATE_CARD, card: c }),
        pay: () => dispatch(submitPayment)
    };
}

class Main extends React.Component {
    componentFor(page) {
        switch (page) {
            case PAGES.CONTRIBUTION:
                return <Contribution amounts={this.props.amounts}
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
                                error={this.props.paymentError}
                                countryGroup={this.props.countryGroup}
                />;
        }
    }

    submit(mobile, event) {
        event.preventDefault(); // we never want the standard submit behaviour, which triggers a reload

        if (!event.target.checkValidity()) return;

        if (this.props.page === PAGES.PAYMENT || mobile) {
            this.props.pay();
        } else {
            this.props.goForward();
        }
    }

    render() {
        const showSummary = !!this.props.card.amount && this.props.page !== PAGES.CONTRIBUTION;

        return <div>
            <AmountSummary currency={this.props.currency} amount={this.props.card.amount} visible={showSummary} />

            <MediaQuery query='(max-width: 740px)'>
                <MobileWrapper submit={this.submit.bind(this, true)} componentFor={this.componentFor.bind(this)} {...this.props} />
            </MediaQuery>

            <MediaQuery query='(min-width: 741px)'>
                <DesktopWrapper submit={this.submit.bind(this, false)} componentFor={this.componentFor.bind(this)} {...this.props} />
            </MediaQuery>
        </div>
    }
}


export default connect(
    mapStateToProps,
    mapDispatchToProps
)(Main);
