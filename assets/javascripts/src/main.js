require([
    'src/modules/raven',
    'src/modules/analytics/setup',
    'src/modules/images',
    'src/modules/metrics',
    'src/modules/abTests',
    'src/modules/contribute'
], function(
    raven,
    analytics,
    images,
    metrics,
    abTests,
    contribute
) {
    'use strict';

    analytics.init();
    metrics.init();
    contribute.init();
    abTests.init();
});
