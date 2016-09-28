import React from 'react';

export default class Contribution extends React.Component {
    active(pos) {
        return this.props.page >= pos;
    }

    render() {
        return (
            <div className="contribute-form__progress">
                <span className={this.active(1) ? 'active' : ''}></span>
                <span className={this.active(2) ? 'active' : ''}></span>
                {!this.props.reducedCheckout && <span className={this.active(3) ? 'active' : ''}></span>}
            </div>
        );
    }
}
