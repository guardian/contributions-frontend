// @flow

import 'whatwg-fetch';
import { h, render } from 'preact';

import InlinePaymentContainer from 'src/components/inline-payment/InlinePaymentContainer';

/** @jsx h */

const defaultContext = {
    region: 'GB',
    intCmp: 'pay_in_epic_no_page_context',
    refererPageviewId: null,
    refererUrl: null,
    ophanBrowserId: null,
    formData:  {
        amounts: [25, 50, 100, 250],
        symbol: '£',
        countryGroup: 'uk'
    }
};

const renderForm = (pageContext: PageContext) => render(
    <InlinePaymentContainer pageContext={pageContext} />,
    document.getElementById('inline-form')
);

const initialise = () => {
    // if we haven't received region data from the parent window within 2 seconds, render the GB data
    const timeoutToRenderDefault = setTimeout(() => renderForm(defaultContext), 2000);

    parent.postMessage({ type: 'PAGE_CONTEXT_REQUEST' }, '*');

    window.addEventListener('message', (event: { data: { type: string, pageContext: PageContext } }) => {
        if (event.data.type === 'PAGE_CONTEXT' && event.data.pageContext) {
            renderForm(event.data.pageContext);
            clearTimeout(timeoutToRenderDefault);
        }
    });
}

initialise();

