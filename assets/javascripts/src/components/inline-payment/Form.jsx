import { h, Component } from 'preact';

import AmountButton from './AmountButton';
import AmountInput from './AmountInput';
import ContributeButton from './ContributeButton';

/** @jsx h */

const Button = 1;
const Input = 2;

export default class Form extends Component {
    constructor() {
        super();

        this.state = {
            selectedAmount: {
                component: undefined,
                value: undefined,
            },

            paypal: {
                data: undefined,
                actions: undefined,
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

    render(props, state) {
        return (
            <form>
                <div class="contributions-inline-epic__button-wrapper">
                    {props.amounts.map(amount => <AmountButton amount={amount} symbol={props.symbol} setAmount={this.setAmountFrom(Button).bind(this, amount)} />)}
                </div>

                <div class="contributions-inline-epic__input-wrapper">
                    <AmountInput
                        setAmount={this.setAmountFrom(Input).bind(this)}
                        amount={this.getInputAmountValue()} />
                </div>

                <ContributeButton test="123" />
            </form>
        );
    }
}
