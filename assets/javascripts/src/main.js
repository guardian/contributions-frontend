require([
    'src/modules/analytics/setup',
    'src/modules/contribute',
    'src/modules/stripe',
    'src/modules/dropdown',
    'src/modules/identityPopup',
    'src/modules/identityPopupDetails',
    'src/modules/userDetails',
    'src/modules/abTests'
], function (
    analytics,
    contribute,
    stripe,
    dropdown,
    identityPopup,
    identityPopupDetails,
    userDetails,
    abTests
) {
    'use strict';

    analytics.init();
    contribute.init();
    stripe.init();

    dropdown.init();
    identityPopup.init();
    identityPopupDetails.init();
    userDetails.init();

    abTests.init();
});
