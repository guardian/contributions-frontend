import React from 'react';

import {Button} from '../Buttons';

export default class RecurringNotification extends React.Component {
    render() {
        return <div className="notification-outer">
            <div className="notification">
                <div className="arrow-up"></div>
                <h2 className="contribute-form__title">Thank you for your interest in monthly contributions</h2>

                <p>At the moment we can only accept one-time contributions, but we’re gauging interest in recurring payments.</p>
                <p>In the meantime, why not <a href="https://membership.theguardian.com/supporter?INTCMP=recurring_contribution" target="_blank">become a supporter</a>?</p>

                <Button className="action action--button contribute-navigation__next"
                        onClick={this.props.dismiss}
                        autoFocus="true">
                    OK
                </Button>
            </div>
        </div>;
    }
}
