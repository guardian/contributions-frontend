import * as ophan from 'src/modules/analytics/ophan';

import store from 'src/store';

import { SET_AMOUNT } from 'src/actions';

export function init() {
    const state = store.getState();

    if ("abTests" in window) {
        var data = {};

        for (var test of abTests) {
            data[test.testSlug] = {
                'variantName': test.variantSlug,
                'complete': 'true'
            }
        }

        ophan.loaded.then(function (ophan) {
            ophan.record({
                abTestRegister: data
            })
        });
    }

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

export function presetAmount(tests) {
    const data = testDataFor(tests, 'AmountHighlightTest');

    return data && data.preselect;
}
