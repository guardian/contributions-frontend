import 'whatwg-fetch';

import store from 'src/store';
import { urls } from 'src/constants';
import * as stripe from 'src/modules/stripe';
import { trackCheckout, trackPayment } from 'src/modules/analytics/ga';

import {showCheckout} from 'src/modules/stripeCheckout'

export const SET_DATA = "SET_DATA";
export const SET_COUNTRY_GROUP = "SET_COUNTRY_GROUP";

export const GO_BACK = "GO_BACK";
export const GO_FORWARD = "GO_FORWARD";
export const SET_AMOUNT = "SET_AMOUNT";
export const UPDATE_DETAILS = "UPDATE_DETAILS";
export const UPDATE_CARD = "UPDATE_CARD";

export const OPEN_STRIPE = "OPEN_STRIPE";
export const CLOSE_STRIPE = "CLOSE_STRIPE";
export const SUBMIT_PAYMENT = "SUBMIT_PAYMENT";
export const PAYMENT_COMPLETE = "PAYMENT_COMPLETE";
export const PAYMENT_ERROR = "PAYMENT_ERROR";

export const PAYPAL_PAY = "PAYPAL_PAY";
export const CARD_PAY = "CARD_PAY";
export const JUMP_TO_PAGE = "JUMP_TO_PAGE";
export const CLEAR_PAYMENT_FLAGS = "CLEAR_PAYMENT_FLAGS";

export const TRACK_STEP = "TRACK_STEP";
export const GA_ENABLED = "GA_ENABLED";

export const AUTOFILL = "AUTOFILL";

export function openStripeCheckout(dispatch) {
    const state = store.getState();
    console.log('is this thing on');
    let hide = () => {
        dispatch({type:CLOSE_STRIPE});
    };
    let process = (token) => {
        dispatch({ type: SUBMIT_PAYMENT });
        let data = paymentFormData(state, token.id);
        fetch(urls.pay, {
            credentials: 'same-origin',
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        })
        .then(response => response.json() // response: Response
            .then(json => {
                return { response: response, json: json }
            }))
            .then((response) => {
                if (response.response.ok)
                    return trackPayment(state.card.amount, state.data.currency.code).then(() => response);
                else
                    return response;
            })
            .then(response => {
                if (response.response.ok) {
                    dispatch({ type: PAYMENT_COMPLETE, response: response.json })
                }
                else{
                    dispatch({ type: PAYMENT_ERROR, kind: 'card', error: response.json })
                }
            })
            .catch(error => dispatch({ type: PAYMENT_ERROR, kind: 'network', error: error }));
    };
    showCheckout(process,hide, state.details.email,state.card.amount,state.data.currency.code);
    dispatch({type: OPEN_STRIPE});


}

export function submitPayment(dispatch) {
    const state = store.getState();

    dispatch({ type: SUBMIT_PAYMENT });

    stripe.createToken(state.card)
        .then(token => paymentFormData(state, token))
        .then(data => fetch(urls.pay, {
            credentials: 'same-origin',
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        }))
        .then(response => response.json() // response: Response
            .then(json => {
                return { response: response, json: json }
            }))
        .then((response) => {
            if (response.response.ok)
                return trackPayment(state.card.amount, state.data.currency.code).then(() => response);
            else
                return response;
        })
        .then(response => {
            if (response.response.ok) dispatch({ type: PAYMENT_COMPLETE, response: response.json })
            else dispatch({ type: PAYMENT_ERROR, kind: 'card', error: response.json })
        })
        .catch(error => dispatch({ type: PAYMENT_ERROR, kind: 'network', error: error }));
}

export function paypalRedirect(dispatch) {
    const state = store.getState();
    dispatch({ type: SUBMIT_PAYMENT });

    const postData = {
        countryGroup: state.data.countryGroup.id ,
        amount: state.card.amount, //TODO should the amount be somewhere else rather than in the card section?,
        cmp: state.data.cmpCode,
        intCmp: state.data.intCmpCode,
        ophanPageviewId: state.data.ophan.pageviewId,
        ophanBrowserId: state.data.ophan.browserId
    };

    const url = '/paypal/auth?csrfToken=' + state.data.csrfToken;

    fetch(url, {
        credentials: 'same-origin',
        method: 'POST',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(postData)
    })
    .then(response => {
        if (response.ok) {
            return response.json();
        } else {
            dispatch({ type: PAYMENT_ERROR, kind: 'paypal', error: {message: 'Sorry, an error occurred, please try again or use another payment method.' }})
        }
    })
    .then(response => {
        return trackPayment(state.card.amount, state.data.currency.code).then(() => response);
    })
    .then((res) =>  window.location = res.approvalUrl)
    .catch(error => dispatch({ type: PAYMENT_ERROR, kind: 'paypal', error: {message: 'Sorry, an error occurred, please try again or use another payment method.' }}));
}

export function trackCheckoutStep(checkoutStep, actionName, label) {
    return (dispatch) => {
        const state = store.getState();

        // this condition is here to debounce events
        if (!state.gaTracking.steps[checkoutStep]) {
            trackCheckout(checkoutStep, actionName, label);
            dispatch({type: TRACK_STEP, step: checkoutStep});
        }
    }
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
        currency: state.data.currency.code,
        amount: state.card.amount,
        email: state.details.email,
        token: token,
        marketing: state.details.optIn,
        postcode: state.details.postcode,
        abTests: state.data.abTests,
        ophanPageviewId: state.data.ophan.pageviewId,
        ophanBrowserId: state.data.ophan.browserId,
        cmp: state.data.cmpCode,
        intcmp: state.data.intCmpCode
    };
}
