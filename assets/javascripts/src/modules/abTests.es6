import * as ophan from 'src/modules/analytics/ophan';
import * as GA from 'src/modules/analytics/ga';

import store from 'src/store';

import { SET_AMOUNT } from 'src/actions';

export function init() {
    const state = store.getState();

    registerTestsWithOphan(state.data.abTests, false);

    // only set the amount from the A/B test if it isn't already set
    // this prevents the A/B test overriding the preset amount (query param) functionality)
    if (isNaN(parseInt(state.card.amount))) {
        store.dispatch({type: SET_AMOUNT, amount: presetAmount(state.data.abTests)});
    }
}

function registerTestsWithOphan(tests, complete) {
    const data = tests && tests.reduce((obj, test) => {
        obj[test.testSlug] = {
            'variantName': test.variantSlug,
            'complete': String(complete)
        }
    }, {}) || {};

    return ophan.record({
        abTestRegister: data
    });
}

function registerTestWithOphan(test, complete) {
    registerTestsWithOphan([test], complete)
}

function testFor(tests, testName) {
    return tests && tests.find(t => t.testName == testName);
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
    };

    const state = store.getState();
    const data = testDataFor(state.data.abTests, 'AmountHighlightTest');
    const defaultAmounts = amounts[countryId()] || amounts['default'];
    const defaults = state.details.recurring === true ? defaultAmounts['monthly'] : defaultAmounts['one-off'];

    return (data && data.values) || defaults;
}

export function presetAmount(tests) {
    const data = testDataFor(tests, 'AmountHighlightTest');
    const defaultAmount = countryId() === 'au' ? 100 : 50;

    return (data && data.preselect) || defaultAmount;
}
