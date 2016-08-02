import React from 'react';

import { PAGES } from 'src/constants';

export default class Title extends React.Component {
    render() {
        const titlesByPageId = {
            [PAGES.CONTRIBUTION]: 'Your contribution',
            [PAGES.DETAILS]: 'Your details',
            [PAGES.PAYMENT]: 'Your payment'
        };

        return <h3 className="contribute-form__title">
            {titlesByPageId[this.props.page]}
        </h3>;
    }
}
