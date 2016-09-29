import React from 'react';

import {Button} from '../Buttons';

export class RecurringSelection extends React.Component {
    render() {
        return <div className="recurring-payment">
            <h2 className="contribution-heading">How often would you like to give?</h2>

            <div className="flex-horizontal full-row">
                <Button type="button" className={'option-button option-button__recurring ' + (this.props.recurring === false ? 'active' : '')}
                        onClick={() => this.props.setRecurring(false)}>
                    Once
                </Button>

                <Button type="button" className={'option-button option-button__recurring ' + (this.props.recurring === true ? 'active' : '') + (this.props.recurringNotified ? 'opaque' : '')}
                        onClick={() => !this.props.recurringNotified && this.props.setRecurring(true)}>
                    Monthly
                </Button>
            </div>
        </div>;
    }
}
