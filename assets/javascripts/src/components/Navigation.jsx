import React from 'react';
import { PAGES } from 'src/constants';

export default class Navigation extends React.Component {
    render() {
        const showForward = this.props.page !== PAGES.PAYMENT && this.props.page !== PAGES.PROCESSING;
        const showBack = this.props.page !== PAGES.CONTRIBUTION && this.props.page !== PAGES.PROCESSING;

        return <div>
          {showBack && <a onClick={this.props.goBack}>back</a> }
          {showForward && <button>forward</button>}
        </div>;
    }
}
