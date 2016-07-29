import React from 'react';

export default class Navigation extends React.Component {

    render() {
        const fwd = () => {
            if (this.props.page !== 3)
            {return <a onClick={this.props.goForward}>forward</a>}
        };
        const bk = () => {
            if (this.props.page !== 1)
            {return <a onClick={this.props.goBack}>back</a>;}
        };

        return <div>
            {bk()}
            {fwd()}
        </div>;
    }
}
