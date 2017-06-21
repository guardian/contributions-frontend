import 'whatwg-fetch';
import { h, render } from 'preact';

import InlinePaymentForm from 'src/components/inline-payment/Form';

/** @jsx h */

// TODO: await setup messasge from parent window (theguardian.com), containing amounts and currency data

render(
    <InlinePaymentForm amounts={[1,5,10,25]} symbol="£" />,
    document.getElementById('inline-form')
);
