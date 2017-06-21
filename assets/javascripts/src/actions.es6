import 'whatwg-fetch';

import store from 'src/store';
import { urls } from 'src/constants';
import { trackCheckout, trackPayment } from 'src/modules/analytics/ga';
import { completeTests, inStripeCheckoutTest } from 'src/modules/abTests';
import * as stripe from 'src/modules/stripe';

export const SET_DATA = "SET_DATA";
export const SET_COUNTRY_GROUP = "SET_COUNTRY_GROUP";

export const GO_BACK = "GO_BACK";
export const GO_FORWARD = "GO_FORWARD";
export const SET_AMOUNT = "SET_AMOUNT";
export const UPDATE_DETAILS = "UPDATE_DETAILS";
export const UPDATE_CARD = "UPDATE_CARD";

export const SUBMIT_PAYMENT = "SUBMIT_PAYMENT";
export const PAYMENT_COMPLETE = "PAYMENT_COMPLETE";
export const PAYMENT_ERROR = "PAYMENT_ERROR";

export const PAYPAL_PAY = "PAYPAL_PAY";
export const CARD_PAY = "CARD_PAY";
export const JUMP_TO_PAGE = "JUMP_TO_PAGE";
export const CLEAR_PAYMENT_FLAGS = "CLEAR_PAYMENT_FLAGS";

export const SET_STRIPE_HANDLER = 'SET_STRIPE_HANDLER';

export const TRACK_STEP = "TRACK_STEP";
export const GA_ENABLED = "GA_ENABLED";

export const AUTOFILL = "AUTOFILL";

export function submitPayment(dispatch) {
    const state = store.getState();

    if (inStripeCheckoutTest()) {
        state.data.stripe.handler.open({ email: state.details.email });
    }
    else {
        dispatch({type: SUBMIT_PAYMENT});
        stripe.createToken(state.card).then(processStripePayment);
    }
}

export function processStripePayment(token) {
    const state = store.getState();
    const data = paymentFormData(state, token.id);

    return fetch(urls.pay, {
        credentials: 'same-origin',
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(data)
    }).then(response => response.json().then(json => {
        return { response: response, json: json }
    })).then(response => {
        if (response.response.ok) {
            return trackPayment(state.card.amount, state.data.currency.code)
                .then(completeTests)
                .then(() => store.dispatch({type: PAYMENT_COMPLETE, response: response.json}))
                .then(() => response);
        }
        else {
            return store.dispatch({type: PAYMENT_ERROR, kind: 'card', error: response.json});
        }
    }).catch(error => store.dispatch({type: PAYMENT_ERROR, kind: 'network', error: error}));
}

export function paypalRedirect(dispatch) {
    const state = store.getState();
    dispatch({ type: SUBMIT_PAYMENT });

    const postData = {
        countryGroup: state.data.countryGroup.id ,
        amount: state.card.amount, //TODO should the amount be somewhere else rather than in the card section?,
        cmp: state.data.cmpCode,
        intCmp: state.data.intCmpCode,
        refererPageviewId: state.data.refererPageviewId,
        refererUrl: state.data.refererUrl,
        ophanPageviewId: state.data.ophan.pageviewId,
        ophanBrowserId: state.data.ophan.browserId,
        ophanVisitId: state.data.ophan.visitId
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
            throw response;
        }
    })
    .then(response => {
        return trackPayment(state.card.amount, state.data.currency.code)
            .then(completeTests)
            .then(() => response);
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
        intcmp: state.data.intCmpCode,
        refererPageviewId: state.data.refererPageviewId,
        refererUrl: state.data.refererUrl,
        ophanVisitId: state.data.ophan.visitId
    };
}
