import React from 'react';
import { connect } from 'react-redux';

import { GO_FORWARD, GO_BACK } from 'src/actions';

import Contribution from './pages/Contribution.jsx';
import Details from './pages/Details.jsx';
import Payment from './pages/Payment.jsx';

import Title from './Title.jsx';
import ProgressIndicator from './ProgressIndicator.jsx';
import Navigation from './Navigation.jsx';

class Main extends React.Component {
    componentFor(page) {
        switch (page) {
            case 1: return <Contribution />;
            case 2: return <Details />;
            case 3: return <Payment />;
        }
    }

    render() {
        return (
            <div>
                <div className="contribute-form__heading">
                    <Title page={this.props.page} />
                    <ProgressIndicator page={this.props.page} />
                </div>

                {this.componentFor(this.props.page)}

                <Navigation page={this.props.page} goBack={this.props.goBack} goForward={this.props.goForward} />
            </div>
        );
    }
}

function mapStateToProps(state) {
    return { page: state.page }
}

function mapDispatchToProps(dispatch) {
    return {
        goBack: () => dispatch({ type: GO_BACK }),
        goForward: () => dispatch({ type: GO_FORWARD })
    }
}

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(Main);
