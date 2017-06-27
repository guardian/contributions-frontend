import { h } from 'preact';
/** @jsx h */


export default class ContributeButton {
    render() {
        return <div class="paypal-button-wrapper">
            <button type="button" id="contribution-button"></button>
        </div>;
    }

    componentDidMount() {
        paypal.Button.render({
            env: 'sandbox', // Or 'sandbox',
            commit: true, // Show a 'Pay Now' button,
            style: {
                color: 'gold',
                label: 'pay',
                size: 'responsive'
            },

            payment: (data, actions) => {
                return this.props.sendPaypalRequest()
                    .then(response => response.json())
                    .then(data => data.paymentId)
            },

            onAuthorize: data => {
                return this.props.executePaypalPayment(data.returnUrl)
            }
        }, '#contribution-button');
    }
}
