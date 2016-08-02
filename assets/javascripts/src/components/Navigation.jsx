import React from 'react';
import {PAGES} from 'src/actions';

export default class Navigation extends React.Component {

    render() {
        const fwd = () => {
            if (!(this.props.page === PAGES.PAYMENT || this.props.page === PAGES.PROCESSING))
            {return <a onClick={this.props.goForward}>forward</a>}
        };
        const bk = () => {
            if (!(this.props.page === PAGES.CONTRIBUTION || this.props.page === PAGES.PROCESSING))
            {return <a onClick={this.props.goBack}>back</a>;}
        };

        return <div>
            {bk()}
            {fwd()}
        </div>;
    }
}
