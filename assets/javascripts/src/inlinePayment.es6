// TODO:
import 'whatwg-fetch';

const authRequestData = {
    countryGroup: 'uk',
    amount: '20',
    intCmp: 'PAYPAL_TEST',
    refererPageviewId: null,
    refererUrl: null,
    ophanPageviewId: null,
    ophanBrowserId: null,
};

function updateFormDataWithToken(token) {
    formData.token = token.id;
    formData.email = token.email
}

function init() {
    paypal.Button.render({
        env: 'sandbox', // Or 'sandbox',
        commit: true, // Show a 'Pay Now' button
        payment: function () {
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

init();
