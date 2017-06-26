import { h, Component } from 'preact';
import Form from "./Form";
import ThankYouMessage from "./ThankYouMessage";

/** @jsx h */

export default class InlinePaymentContainer extends Component {

    constructor(props: { pageContext: PageContext }) {
        super(props);
        this.state = {
            showThankYouMessage: false,
            showErrorMessage: false
        }
    }

    showThankYouMessage() {
        this.setState({ showThankYouMessage: true })
    }

    showErrorMessage() {
        this.setState({ showErrorMessage: true })
    }

    clearErrorMessage() {
        this.setState({ showErrorMessage: false })
    }

    render(props, state) {
        if (state.showThankYouMessage) {
            return <ThankYouMessage/>
        } else {
            return <Form
                pageContext={props.pageContext}
                onPaymentComplete={this.showThankYouMessage.bind(this)}
                onPaymentFailed={this.showErrorMessage.bind(this)}
                onPaymentSubmitted={this.clearErrorMessage.bind(this)}
                showErrorMessage={state.showErrorMessage}
            />
        }
    }
}
