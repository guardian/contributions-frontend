import cookie from 'src/utils/cookie';
import * as ga from 'src/modules/analytics/ga';
import * as ophan from 'src/modules/analytics/ophan';
import * as krux from 'src/modules/analytics/krux';
import store from 'src/store';
import { GA_ENABLED } from 'src/actions'

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

/**
 * @param enabled whether ophan is enabled or not
 * @returns {Promise} resolving when ready
 */
function setupOphan(enabled) {
    if (enabled) {
        return ophan.init();
    } else {
        return Promise.resolve();
    }
}


/**
 * @param enabled whether google analytics is enabled or not
 * @returns {Promise} resolving when ready
 */
function setupAnalytics(enabled) {
    store.dispatch({ type: GA_ENABLED, enabled: enabled });
    return Promise.resolve(ga.init());
}



/**
 * @param enabled whether krux is enabled or not
 * @returns {Promise} resolving when ready
 */
function setupKrux(enabled) {
    if (enabled) {
        return krux.init();
    } else {
        return Promise.resolve();
    }
}

export function init() {
    return setupOphan(analyticsEnabled)
        .then(() => setupAnalytics(analyticsEnabled))
        .then(() => setupKrux(analyticsEnabled && !guardian.isDev));
}
