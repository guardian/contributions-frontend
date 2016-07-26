require([
    'babel-polyfill',
    'ajax',
    'react-dom',
    'src/modules/raven',
    'src/modules/analytics/setup',
    'src/modules/images',
    'src/modules/toggle',
    'src/modules/dropdown',
    'src/modules/navigation',
    'src/modules/userDetails',
    'src/modules/events/cta',
    'src/modules/form',
    'src/modules/form/processSubmit',
    'src/modules/identityPopup',
    'src/modules/identityPopupDetails',
    'src/modules/metrics',
    'src/modules/abTests',
    'src/modules/contribute'
], function(
    b,
    ajax,
    ReactDOM,
    raven,
    analytics,
    images,
    toggle,
    dropdown,
    navigation,
    userDetails,
    cta,
    form,
    processSubmit,
    identityPopup,
    identityPopupDetails,
    metrics,
    abTests,
    contribute
) {
    'use strict';

    ajax.init({page: {ajaxUrl: ''}});

    analytics.init();

    // Global
    images.init();
    toggle.init();
    dropdown.init();
    identityPopup.init();
    identityPopupDetails.init();
    navigation.init();
    userDetails.init();

    // Events
    cta.init();

    // Forms
    form.init();
    processSubmit.init();

    // Metrics
    metrics.init();

    contribute.init();
    abTests.init();
});
