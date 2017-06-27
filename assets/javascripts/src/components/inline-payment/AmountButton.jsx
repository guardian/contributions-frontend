import { h } from 'preact';
/** @jsx h */

const AmountButton = props => (
    <button type="button"
            class={`contributions-inline-epic__button--amount ${props.dim && 'fade-out'}`}
            onClick={props.setAmount}>
        {props.symbol}{props.amount}
    </button>
);

export default AmountButton;
