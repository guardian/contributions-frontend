import * as ophan from 'src/modules/analytics/ophan';
import * as GA from 'src/modules/analytics/ga';

import store from 'src/store';

import { SET_AMOUNT } from 'src/actions';

export function init() {
    const state = store.getState();

    registerTestsWithOphan(state.data.abTests, false);
    completeAARecurringTestIfApplicable(state.data.abTests);

    // only set the amount from the A/B test if it isn't already set
    // this prevents the A/B test overriding the preset amount (query param) functionality)
    if (isNaN(parseInt(state.card.amount))) {
        store.dispatch({type: SET_AMOUNT, amount: presetAmount(state.data.abTests)});
    }
}

function registerTestsWithOphan(tests, complete) {
    const data = {};

    for (var test of tests) {
        data[test.testSlug] = {
            'variantName': test.variantSlug,
            'complete': String(complete)
        }
    }

    ophan.loaded.then(function (ophan) {
        ophan.record({
            abTestRegister: data
        })
    })
}

function registerTestWithOphan(test, complete) {
    registerTestsWithOphan([test], complete)
}

/**
 * If the tests contains the AA recurring test, its complete flag is set to true in Ophan, and an event is sent to GA.
 * The aim of this is to check that the two methods of tracking the test reconcile.
 *
 * @param tests an array of tests
 */
function completeAARecurringTestIfApplicable(tests) {
    // only one variant name to check
    const targetTest = tests.find(test =>
        (test.testName == 'AARecurringTest') && (test.variantName == 'default')
    );

    if (targetTest !== undefined) {
        registerTestWithOphan(targetTest, true);
        GA.waitForGA().then(_ => GA.event('Test', 'NoAction', 'AARecurringTest'));
    }
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
