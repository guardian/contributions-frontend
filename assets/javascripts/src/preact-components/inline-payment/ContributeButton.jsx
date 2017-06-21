import { h } from 'preact';
/** @jsx h */


export default class ContributeButton {

    render() {
        return <button type="button" id="contribution-button"></button>;
    }

    componentDidMount() {
        console.log(this);

        paypal.Button.render({
            env: 'sandbox', // Or 'sandbox',
            commit: true, // Show a 'Pay Now' button,
            style: {
                color: 'gold',
                label: 'pay',
                size: 'responsive'
            },

            payment: function (data, actions) {
                console.log(data, actions);

                return fetch('/paypal/auth', {
                    method: 'POST',
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(authRequestData)
                })
                    .then(response => response.json())
                    .then(data => data.paymentId);
            },
            onAuthorize: function (data, actions) {
                console.log('onAuthorize', data, actions);
            }
        }, '#contribution-button');
    }
}
