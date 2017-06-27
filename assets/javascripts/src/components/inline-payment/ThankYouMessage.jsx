import { h } from 'preact';
/** @jsx h */

const ThankYouMessage = props => (
    <div class="thank-you">
        <h2 class="thank-you__heading">Thank you!</h2>
        <p>You’ve made a vital contribution that will help us maintain our indepedent, investigative journalism.</p>
        <p>If you have any questions about contributing to the Guardian, please <a href="mailto:contribution.support@theguardian.com">contact us</a>.</p>
    </div>
);

export default ThankYouMessage;
