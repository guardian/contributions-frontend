import { h } from 'preact';
/** @jsx h */

const AmountButton = props => (
    <button type="button"
            class="contributions-inline-epic__button--amount"
            onClick={props.setAmount}>
        {props.symbol}{props.amount}
    </button>
);

export default AmountButton;
