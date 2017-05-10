import React from 'react';
import LegalNotice from '../LegalNotice';
import Title from '../Title.jsx';
import ProgressIndicator from '../ProgressIndicator.jsx';
import Navigation from '../Navigation.jsx';

import {ALL_PAGES, PAGES} from 'src/constants';

export default class MobileWrapper extends React.Component {

    render() {
        if (this.props.page == PAGES.CONTRIBUTION)
            return this.renderInForm([PAGES.CONTRIBUTION]);
        else
            return this.renderInForm([PAGES.DETAILS]);

    }

    renderInForm(pages) {
        return <form className={'flex-vertical contribute-form__inner'} onSubmit={this.props.submit.bind(this)}>
            {pages.map(p =>
                <section className="contribute-section" key={p}>
                    <div className="contribute-form__heading">
                        <Title page={p}/>
                        <ProgressIndicator page={this.props.page} />
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
                        payWithCard={this.props.payWithCard}
                        jumpToFirstPage={this.props.jumpToFirstPage}
                        mobile={true}
                        clearPaymentFlags={this.props.clearPaymentFlags}
                        stripeCheckout={this.props.stripeCheckout}
                    />

                    {!this.props.processing  && p== PAGES.CONTRIBUTION && <LegalNotice countryGroup={this.props.countryGroup}/>}
                </section>
            )}
        </form>;
    }
}



