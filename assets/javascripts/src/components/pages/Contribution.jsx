import React from 'react';

export default class Contribution extends React.Component {
    render() {
        const amounts = [25, 50, 100, 250];

        return <div className="contribute-controls">
            {amounts.map(amount => <button type="button" tabindex="8" className="contribute-controls__button option-button" data-amount={amount}>£{amount}</button>)}
            <input type="number" className="contribute-controls__input input-text" placeholder="Other amount" maxlength="10" tabindex="12" />
        </div>;
    }
}
