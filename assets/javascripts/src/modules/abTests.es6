import * as ophan from 'src/modules/analytics/ophan';
import * as GA from 'src/modules/analytics/ga';

import store from 'src/store';

import { SET_AMOUNT } from 'src/actions';

export function init() {
    const state = store.getState();

    registerTestsWithOphan(state.data.abTests, initTestFlags);

    registerAARecurringTestWithGA(state.data.abTests);

    // only set the amount from the A/B test if it isn't already set
    // this prevents the A/B test overriding the preset amount (query param) functionality)
    if (isNaN(parseInt(state.card.amount))) {
        store.dispatch({type: SET_AMOUNT, amount: presetAmount(state.data.abTests)});
    }
}

/**
 * Check if an object is empty.
 * @see http://stackoverflow.com/questions/679915/how-do-i-test-for-an-empty-javascript-object
 */
function isEmpty(obj) {
    return Object.keys(obj).length === 0 && obj.constructor === Object;
}

/**
 * The function f should take a test and return the complete flag (true/false) for the test.
 * If the function returns null, the complete flag for the test will not be set.
 * Any tests which have their complete flag set are registered with Ophan.
 */
function registerTestsWithOphan(tests, f) {
    const data = {};

    for (var test of tests) {
        const completeFlag = f(test);

        if (completeFlag !== null) {
            data[test.testSlug] = {
                'variantName': test.variantSlug,
                'complete': String(completeFlag)
            }
        }
    }

    if (!isEmpty(data)) {
        ophan.loaded.then(function (ophan) {
            ophan.record({
                abTestRegister: data
            })
        })
    }
}

/**
 * @see AARecurringTest for the corresponding Scala class.
 */
function isAARecurringTest(test) {
    // Only one variant name to check.
    return (test.testName == 'AARecurringTest') && (test.variantName == 'default');
}

/**
 * To be used with registerTestsWithOphan().
 * Sets a test's completed flag to false, unless it is the AA recurring test,
 * in which case its complete flag is set to true.
 */
function initTestFlags(test) {
   return isAARecurringTest(test);
}

/**
 * To be used with registerTestsWithOphan().
 * Set a test's complete flag to true, apart from the AA recurring test which is ignored.
 */
function completeTestFlags(test) {
    if (isAARecurringTest(test)) {
        return null;
    } else {
        return true;
    }
}

/**
 * Send an AA recurring test event to GA.
 * @see https://developers.google.com/analytics/devguides/collection/analyticsjs/events
 */
function sendAARecurringTestEvent() {
    GA.event('no-object', 'pageview', 'aa-recurring-test')
}

/**
 * Sends one recurring test to GA iff the tests include at least one AA recurring test.
 */
function registerAARecurringTestWithGA(tests) {
    for (var test of tests) {
        if (isAARecurringTest(test)) {
            sendAARecurringTestEvent();
            break;
        }
    }
}

function trackComplete() {
    const state = store.getState();
    registerTestsWithOphan(state.data.abTests, completeTestFlags);
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

export function reducedCheckout(tests) {
    return (tests[0].testName == 'ReducedCheckoutTest') && (tests[0].variantName = 'test')
}

export function presetAmount(tests) {
    const data = testDataFor(tests, 'AmountHighlightTest');
    const defaultAmount = countryId() === 'au' ? 100 : 25;

    return (data && data.preselect) || defaultAmount;
}
