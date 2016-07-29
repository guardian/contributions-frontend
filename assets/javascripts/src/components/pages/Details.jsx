import React from 'react';

export default class Details extends React.Component {
    render() {
        return <div>
            <input
                type="text"
                value={this.props.details.name}
                onChange={(event)=>{event.target.checkValidity();this.props.updateDetails({name: event.target.value})}}
                required
                />
            <input
                type="email"
                value={this.props.details.email}
                onChange={(event)=>{event.target.checkValidity();this.props.updateDetails({email: event.target.value})}}
                required
            />
            <input
                type="text"
                value={this.props.details.postcode}
                onChange={(event)=>{event.target.checkValidity();this.props.updateDetails({postcode: event.target.value})}}
                required
            />
        </div>
    }
}
