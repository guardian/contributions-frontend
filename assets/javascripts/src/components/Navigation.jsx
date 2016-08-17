import React from 'react';

import Spinner from './Spinner.jsx';
import { PAGES } from 'src/constants';

export default class Navigation extends React.Component {
    render() {
        const showForward = !this.props.processing &&
            this.props.page !== PAGES.PAYMENT &&
            this.props.page !== PAGES.PROCESSING;

        const showBack = !this.props.processing &&
            this.props.page !== PAGES.CONTRIBUTION &&
            this.props.page !== PAGES.PROCESSING;

        const showPay = !this.props.processing &&
            this.props.page === PAGES.PAYMENT;

        return <div className="contribute-navigation">
          {showBack && <a className="contribute-navigation__back" onClick={this.props.goBack}>Back</a> }
          {showForward && <button className="contribute-navigation__button contribute-navigation__next action action--button action--next">Next</button>}
          {showPay && <button className="contribute-navigation__button contribute-navigation__pay action action--button action--pay">Contribute {this.props.symbol}{this.props.amount}</button>}
          {this.props.processing && <Spinner text="Processing" />}
        </div>;
    }
}