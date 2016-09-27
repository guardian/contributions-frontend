import React from 'react';

import {Button} from '../Buttons';

export default class RecurringNotification extends React.Component {
    render() {
        return <div className="notification-outer">
            <div className="notification">
                <h2 className="contribute-form__title">Thank you for your interest in monthly contributions</h2>

                <p>At the moment we can only accept one-off contributions but we’ll be exploring new options in the future.</p>
                <p>Email <a href="mailto:contributions.help@theguardian.com">contributions.help@theguardian.com</a> if you’d like to know more.</p>

                <Button className="action action--button contribute-navigation__next"
                        onClick={this.props.dismiss}>
                    OK
                </Button>
            </div>
        </div>;
    }
}
