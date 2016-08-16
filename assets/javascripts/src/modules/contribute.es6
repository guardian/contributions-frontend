import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

import Main from 'src/components/Main.jsx';
import store from 'src/store';
import { SET_AB_TESTS } from 'src/actions';

export function init() {
    const container = document.getElementById('contribute');
    const abTests = container.dataset.abTests;

    ReactDOM.render(
        React.createElement(Provider, { store: store },
            React.createElement(Main)
        ),
        container
    );

    store.dispatch({ type: SET_AB_TESTS, tests: JSON.parse(abTests) });
}
