import React from 'react';

import Title from '../Title.jsx';
import ProgressIndicator from '../ProgressIndicator.jsx';
import Navigation from '../Navigation.jsx';

import { ALL_PAGES, PAGES } from 'src/constants';

export default class MobileWrapper extends React.Component {

    render() {
       const DETAILS_PAGES = [PAGES.DETAILS, PAGES.PAYMENT];
        return <span>
            {this.renderInForm([PAGES.CONTRIBUTION])}
            {this.renderInForm(DETAILS_PAGES)}
        </span>
    }


    renderInForm(pages) {

        return <form className={'flex-vertical contribute-form__inner'} onSubmit={this.props.submit.bind(this)}>
            {pages.map(p =>
                <section className={'contribute-section ' + (this.shouldDisplay(p) ? 'current' : '')} key={p}>
                    <div className="contribute-form__heading">
                        <Title page={p}/>
                        <ProgressIndicator page={this.props.page}/>
                    </div>

                    {this.props.componentFor(p)}

                    <Navigation
                        page={p}
                        goBack={this.props.goBack}
                        amount={this.props.card.amount}
                        currency={this.props.currency}
                        processing={this.props.processing}
                        pay={this.props.pay}
                        payWithPaypal={this.props.payWithPaypal}
                        payWithCard={this.props.payWithCard}/>

                </section>
            )}
        </form>;
    }

    shouldDisplay(page) {
        if (PAGES.CONTRIBUTION == this.props.page) {
            return (page == PAGES.CONTRIBUTION)
        }
        else {
            return page != PAGES.CONTRIBUTION
        }
    }


}



