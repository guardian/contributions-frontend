import React from 'react';

export default class Title extends React.Component {
    render() {
        const titlesByPageId = {
            1: 'Your contribution',
            2: 'Your details',
            3: 'Your payment'
        } ;

        return <h3 className="contribute-form__title">
            {titlesByPageId[this.props.page]}
        </h3>;
    }
}
