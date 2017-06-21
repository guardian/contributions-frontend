import { h, Component } from 'preact';

import AmountButton from './AmountButton';
import AmountInput from './AmountInput';
import ContributeButton from './ContributeButton';

/** @jsx h */

const Button = 1;
const Input = 2;

const formDataByRegion = {
    'GB': {
        amounts: [25, 50, 100, 250],
        symbol: '£'
    },

    'EU': {
        amounts: [25, 50, 100, 250],
        symbol: '€'
    },

    'US': {
        amounts: [25, 50, 100, 250],
        symbol: '$'
    },

    'AU': {
        amounts: [50, 100, 250, 500],
        symbol: '$'
    }
}

export default class Form extends Component {
    constructor(props: { pageContext: PageContext }) {
        super(props);

        this.formData = formDataByRegion[props.pageContext.region] || formDataByRegion.GB;

        this.state = {
            selectedAmount: {
                component: undefined,
                value: undefined,
            }
        }
    }

    setAmountFrom(component) {
        return amount => {
            this.setState({
                selectedAmount: {
                    component: component,
                    value: amount
                }
            });
        }
    }

    getInputAmountValue() {
        const amount = this.state.selectedAmount;
        return amount.component === Input ? amount.value : null
    }

    sendPaypalRequest() {
        const authRequestData = {
            countryGroup: this.props.pageContext.countryGroup,
            amount: this.state.selectedAmount.value,
            intCmp: this.props.pageContext.intCmp,
            refererPageviewId: this.props.pageContext.refererPageviewId,
            refererUrl: this.props.pageContext.refererUrl,
            ophanPageviewId: this.props.pageContext.ophanPageviewId,
            ophanBrowserId: this.props.pageContext.ophanBrowserId,
        };

        return fetch('/paypal/auth', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(authRequestData)
        })
    }

    render(props, state) {
        return (
            <form>
                <div class="contributions-inline-epic__button-wrapper">
                    {this.formData.amounts.map(amount =>
                        <AmountButton
                            amount={amount}
                            symbol={this.formData.symbol}
                            setAmount={this.setAmountFrom(Button).bind(this, amount)} />
                    )}
                </div>

                <div class="contributions-inline-epic__input-wrapper">
                    <AmountInput
                        setAmount={this.setAmountFrom(Input).bind(this)}
                        amount={this.getInputAmountValue()} />
                </div>

                <ContributeButton
                    amount={state.selectedAmount.value}
                    sendPaypalRequest={this.sendPaypalRequest.bind(this)} />
            </form>
        );
    }
}
