import React from 'react';

import InputField from '../InputField.jsx';
import CardIcon from '../CardIcon.jsx';

export default class Payment extends React.Component {
    render() {
        return <div className='contribute-payment contribute-fields'>

            <InputField label="Card number" type="text" value={this.props.card.number}
                        onChange={event => this.props.updateCard({ number: event.target.value })}
                        tabIndex="13" size="20" id="cc-num" data-stripe="number"
                        pattern="[0-9]*" placeholder="0000 0000 0000 0000" maxLength="19" autoComplete="off"
                        outerClassName="with-card-icon" required autoFocus>
                <CardIcon number={this.props.card.number} />
            </InputField>

            <div className="flex-horizontal">
                <InputField label="Expiry date"
                            type="text"
                            value={this.props.card.expiry}
                            onChange={event => this.props.updateCard({ expiry: event.target.value })}
                            outerClassName="half-width"
                            required />

                <InputField label="Security code"
                            type="text"
                            value={this.props.card.cvc}
                            onChange={event => this.props.updateCard({ cvc: event.target.value })}
                            outerClassName="half-width"
                            required />
            </div>
        </div>
    }
}
