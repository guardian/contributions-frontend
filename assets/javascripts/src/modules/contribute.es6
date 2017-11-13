import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

import Main from 'src/components/Main';
import Ticker from 'src/components/Ticker';
import store from 'src/store';

import { SET_DATA, SET_COUNTRY_GROUP, SET_AMOUNT, GO_FORWARD, AUTOFILL} from 'src/actions';
import { attachCurrencyListeners, attachErrorDialogListener } from 'src/modules/domListeners';

import * as ophan from 'src/modules/analytics/ophan';

export function init() {
    const container = document.getElementById('contribute');
    const presetAmount = getUrlParameter('amount');

    store.dispatch({ type: SET_DATA, data: appDataFrom(container) });

    if (presetAmount) {
        store.dispatch({ type: SET_AMOUNT, amount: parseInt(presetAmount) });
    }

    ReactDOM.render(
        React.createElement(Provider, { store: store },
            React.createElement(Main)
        ),
        container
    );

    const tickerContainer = document.getElementById('ticker');
    if (tickerContainer) {
        fetch(tickerContainer.getAttribute('data-src'), {
           method: 'get'
        }).then(resp => {
            return resp.json();
        }).then(json => {
            const stats = json.sheets.Sheet1[0];
            ReactDOM.render(
                React.createElement(Ticker, {
                    total: stats.total,
                    target: stats.goal,
                }),
                tickerContainer
            );
        });

    }

    attachCurrencyListeners();
    attachErrorDialogListener();
    setOphanIds();
    autoFill();
}

function autoFill() {
    fetch('/user/autofill', {
        method: 'get',
        credentials: 'include'
    }).then(resp => {
        return resp.json();
    }).then(json => {
        if (json.email || json.name) {
            store.dispatch({
                type: AUTOFILL,
                details: {
                    name: json.name,
                    email: json.email
                }
            });
        }
    });
}

/**
 * Convert data attributes on the container element to an object
 *
 * @param container
 * @returns {{abTests, maxAmount: number, countryGroup, currency}}
 */
function appDataFrom(container) {
    const { currency, ...countryGroup } = JSON.parse(container.getAttribute('data-country-group'));

    return {
        abTests: JSON.parse(container.getAttribute('data-ab-tests')),
        maxAmount: Number(JSON.parse(container.getAttribute('data-max-amount'))),
        countryGroup: countryGroup,
        currency: currency,
        cmpCode: container.getAttribute('data-cmp-code'),
        intCmpCode: container.getAttribute('data-int-cmp-code'),
        refererPageviewId: container.getAttribute('data-referrer-pageview-id'),
        refererUrl: container.getAttribute('data-referrer-url'),
        csrfToken: container.getAttribute('data-csrf-token'),
        disableStripe: container.getAttribute('data-disable-stripe') === 'true',
        componentId: container.getAttribute('data-component-id'),
        componentType: container.getAttribute('data-component-type'),
        source: container.getAttribute('data-source'),
        referrerAbTest: JSON.parse(container.getAttribute('data-referrer-ab-test'))
    };
}

function getUrlParameter(rawName, url) {
    const name = rawName.replace(/[\[\]]/g, "\\$&");
    const regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)");
    const results = regex.exec(url || window.location.href);

    if (!results) return null;
    if (!results[2]) return '';

    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

function setOphanIds() {
    store.dispatch({
        type: SET_DATA,
        data: { ophan: {
            pageviewId: ophan.viewId,
            browserId: ophan.browserId,
            visitId: ophan.visitId
        }}
    });
}
