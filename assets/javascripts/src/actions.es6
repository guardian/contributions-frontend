import 'whatwg-fetch';

import store from 'src/store';
import { urls } from 'src/constants';
import * as stripe from 'src/modules/stripe';

export const SET_AB_TESTS = "SET_AB_TESTS";

export const GO_BACK = "GO_BACK";
export const GO_FORWARD = "GO_FORWARD";
export const SET_AMOUNT = "SET_AMOUNT";
export const UPDATE_DETAILS = "UPDATE_DETAILS";
export const UPDATE_CARD = "UPDATE_CARD";

export const SUBMIT_PAYMENT = "SUBMIT_PAYMENT";
export const PAYMENT_COMPLETE = "PAYMENT_COMPLETE";
export const PAYMENT_ERROR = "PAYMENT_ERROR";

export function submitPayment(dispatch) {
    const state = store.getState();

    dispatch({ type: SUBMIT_PAYMENT });

    stripe.createToken(state.card)
        .then(token => paymentFormData(state, token))
        .then(data => fetch(urls.pay, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        }))
        .then(() => dispatch({ type: PAYMENT_COMPLETE }))
        .catch(error => dispatch({ type: PAYMENT_ERROR, error: error }));
}

/**
 * Convert app state to the structure required for payment posts
 *
 * @param state
 * @return object
 */
function paymentFormData(state, token) {
    return {
        name: state.details.name,
        currency: 'GBP',
        amount: state.card.amount,
        email: state.details.email,
        token: token,
        marketing: state.details.optIn,
        postCode: state.details.postCode,
        abTests: state.abTests,
        ophanId: 1,
        cmp: '',
        intcmp: ''
    };
}
