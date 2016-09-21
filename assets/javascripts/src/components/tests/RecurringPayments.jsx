import React from 'react';

import {Button} from '../Buttons';

export class RecurringSelection extends React.Component {
    render() {
        return <div className="recurring-payment">
            <h2>How often?</h2>

            <div className="flex-horizontal full-row">
                <Button type="button" className={'option-button option-button__recurring ' + (this.props.recurring === false ? 'active' : '')}
                        onClick={() => this.props.setRecurring(false)}>
                    One-off
                </Button>

                <Button type="button" className={'option-button option-button__recurring ' + (this.props.recurring === true ? 'active' : '') + (this.props.recurringNotified ? 'opaque' : '')}
                        onClick={() => !this.props.recurringNotified && this.props.setRecurring(true)}>
                    Monthly
                </Button>
            </div>
        </div>;
    }
}
