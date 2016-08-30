import React from 'react';

export default class CardIcon extends React.Component {
    cardType(number) {
        return Stripe.cardType(number).toLowerCase().replace(' ', '-')
    }

    render() {
        return <i className={'credit-card--input-visualisation sprite-card sprite-card--' + this.cardType(this.props.number)}></i>;
    }
}
