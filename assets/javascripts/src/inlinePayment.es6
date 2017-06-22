// @flow

import 'whatwg-fetch';
import { h, render } from 'preact';

import InlinePaymentForm from 'src/components/inline-payment/Form';

/** @jsx h */

const defaultContext = {
    region: 'GB',
    intCmp: 'PAYPAL_TEST',
    refererPageviewId: null,
    refererUrl: null,
    ophanBrowserId: null
};

const renderForm = (pageContext: PageContext) => render(
    <InlinePaymentForm pageContext={pageContext} />,
    document.getElementById('inline-form')
);

const initialise = () => {
    // if we haven't received region data from the parent window within 2 seconds, render the GB data
    const timeoutToRenderDefault = setTimeout(() => renderForm(defaultContext), 2000);

    parent.postMessage({ type: 'CONTEXT_REQUEST' }, '*');

    window.addEventListener('message', (event: { data: { type: string, pageContext: PageContext } }) => {
        if (event.data.type === 'PAGE_CONTEXT' && event.data.pageContext) {
            renderForm(event.data.pageContext);
            clearTimeout(timeoutToRenderDefault);
        }
    });
}

initialise();

