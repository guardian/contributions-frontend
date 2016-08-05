import React from 'react';

import { PAGES } from 'src/constants';

export default class Title extends React.Component {
    render() {
        const titlesByPageId = {
            [PAGES.CONTRIBUTION]: 'Your contribution',
            [PAGES.DETAILS]: 'Your details',
            [PAGES.PAYMENT]: 'Your payment'
        };

        return <div className="contribute-form__title-outer">
            <h3 className="contribute-form__title">
                {titlesByPageId[this.props.page]}
            </h3>

            {this.props.page === PAGES.PAYMENT &&
                <div className="security-note">
                    <svg className="icon-inline icon-inline--small icon-inline--top">
                        <use xmlnsXlink="http://www.w3.org/1999/xlink" xlinkHref="#icon-secure"></use>
                    </svg>
                    This site is secure
                </div>
            }

        </div>;
    }
}
