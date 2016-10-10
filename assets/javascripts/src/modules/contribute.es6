import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

import Main from 'src/components/Main.jsx';
import store from 'src/store';

import { SET_DATA, SET_COUNTRY_GROUP, SET_AMOUNT, GO_FORWARD } from 'src/actions';
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

    attachCurrencyListeners();
    attachErrorDialogListener();
    setOphanId();
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
        csrfToken: container.getAttribute('data-csrf-token')
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

function setOphanId() {
    ophan.loaded.then(o => store.dispatch({
        type: SET_DATA,
        data: { ophanId: o.viewId }
    }));
}
