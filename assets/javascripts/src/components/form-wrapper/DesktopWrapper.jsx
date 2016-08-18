import React from 'react';

import Title from '../Title.jsx';
import ProgressIndicator from '../ProgressIndicator.jsx';
import Navigation from '../Navigation.jsx';

import {ALL_PAGES} from 'src/constants';

export default class DesktopWrapper extends React.Component {
    render() {
        return <div>
            {ALL_PAGES.map(p =>
                <section className={'contribute-section ' + (this.props.page === p ? 'current' : '')} key={p}>
                    <div className="contribute-form__heading">
                        <Title page={p}/>
                        <ProgressIndicator page={this.props.page}/>
                    </div>
                    <form className={'flex-vertical contribute-form__inner'}
                          onSubmit={this.props.submit.bind(this)} key={p}>

                        {this.props.componentFor(p)}

                        <Navigation
                            page={p}
                            goBack={this.props.goBack}
                            amount={this.props.card.amount}
                            currency={this.props.currency}
                            processing={this.props.processing}
                            pay={this.props.pay}/>
                    </form>
                </section>
            )}
        </div>;
    }
}
