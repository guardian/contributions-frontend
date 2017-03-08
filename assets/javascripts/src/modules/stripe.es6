import {SET_STRIPE_HANDLER, SUBMIT_PAYMENT, processStripePayment} from 'src/actions';
import {inStripeCheckoutTest} from 'src/modules/abTests';

import store from 'src/store';

export function init() {
    if (inStripeCheckoutTest()) initStripeCheckout();
    else initStripeJS();
}

function initStripeCheckout() {
    const handler = StripeCheckout.configure({
        key: guardian.stripe.key,
        image: guardian.stripe.image,
        locale: 'auto',
        name: 'The Guardian',
        description: 'Make a contribution',
        allowRememberMe: false,
        zipCode: false,
        token: token => {
            store.dispatch({ type: SUBMIT_PAYMENT });
            processStripePayment(token);
        }
    });

    store.dispatch({
        type: SET_STRIPE_HANDLER,
        handler: handler
    });
}

function initStripeJS() {
    Stripe.setPublishableKey(guardian.stripe.key);
}


export function createToken(card) {
    return new Promise((resolve, reject) => {
        Stripe.card.createToken({
            number: card.number,
            cvc: card.cvc,
            exp: card.expiry
        }, (status, response) => {
            if (response.error) reject(response.error);
            else resolve(response);
        });
    });
};
