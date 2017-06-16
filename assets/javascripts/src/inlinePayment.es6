// TODO:
import 'whatwg-fetch';

const formData = {
    // We don't collect the reader's name in-Epic currently.
    name: 'unknown',
    currency: 'GBP',
    amount: '20',
    email: null,
    token: null,
    // Since we don't ask for marketing opt-in, assume the reader doesn't want to be contacted.
    marketing: false,
    ophanPageviewId: null,
    ophanBrowserId: null,
    refererPageviewId: null,
    refererUrl: null,
    intcmp: null,
};

function updateFormDataWithToken(token) {
    formData.token = token.id;
    formData.email = token.email
}

const processStripeContribution = (token) => {

    updateFormDataWithToken(token);

    fetch('https://contribute.theguardian.com/stripe/pay', {
        credentials: 'same-origin',
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData)
    })
};

const handler = StripeCheckout.configure({
    key: 'pk_test_35RZz9AAyqErQshL410RDZMs',
    name: 'The Guardian',
    description: 'Make a contribution',
    allowRememberMe: false,
    token: processStripeContribution,
});

console.log(document.getElementById("contribution-button"));


document.getElementById("contribution-button").addEventListener('click', function(e) {

    // Amount configured here, since it is not known when the Stripe checkout is configured.
    handler.open({
        amount: formData.amount,
        currency: formData.currency
    });

    e.preventDefault();
});

// Close Checkout on page navigation:
window.addEventListener('popstate', function() {
    handler.close();
});
