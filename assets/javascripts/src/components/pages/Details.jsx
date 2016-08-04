import React from 'react';

export default class Details extends React.Component {
    render() {
        return <div className="contribute--details">
            <label htmlFor="name" className="label">Full name</label>
            <input
                className="input-text contribute-controls__input"
                id="name"
                type="text"
                value={this.props.details.name}
                onChange={(event)=>{this.props.updateDetails({ name: event.target.value })}}
                required />

            <label htmlFor="email" className="label">Email</label>
            <input
                className="input-text contribute-controls__input"
                id="email"
                type="email"
                value={this.props.details.email}
                onChange={(event)=>{this.props.updateDetails({ email: event.target.value })}}
                required />

            <label htmlFor="postcode" className="label">Postcode</label>
            <input
                className="input-text input-text--postcode contribute-controls__input"
                id="postcode"
                type="text"
                value={this.props.details.postcode}
                onChange={(event)=>{this.props.updateDetails({ postcode: event.target.value })}} />
        </div>
    }
}
