require([
    'src/modules/analytics/setup',
    'src/modules/images',
    'src/modules/metrics',
    'src/modules/contribute',
    'src/modules/stripe',
    'src/modules/dropdown',
    'src/modules/identityPopup',
    'src/modules/identityPopupDetails',
    'src/modules/userDetails',
    'src/modules/abTests'
], function (
    analytics,
    images,
    metrics,
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
    images.init();
    metrics.init();
    contribute.init();
    stripe.init();

    dropdown.init();
    identityPopup.init();
    identityPopupDetails.init();
    userDetails.init();

    abTests.init();
});
