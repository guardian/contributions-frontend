import { h, Component } from 'preact';
/** @jsx h */

const AmountInput = props => (
    <div class="input-text contributions-inline-epic__input--amount">
        <span class="symbol">{props.symbol}</span>

        <input
            value={props.amount}
            type="number"
            class="input-text input-text--inner"
            placeholder="Other amount"
            onInput={e => props.setAmount(e.target.value)} />
    </div>
);

export default AmountInput;
