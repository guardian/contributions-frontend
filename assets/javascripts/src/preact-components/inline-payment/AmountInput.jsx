import { h, Component } from 'preact';
/** @jsx h */

const AmountInput = props => (
    <input
        value={props.amount}
        type="number"
        placeholder="Other amount"
        class="input-text contributions-inline-epic__input--amount"
        onInput={e => props.setAmount(e.target.value)}/>
);

export default AmountInput;
