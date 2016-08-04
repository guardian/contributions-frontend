import * as user  from 'src/utils/user'

const dimensions = {
    signedIn: 'dimension1',
};

export function init() {
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

    var isLoggedIn = user.isLoggedIn();

    ga('create', guardian.googleAnalytics.trackingId, {
        'allowLinker': true,
        'cookieDomain': guardian.googleAnalytics.cookieDomain
    });

    ga('require', 'linker');

    /**
     * Enable enhanced link attribution
     * https://support.google.com/analytics/answer/2558867?hl=en-GB
     */
    ga('require', 'linkid', 'linkid.js');

console.log(codes);
    ga('set', dimensions.signedIn, isLoggedIn.toString());

}

export function pageView(name){
    ga('set','page','/#' + name );
    ga('send', 'pageview');
}

export function event(name){
    ga('send','event','Contribution',name);
}
