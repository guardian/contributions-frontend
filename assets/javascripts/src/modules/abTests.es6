import * as ophan from 'src/modules/analytics/ophan';

import store from 'src/store';

import { SET_AMOUNT } from 'src/actions';

export function init() {
    const state = store.getState();
    const data = {};

    for (var test of state.data.abTests) {
        data[test.testSlug] = {
            'variantName': test.variantSlug,
            'complete': 'false'
        }
    }

    ophan.loaded.then(function (ophan) {
        ophan.record({
            abTestRegister: data
        })
    });

    // only set the amount from the A/B test if it isn't already set
    // this prevents the A/B test overriding the preset amount (query param) functionality)
    if (isNaN(parseInt(state.card.amount))) {
        store.dispatch({ type: SET_AMOUNT, amount: presetAmount(state.data.abTests) });
    }
}

function testDataFor(tests, testName) {
    const test = tests.find(t => t.testName == testName);
    return test && test.data;
}

export function amounts(tests) {
    const data = testDataFor(tests, 'AmountHighlightTest');
    const defaultAmounts = [25, 50, 100, 250];
    return (data && data.values) || defaultAmounts;
}

export function paymentMethods(tests) {
    return {
        paymentMethods: testDataFor(tests, 'PaymentMethodTest').paymentMethods,
        isControl: function () {
            return (this.paymentMethods.indexOf("CARD") >= 0 && this.paymentMethods.length == 1);
        }
    }
}

export function postcode(tests) {
    return testDataFor(tests, 'PostcodeTest').displayPostcode;
}

export function presetAmount(tests) {
    const data = testDataFor(tests, 'AmountHighlightTest');

    return data && data.preselect;
}
