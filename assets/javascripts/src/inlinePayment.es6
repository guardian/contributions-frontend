// @flow

import 'whatwg-fetch';
import { h, render } from 'preact';

import InlinePaymentForm from 'src/components/inline-payment/Form';

/** @jsx h */

const defaultContext = {
    region: 'GB',
    countryGroup: 'uk',
    intCmp: 'PAYPAL_TEST',
    refererPageviewId: null,
    refererUrl: null,
    ophanBrowserId: null
};

const renderForm = (pageContext: PageContext) => render(
    <InlinePaymentForm pageContext={pageContext} />,
    document.getElementById('inline-form')
);

// if we haven't received region data from the parent window within 1 second, render the GB data
const timeoutToRenderDefault = setTimeout(() => renderForm(defaultContext), 1000);

window.addEventListener('message', (data: { type: string, pageContext: PageContext }) => {
    if (data.type === 'PAGE_CONTEXT') {
        renderForm(data.pageContext);
        clearTimeout(timeoutToRenderDefault);
    }
});
