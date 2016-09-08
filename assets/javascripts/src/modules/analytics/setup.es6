import cookie from 'src/utils/cookie';
import * as ga from 'src/modules/analytics/ga';
import * as ophan from 'src/modules/analytics/ophan';
import krux from 'src/modules/analytics/krux';
import Promise from 'promise-polyfill';

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
    return ophan.init().then(ga.init,ga.init);
}

function setupThirdParties() {
    return krux.init();
}

export function init() {

    let promises = [];

    if (analyticsEnabled) {
        promises.push(setupAnalytics());
    }

    if(analyticsEnabled && !guardian.isDev) {
        promises.push(setupThirdParties());
    }

    return Promise.all(promises);
}
