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
        store.dispatch({type: SET_AMOUNT, amount: presetAmount(state.data.abTests)});
    }
}

function testDataFor(tests, testName) {
    const test = tests.find(t => t.testName == testName);
    return test && test.data;
}

function countryId() {
    try {
        return store.getState().data.countryGroup.id;
    } catch (e) {
        return '';
    }
}

export function amounts(tests) {
    const data = testDataFor(tests, 'AmountHighlightTest');
    const defaults = countryId() === 'au' ? [50, 100, 250, 500] : [25, 50, 100, 250];

    return (data && data.values) || defaults;
}
export function reducedCheckout(tests) {
    return (tests[0].testName == 'ReducedCheckoutTest') && (tests[0].variantName = 'test')
}

export function presetAmount(tests) {
    const data = testDataFor(tests, 'AmountHighlightTest');
    const defaultAmount = countryId() === 'au' ? 100 : 25;

    return (data && data.preselect) || defaultAmount;
}
