import React from 'react';

import Title from '../Title.jsx';
import ProgressIndicator from '../ProgressIndicator.jsx';
import Navigation from '../Navigation.jsx';

import { ALL_PAGES } from 'src/constants';

export default class MobileWrapper extends React.Component {
    render() {
        return <form className={'flex-vertical contribute-form__inner'} onSubmit={this.props.submit.bind(this)}>
            {ALL_PAGES.map(p =>
                <section className={'contribute-section ' + (this.props.page === p ? 'current' : '')} key={p}>
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
                        payWithPaypal={this.props.payWithPaypal}/>
                </section>
            )}
        </form>;
    }
}
