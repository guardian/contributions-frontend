import { h, Component} from 'preact';
/** @jsx h */

export default class AmountButton extends Component {
    componentDidMount() {
        if (this.props.focusOnMount) this.base.focus();
    }

    render(props) {
        return <button type="button"
                       class={`contributions-inline-epic__button--amount ${props.dim && 'fade-out'}`}
                       onFocus={props.setAmount}>
            {props.symbol}{props.amount}
        </button>;
    }
}
