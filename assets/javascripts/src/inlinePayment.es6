import 'whatwg-fetch';
import { h, render } from 'preact';

import InlinePaymentForm from 'src/preact-components/inline-payment/Form';

/** @jsx h */


render(<InlinePaymentForm amounts={[1,5,10,25]} symbol="£" />, document.getElementById('inline-form'));

/**
(function init() {

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
        authRequestData.token = token.id;
        authRequestData.email = token.email
    }

    function initPaypal() {
        paypal.Button.render({
            env: 'sandbox', // Or 'sandbox',
            commit: true, // Show a 'Pay Now' button,
            style: {
                color: 'gold',
                label: 'pay',
                size: 'responsive'
            },
            payment: function () {

                console.log(`amount from form: ${getAmountFromForm()}`);
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

    const inputAmount = document.getElementById('js-contribution-amount-input');
    const buttons = Array.from(document.getElementsByClassName('contributions-inline-epic__button--amount'));

    function initFormEventListeners() {
        buttons.forEach(button => {
            button.addEventListener('click', event => {
                inputAmount.value = null;
            })
        })
    }

    function getAmountFromForm() {

        const amountFromInput = parseFloat(inputAmount.value);

        const activeButton = buttons.find(button => {
            console.log(button, document.activeElement);

            button === document.activeElement
        });
        const amountFromButton = activeButton && parseFloat(activeButton.dataset.amount);

        return amountFromInput || amountFromButton
    }

    initPaypal();
    initFormEventListeners();


})();
**/

