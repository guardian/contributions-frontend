import React from 'react';

import { InputField } from '../InputField.jsx';

export default class Details extends React.Component {
    render() {
        return <div className='contribute-details contribute-fields'>

            <InputField label="Full name" id="name" type="text"
                        value={this.props.details.name}
                        onChange={ event => this.props.updateDetails({ name: event.target.value })}
                        required autoFocus />

            <InputField label="Email" id="email" type="email"
                        value={this.props.details.email}
                        onChange={ event => this.props.updateDetails({ email: event.target.value })}
                        required />

            <InputField label="Postcode" id="postcode" type="text"
                        value={this.props.details.postcode}
                        onChange={ event => this.props.updateDetails({ postcode: event.target.value })}
                        />

            <div className="giraffe-checkbox">
                <input id="guardian-opt-in" type="checkbox" name="guardian-opt-in"
                       checked={this.props.details.optIn}
                       onChange={event => this.props.updateDetails({ optIn: event.target.checked })} />
                <label htmlFor="guardian-opt-in">Keep me up to date with offers from the Guardian</label>
            </div>
        </div>
    }
}
