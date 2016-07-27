import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

import Main from 'src/components/Main.jsx';

import store from 'src/store';

export function init() {
    ReactDOM.render(
        React.createElement(Provider, { store: store },
            React.createElement(Main)
        ),
        document.getElementById('contribute')
    );
}
