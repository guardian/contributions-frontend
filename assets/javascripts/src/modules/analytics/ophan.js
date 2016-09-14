define(['src/modules/raven'],function(raven) {
    'use strict';

    var ophanUrl = '//j.ophan.co.uk/contribution.js';
    var ophan = curl(ophanUrl);

    function init() {
        ophan.then(null, function(err) {
            raven.Raven.captureException(err);
        });
    }

    return {
        init: init,
        loaded: ophan
    };
});
