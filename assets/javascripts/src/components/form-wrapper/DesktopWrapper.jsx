import React from 'react';

import Title from '../Title.jsx';
import ProgressIndicator from '../ProgressIndicator.jsx';
import Navigation from '../Navigation.jsx';

import {ALL_PAGES} from 'src/constants';

export default class DesktopWrapper extends React.Component {
    render() {
        return <div>
            <section className='contribute-section' key={this.props.page}>
                <div className="contribute-form__heading">
                    <Title page={this.props.page}/>
                    <ProgressIndicator page={this.props.page}/>
                </div>
                <form className={'flex-vertical contribute-form__inner'}
                      onSubmit={this.props.submit.bind(this)} key={this.props.page}>

                    {this.props.componentFor(this.props.page)}

                    <Navigation
                        page={this.props.page}
                        goBack={this.props.goBack}
                        amount={this.props.card.amount}
                        currency={this.props.currency}
                        processing={this.props.processing}
                        pay={this.props.pay}/>
                </form>
            </section>
        </div>;
    }
}
