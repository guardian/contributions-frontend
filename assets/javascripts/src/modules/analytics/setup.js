/*global guardian:true */
define([
    'src/utils/cookie',
    'src/modules/analytics/ga',
    'src/modules/analytics/ophan.es6',
    'src/modules/analytics/krux'
], function (
    cookie,
    ga,
    ophan,
    krux
) {
    'use strict';

    /*
     Re: https://bugzilla.mozilla.org/show_bug.cgi?id=1023920#c2

     The landscape at the moment is:

     On navigator [Firefox, Chrome, Opera]
     On window [IE, Safari]
     */
    var isDNT = navigator.doNotTrack == '1' || window.doNotTrack == '1';

    var analyticsEnabled = (
        guardian.analyticsEnabled &&
        !isDNT &&
        !cookie.getCookie('ANALYTICS_OFF_KEY')
    );

    function setupAnalytics() {
        ophan.init();
        ga.init();
    }

    function setupThirdParties() {
        krux.init();
    }

    function init() {

        if (analyticsEnabled) {
            setupAnalytics();
        }

        if(analyticsEnabled && !guardian.isDev) {
            setupThirdParties();
        }
    }

    return {
        init: init
    };
});
