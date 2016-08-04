import React from 'react';
import { connect } from 'react-redux';

import { GO_FORWARD, GO_BACK, UPDATE_DETAILS, UPDATE_CARD, PAY, SET_AMOUNT } from 'src/actions';
import { PAGES } from 'src/constants';

import Contribution from './pages/Contribution.jsx';
import Details from './pages/Details.jsx';
import Payment from './pages/Payment.jsx';
import Processing from './pages/Processing.jsx';

import Title from './Title.jsx';
import ProgressIndicator from './ProgressIndicator.jsx';
import Navigation from './Navigation.jsx';

class Main extends React.Component {
    componentFor(page) {
        switch (page) {
            case PAGES.CONTRIBUTION: return <Contribution setAmount={this.props.setAmount} currentAmount={this.props.card.amount} />;
            case PAGES.DETAILS: return <Details details={this.props.details} updateDetails={this.props.updateDetails} />;
            case PAGES.PAYMENT: return <Payment card={this.props.card} updateCard={this.props.updateCard} pay={this.props.pay} />;
            case PAGES.PROCESSING: return <Processing />;
        }
    }

    submit(event) {
        event.preventDefault();
        this.props.goForward();
    }

    render() {
        return (
            <section>
                <div className="contribute-form__heading">
                    <Title page={this.props.page} />
                    <ProgressIndicator page={this.props.page} />
                </div>

                <form className="flex-vertical contribute-form__inner" onSubmit={this.submit.bind(this)}>
                    {this.componentFor(this.props.page)}
                    <Navigation page={this.props.page} goBack={this.props.goBack} />
                </form>
            </section>
        );
    }
}

function mapStateToProps(state) {
    return {
        page: state.page,
        details: state.details,
        card: state.card
    };
}

function mapDispatchToProps(dispatch) {
    return {
        goBack: () => dispatch({ type: GO_BACK }),
        goForward: () => dispatch({ type: GO_FORWARD }),
        setAmount: a => dispatch({ type: SET_AMOUNT, amount: a }),
        updateDetails: d => dispatch({ type: UPDATE_DETAILS, details: d }),
        updateCard: c => dispatch({ type: UPDATE_CARD, card: c }),
        pay: () => dispatch({ type: PAY })
    };
}

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(Main);
