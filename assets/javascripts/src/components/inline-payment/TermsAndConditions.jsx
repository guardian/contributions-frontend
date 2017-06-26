import { h } from 'preact';
/** @jsx h */

const TermsAndConditions = props => (
    <div>
        By proceeding, you are agreeing to our <a href="https://www.theguardian.com/info/2016/apr/04/contribution-terms-and-conditions" target="_blank">Terms & Conditions</a> and <a href="https://www.theguardian.com/help/privacy-policy" target="_blank">Privacy Policy</a>.
        The ultimate owner of the Guardian is The Scott Trust Limited.
        Contributions are not eligible for Gift Aid in the UK nor a tax-deduction elsewhere.
        You can learn more about one-off contributions <a href={`https://contribute.theguardian.com?INTCMP=${props.campaignCode}`} target="_blank">here</a>.
    </div>
);

export default TermsAndConditions;
