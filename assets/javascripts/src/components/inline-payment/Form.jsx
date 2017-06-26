import { h, Component } from 'preact';

import AmountButton from './AmountButton';
import AmountInput from './AmountInput';
import ContributeButton from './ContributeButton';
import ErrorMessage from './ErrorMessage';

/** @jsx h */

const Button = 1;
const Input = 2;


const formDataByRegion = {
    'GB': {
        amounts: [25, 50, 100, 250],
        symbol: '£',
        countryGroup: 'uk'
    },

    'EU': {
        amounts: [25, 50, 100, 250],
        symbol: '€',
        countryGroup: 'eu',
    },

    'US': {
        amounts: [25, 50, 100, 250],
        symbol: '$',
        countryGroup: 'us',
    },

    'AU': {
        amounts: [50, 100, 250, 500],
        symbol: '$',
        countryGroup: 'au'
    }
};


export default class Form extends Component {
    constructor(props: {
        pageContext: PageContext,
        onPaymentComplete: () => void,
        onPaymentFailed: () => void,
        onPaymentSubmitted: () => void
    }) {
        super(props);

        this.state = {
            selectedAmount: {
                component: undefined,
                value: undefined,
            }
        };

        this.formData = formDataByRegion[props.pageContext.region] || formDataByRegion.GB;
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
        this.props.onPaymentSubmitted();

        const authRequestData = {
            countryGroup: this.formData.countryGroup,
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

    executePaypalPayment(url) {
        return fetch(url, {
            headers: { 'Accept': 'application/json' },
        }).then(response => {
            if (response.ok) this.props.onPaymentComplete()
            else this.props.onPaymentFailed()
        });
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
                    sendPaypalRequest={this.sendPaypalRequest.bind(this)}
                    executePaypalPayment={this.executePaypalPayment.bind(this)} />

                { props.showErrorMessage && <ErrorMessage/> }
            </form>
        );
    }
}
