require([
    'src/modules/analytics/setup',
    'src/modules/analytics/ga',
    'src/modules/contribute',
    'src/modules/stripe',
    'src/modules/stripeCheckout',
    'src/modules/dropdown',
    'src/modules/identityPopup',
    'src/modules/identityPopupDetails',
    'src/modules/userDetails',
    'src/modules/abTests'
], function (
    analytics,
    ga,
    contribute,
    stripe,
    stripeCheckout,
    dropdown,
    identityPopup,
    identityPopupDetails,
    userDetails,
    abTests
) {
    'use strict';

    analytics.init().then(function () {
        ga.pageView();
    });
    contribute.init();
    stripe.init();
    stripeCheckout.init();

    dropdown.init();
    identityPopup.init();
    identityPopupDetails.init();
    userDetails.init();

    abTests.init();
});
