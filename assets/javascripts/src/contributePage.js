require([
    'src/modules/analytics/setup',
    'src/modules/analytics/ga',
    'src/modules/contribute',
    'src/modules/stripe',
    'src/modules/dropdown',
    'src/modules/identityPopup',
    'src/modules/identityPopupDetails',
    'src/modules/userDetails',
    'src/modules/abTests',
    'src/modules/ticker'
], function (
    analytics,
    ga,
    contribute,
    stripe,
    dropdown,
    identityPopup,
    identityPopupDetails,
    userDetails,
    abTests,
    ticker
) {
    'use strict';

    analytics.init().then(function () {
        ga.pageView();
    });
    contribute.init();
    stripe.init();

    dropdown.init();
    identityPopup.init();
    identityPopupDetails.init();
    userDetails.init();
    ticker.init();

    abTests.init();
});
