import { h, Component } from 'preact';

import AmountButton from './AmountButton';
import AmountInput from './AmountInput';
import ContributeButton from './ContributeButton';
import ErrorMessage from './ErrorMessage';
import TermsAndConditions from './TermsAndConditions';

/** @jsx h */

const Button = 1;
const Input = 2;

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

        this.formData = props.pageContext.formData
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
        if (!this.state.selectedAmount.value) return;

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
                <div>Give to the Guardian in less than a minute with a one-off contribution.</div>

                <div class="contribute-controls">
                    <div class="contributions-inline-epic__button-wrapper">
                        {this.formData.amounts.map((amount, i) => {
                            return <AmountButton
                                amount={amount}
                                focusOnMount={i === 1}
                                symbol={this.formData.symbol}
                                setAmount={this.setAmountFrom(Button).bind(this, amount)}
                                dim={!!this.getInputAmountValue()}/>
                        })}
                    </div>

                    <div class="contributions-inline-epic__input-wrapper">
                        <AmountInput
                            setAmount={this.setAmountFrom(Input).bind(this)}
                            amount={this.getInputAmountValue()}
                            symbol={this.formData.symbol} />
                    </div>

                    <ContributeButton
                        amount={state.selectedAmount.value}
                        sendPaypalRequest={this.sendPaypalRequest.bind(this)}
                        executePaypalPayment={this.executePaypalPayment.bind(this)}>

                    </ContributeButton>
                </div>

                { props.showErrorMessage && <ErrorMessage/> }

                <TermsAndConditions campaignCode={props.pageContext.intCmp}/>
            </form>
        );
    }
}
