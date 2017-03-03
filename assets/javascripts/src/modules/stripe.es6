import {SET_STRIPE_HANDLER, processStripePayment} from 'src/actions';
import store from 'src/store';

export function init() {
    const handler = StripeCheckout.configure({
        key: guardian.stripe.key,
        image: guardian.stripe.image,
        locale: 'auto',
        name: 'The Guardian',
        description: 'Make a contribution',
        allowRememberMe: false,
        zipCode: false,
        token: token => processStripePayment(token).then(() => {
            console.log('handler');
        })
    });

    store.dispatch({
        type: SET_STRIPE_HANDLER,
        handler: handler
    });
}

export function createToken(card) {
    return new Promise((resolve, reject) => {
        Stripe.card.createToken({
            number: card.number,
            cvc: card.cvc,
            exp: card.expiry
        }, (status, response) => {
            if (response.error) reject(response.error);
            else resolve(response.id);
        });
    });
};
