import 'whatwg-fetch';
import { h, render } from 'preact';

import InlinePaymentForm from 'src/components/inline-payment/Form';

/** @jsx h */

// TODO: await setup messasge from parent window (theguardian.com), containing amounts and currency data

const formDataByRegion = {
    'GB': {
        amounts: [25, 50, 100, 250],
        symbol: '£'
    },

    'EU': {
        amounts: [25, 50, 100, 250],
        symbol: '€'
    },

    'US': {
        amounts: [25, 50, 100, 250],
        symbol: '$'
    },

    'AU': {
        amounts: [50, 100, 250, 500],
        symbol: '$'
    }
};

const renderForm = formData => render(
    <InlinePaymentForm amounts={formData.amounts} symbol={formData.symbol}/>,
    document.getElementById('inline-form')
);

// if we haven't received region data from the parent window within 1 second, render the GB data
const timeoutToRenderDefault = setTimeout(renderForm.bind(null, formDataByRegion.GB), 1000);

window.addEventListener('message', data => {
    if (data.type === 'SET_REGION' && data.region) {
        const formData = formDataByRegion[data];

        if (formData) {
            renderForm(formData);
            clearTimeout(timeoutToRenderDefault);
        }
    }
});
