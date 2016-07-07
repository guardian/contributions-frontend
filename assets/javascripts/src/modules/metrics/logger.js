/* global ga */
define([], function () {
    'use strict';

    return function(options) {
        var metricData;
        if (window.ga) {
            metricData = Object.assign({
                'hitType': 'event'
            }, options);
            ga('send', metricData);
        }
    };
});
