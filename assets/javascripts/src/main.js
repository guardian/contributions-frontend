require([
    'src/modules/analytics/setup',
    'src/modules/images',
    'src/modules/metrics',
    'src/modules/contribute',
    'src/modules/stripe'
], function (
    analytics,
    images,
    metrics,
    contribute,
    stripe
) {
    'use strict';

    analytics.init();
    metrics.init();
    contribute.init();
    stripe.init();
});
