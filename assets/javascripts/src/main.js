require([
    'ajax',
    'src/modules/raven',
    'src/modules/analytics/setup',
    'src/modules/images',
    'src/modules/metrics',
    'src/modules/abTests',
    'src/modules/contribute'
], function(
    ajax,
    raven,
    analytics,
    images,
    metrics,
    abTests,
    contribute
) {
    'use strict';

    ajax.init({ page: { ajaxUrl: '' } });
    analytics.init();
    metrics.init();
    contribute.init();
    abTests.init();
});
