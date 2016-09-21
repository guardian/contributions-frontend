import React from 'react';

import Title from '../Title.jsx';
import ProgressIndicator from '../ProgressIndicator.jsx';
import Navigation from '../Navigation.jsx';
import LegalNotice from '../LegalNotice';
import {PAGES} from 'src/constants';


export default class DesktopWrapper extends React.Component {
    render() {
        const makeThingsOpaque = this.props.showRecurring && this.props.recurring === null;

        return <div>
           <section className={'contribute-section ' + (makeThingsOpaque ? 'opaque' : '')} key={this.props.page}>
                <div className="contribute-form__heading">
                    <Title page={this.props.page}/>
                    <ProgressIndicator page={this.props.page}/>
                </div>
                <form className={'flex-vertical contribute-form__inner'}
                      onSubmit={this.props.submit.bind(this)} key={this.props.page}>

                    {this.props.componentFor(this.props.page, false)}

                    <div className="opacity-wrapper contribute-navigation">
                        <Navigation
                            page={this.props.page}
                            goBack={this.props.goBack}
                            amount={this.props.card.amount}
                            currency={this.props.currency}
                            processing={this.props.processing}
                            pay={this.props.pay}
                            payWithPaypal={this.props.payWithPaypal}
                            payWithCard={this.props.payWithCard}
                            mobile={false}
                            clearPaymentFlags={this.props.clearPaymentFlags}/>
                    </div>
                    {!this.props.processing && this.props.page==PAGES.CONTRIBUTION && <LegalNotice countryGroup={this.props.countryGroup}/>}
                </form>
            </section>
        </div>;
    }
}
