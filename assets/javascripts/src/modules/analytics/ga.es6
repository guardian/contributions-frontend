import * as user  from 'src/utils/user';
import * as cookie from 'src/utils/cookie';
import store from 'src/store';

const dimensions = {
    signedIn: 'dimension1',
    signedOut: 'dimension2',
    ophanPageViewId: 'dimension3',
    ophanBrowserId: 'dimension4',
    platform: 'dimension5',
    identityId: 'dimension6',
    isLoggedOn: 'dimension7',
    stripeId: 'dimension8',
    zouraId: 'dimension9',
    membershipNumber: 'dimension10',
    productPurchased: 'dimension11',
    intcmp: 'dimension12'
};

const defaultTracker = 'membershipPropertyTracker';

const gaTimeout = 1000;

function create(){
    /*eslint-disable */
    (function (i, s, o, g, r, a, m) {
        i['GoogleAnalyticsObject'] = r;
        i[r] = i[r] || function () {
                (i[r].q = i[r].q || []).push(arguments)
            }, i[r].l = 1 * new Date();
        a = s.createElement(o),
            m = s.getElementsByTagName(o)[0];
        a.async = 1;
        a.src = g;
        m.parentNode.insertBefore(a, m)
    })(window, document, 'script', '//www.google-analytics.com/analytics.js', 'ga');
    /*eslint-enable */
    window.ga('create', guardian.googleAnalytics.trackingId, {
        'allowLinker': true,
        'name': defaultTracker,
        'cookieDomain': guardian.googleAnalytics.cookieDomain
    });
    return gaProxy
}

export function gaProxy() {
    const state = store.getState();
    if (state.gaTracking.enabled && window.ga) {
        const allArgs = Array.from(arguments);
        allArgs[0] = defaultTracker + '.' + allArgs[0];
        window.ga.apply(window.ga, allArgs);
    }
}

export function init() {
    const state = store.getState();
    if (!state.gaTracking.enabled) {
        return gaProxy;
    }
    let guardian = window.guardian;
    let ga = create();

    ga('require', 'linker');

    /**
     * Enable enhanced link attribution
     * https://support.google.com/analytics/answer/2558867?hl=en-GB
     */
    ga('require', 'linkid', 'linkid.js');

    ga('require', 'ec');

    //Set the custom dimensions.
    let u = user.getUserFromCookie();
    let isLoggedIn = !!u;
    let signedOut = !!cookie.getCookie('GU_SO') && !isLoggedIn;
    ga('set', dimensions.signedIn, isLoggedIn.toString());
    ga('set', dimensions.isLoggedOn, isLoggedIn.toString());
    ga('set', dimensions.signedOut, signedOut.toString());
    ga('set', dimensions.platform, 'contributions');
    if (isLoggedIn) {
        ga('set', dimensions.identityId, u.id);
    }
    if (guardian.ophan) {
        ga('set', dimensions.ophanPageViewId, guardian.ophan.pageViewId);
    }
    if("productDetails" in guardian) {
        ga('set',dimensions.stripeId,guardian.productDetails.charge);
    }
    ga('set', dimensions.ophanBrowserId, cookie.getCookie('bwid'));

    let intcmp = new RegExp('INTCMP=([^&]*)').exec(location.search);
    if (intcmp && intcmp[1]){
        ga('set',dimensions.intcmp,intcmp[1]);
    }

    return ga;
}

/**
 * @returns {Promise} a Promise that will resolved when the call to ga comes back, or after a timeout define by gaTimeout
 * it will never be rejected
 */
export function pageView() {
    return new Promise(resolve => {
        const timeout = setTimeout(resolve, gaTimeout);
        gaProxy('send', {
            hitType: 'pageview',
            hitCallback: () => {
                clearTimeout(timeout);
                resolve();
            }
        });
    });
}

/**
 * @returns {Promise} a Promise that will resolved when the call to ga comes back, or after a timeout define by gaTimeout
 * it will never be rejected
 */
export function event(category, actionName, label, value) {
    return new Promise(resolve => {
        const timeout = setTimeout(resolve, gaTimeout);
        gaProxy('send', {
            hitType: 'event',
            eventCategory: category,
            eventAction: actionName,
            eventLabel: label,
            eventValue: value,
            hitCallback: () => {
                clearTimeout(timeout);
                resolve();
            }
        });
    });
}

export function setCheckoutStep(checkoutStep) {
    gaProxy('ec:setAction', 'checkout', {
        'step': checkoutStep
    });
}

export function trackCheckout(checkoutStep, actionName, label) {
    setCheckoutStep(checkoutStep);
    return event('Checkout', actionName, label);
}

export function trackPayment(price, currency) {
    gaProxy('set', '&cu', currency.toUpperCase());

    gaProxy('ec:addProduct', {
        'id': '1',
        'name': 'Contribution',
        'price': price
    });

    gaProxy('ec:setAction', 'purchase', {
        'id': Math.floor(Math.random() * 1000000), // ga wants a transaction ID, but I don't want to send it
        'revenue': price
    });

    return event('Payment', 'Contribute', 'purchase');
}

export function waitForGA() {
    return new Promise(resolve => {
        function wait() {
            if (window.ga === undefined) {
                setTimeout(wait, 100);
            } else {
                return resolve();
            }
        }
        wait();
    })
}
