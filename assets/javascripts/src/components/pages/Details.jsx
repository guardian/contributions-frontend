import React from 'react';

export default class Details extends React.Component {
    render() {
        return <div>
            <input
                type="text"
                value={this.props.details.name}
                onChange={(event)=>{this.props.updateDetails({ name: event.target.value })}}
                required
                />
            <input
                type="email"
                value={this.props.details.email}
                onChange={(event)=>{this.props.updateDetails({ email: event.target.value })}}
                required
            />
            <input
                type="text"
                value={this.props.details.postcode}
                onChange={(event)=>{this.props.updateDetails({ postcode: event.target.value })}}
                required
            />
        </div>
    }
}
