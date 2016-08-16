import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

import Main from 'src/components/Main.jsx';
import store from 'src/store';
import { SET_DATA } from 'src/actions';

export function init() {
    const container = document.getElementById('contribute');

    ReactDOM.render(
        React.createElement(Provider, { store: store },
            React.createElement(Main)
        ),
        container
    );

    store.dispatch({ type: SET_DATA, data: appDataFrom(container) });
}

/**
 * Convert data attributes on the container element to an object
 *
 * @param container
 * @returns {{abTests, maxAmount: number, countryGroup, currency}}
 */
function appDataFrom(container) {
    const { currency, ...countryGroup } = JSON.parse(container.dataset.countryGroup)

    return {
        abTests: JSON.parse(container.dataset.abTests),
        maxAmount: Number(JSON.parse(container.dataset.maxAmount)),
        countryGroup: countryGroup,
        currency: currency
    };
}
