require([
    'ajax',
    'src/modules/raven',
    'src/modules/analytics/setup',
    'src/modules/images',
    'src/modules/metrics',
    'src/modules/abTests',
    'src/modules/contribute',
    'src/modules/stripe'
], function(
    ajax,
    raven,
    analytics,
    images,
    metrics,
    abTests,
    contribute,
    stripe
) {
    'use strict';

    ajax.init({ page: { ajaxUrl: '' } });
    analytics.init();
    metrics.init();
    contribute.init();
    abTests.init();
    stripe.init();
});
