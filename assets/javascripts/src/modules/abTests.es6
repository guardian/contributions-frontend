import * as ophan from 'src/modules/analytics/ophan';
import * as GA from 'src/modules/analytics/ga';

import store from 'src/store';

import { SET_AMOUNT } from 'src/actions';

export function init() {
    const state = store.getState();

    ophan.loaded.then(function (ophan) {
        ophan.record({
            abTestRegister: abTestData(state.data.abTests, false)
        })
    });

    // only set the amount from the A/B test if it isn't already set
    // this prevents the A/B test overriding the preset amount (query param) functionality)
    if (isNaN(parseInt(state.card.amount))) {
        store.dispatch({type: SET_AMOUNT, amount: presetAmount(state.data.abTests)});
    }
}

function abTestData(tests, complete) {
    const data = {};

    for (var test of tests) {
        data[test.testSlug] = {
            'variantName': test.variantSlug,
            'complete': complete
        }
    }

    return data;
}

function trackComplete() {
    const state = store.getState();

    return ophan.loaded.then(function (ophan) {
        ophan.record({
            abTestRegister: abTestData(state.data.abTests, true)
        });
    });
}

function testFor(tests, testName) {
    return tests.find(t => t.testName == testName);
}

function testDataFor(tests, testName) {
    const test = testFor(tests, testName);
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
    const amounts = {
        'au': {
            'one-off': [50, 100, 250, 500],
            'monthly': [5, 10, 25, 50]
        },

        'default': {
            'one-off': [25, 50, 100, 250],
            'monthly': [2, 5, 10, 20]
        }
    }

    const state = store.getState();
    const data = testDataFor(state.data.abTests, 'AmountHighlightTest');
    const defaultAmounts = amounts[countryId()] || amounts['default'];
    const defaults = state.details.recurring === true ? defaultAmounts['monthly'] : defaultAmounts['one-off'];

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

export function showRecurring(tests) {
    const test = testFor(tests, 'RecurringPaymentTest');
    return test && test.variantSlug === 'recurring';
}

export function trackRecurring(amount) {
    const state = store.getState();
    const currency = state.data.currency.code || '';

    GA.event('recurring', 'Pay monthly', currency , amount);
    trackComplete();
}
