import React from 'react';
import { connect } from 'react-redux';

import { GO_FORWARD, GO_BACK, UPDATE_DETAILS, UPDATE_CARD, PAY, SET_AMOUNT } from 'src/actions';
import {  PAGES, ALL_PAGES } from 'src/constants';

import Contribution from './pages/Contribution.jsx';
import Details from './pages/Details.jsx';
import Payment from './pages/Payment.jsx';
import Processing from './pages/Processing.jsx';

import Title from './Title.jsx';
import ProgressIndicator from './ProgressIndicator.jsx';
import Navigation from './Navigation.jsx';

function mapStateToProps(state) {
    return {
        page: state.page.page,
        processing: state.page.processing,
        details: state.details,
        card: state.card,
        symbol: '£'
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

class Main extends React.Component {
    componentFor(page) {
        switch (page) {
            case PAGES.CONTRIBUTION:
                return <Contribution amounts={[25, 50, 100, 250]}
                                     max={2000}
                                     symbol={this.props.symbol}
                                     setAmount={this.props.setAmount}
                                     currentAmount={this.props.card.amount} />;

            case PAGES.DETAILS:
                return <Details details={this.props.details}
                                updateDetails={this.props.updateDetails} />;

            case PAGES.PAYMENT:
                return <Payment card={this.props.card}
                                updateCard={this.props.updateCard} />;

            case PAGES.PROCESSING:
                return <Processing />;
        }
    }

    submit(event) {
        event.preventDefault();

        if (this.props.page === PAGES.PAYMENT) {
            this.props.pay();
        } else {
            this.props.goForward();
        }
    }

    render() {
        return <div>
            {ALL_PAGES.map(p =>
                <section className={'contribute-section ' + (this.props.page === p ? 'current' : '')} key={p}>
                    <div className="contribute-form__heading">
                        <Title page={p}/>
                        <ProgressIndicator page={this.props.page}/>
                    </div>

                    <form className={'flex-vertical contribute-form__inner'}
                          onSubmit={this.submit.bind(this)} key={p}>

                        {this.componentFor(p)}

                        <Navigation
                            page={this.props.page}
                            goBack={this.props.goBack}
                            amount={this.props.card.amount}
                            symbol={this.props.symbol}
                            processing={this.props.processing}
                            pay={this.props.pay} />
                    </form>
                </section>
            )}
        </div>;
    }
}

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(Main);
