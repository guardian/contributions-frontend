import React from 'react';
import ReactDOM from 'react-dom';

import Main from 'src/components/Main.jsx';

export function init() {
    ReactDOM.render(
        React.createElement(Main),
        document.getElementById('contribute')
    );
}
