require([
    'babel-polyfill',
    'ajax',
    'src/modules/raven',
    'src/modules/analytics/setup',
    'src/modules/images',
    'src/modules/toggle',
    'src/modules/dropdown',
    'src/modules/navigation',
    'src/modules/userDetails',
    'src/modules/form',
    'src/modules/form/processSubmit',
    'src/modules/identityPopup',
    'src/modules/identityPopupDetails',
    'src/modules/metrics',
    'src/modules/giraffe',
    'src/modules/abTests'
], function(
    b,
    ajax,
    raven,
    analytics,
    images,
    toggle,
    dropdown,
    navigation,
    userDetails,
    form,
    processSubmit,
    identityPopup,
    identityPopupDetails,
    metrics,
    giraffe,
    abTests
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

    // Forms
    form.init();
    processSubmit.init();

    // Metrics
    metrics.init();


    giraffe.init();
    abTests.init();
});
