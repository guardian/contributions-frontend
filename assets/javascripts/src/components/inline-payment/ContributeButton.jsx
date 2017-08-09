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
            env: 'production', // change to 'sandbox' to allow payments in dev mode
            commit: true,
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
