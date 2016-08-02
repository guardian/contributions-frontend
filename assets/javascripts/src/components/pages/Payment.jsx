import React from 'react';

export default class Payment extends React.Component {
    render() {
        return <div>
            <input
                type="text"
                value={this.props.card.number}
                onChange={(event)=>{console.log(event);event.target.checkValidity();this.props.updateCard({number: event.target.value})}}
                required
            />
            <input
                type="text"
                value={this.props.card.cvc}
                onChange={(event)=>{event.target.checkValidity();this.props.updateCard({cvc: event.target.value})}}
                required
            />
            <input
                type="text"
                value={this.props.card.exp}
                onChange={(event)=>{event.target.checkValidity();this.props.updateCard({exp: event.target.value})}}
                required
            />
            <button
                onClick={this.props.pay}
                onSubmit={this.props.pay}
            >money</button>
        </div>
    }
}
