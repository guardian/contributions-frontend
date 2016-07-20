require([
    'babel-polyfill',
    'ajax',
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
    'src/modules/giraffe',
    'src/modules/abTests'
], function(
    b,
    ajax,
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
    giraffe,
    abTests
) {
    'use strict';

    console.log('Giraffe js initialising.');

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


    giraffe.init();
    abTests.init();
});
