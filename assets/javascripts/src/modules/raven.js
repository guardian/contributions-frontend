define(['src/utils/user','raven-js'], function (user, Raven) {
    'use strict';


        var dsn = 'https://4be2858531754dae8a011addc0b69198@app.getsentry.com/90312';
        var tags = { build_number: guardian.membership.buildNumber };
        var cookieUser = user.getUserFromCookie();

        if (cookieUser) {
            tags.userIdentityId = cookieUser.id;
        }

        /**
         * Set up Raven, which speaks to Sentry to track errors
         */
        Raven.config(dsn, {
            whitelistUrls: [ /contribute\.theguardian\.com/, /contribute\.thegulocal\.com/, /localhost/ ],
            tags: tags,
            release: tags.build_number,
            ignoreErrors: [ /duplicate define: jquery/ ],
            ignoreUrls: [ /platform\.twitter\.com/ ],
            shouldSendCallback: function(data) {
                if(window.guardian.isDev) {
                    console.log('Raven', data);
                }
                return !window.guardian.isDev;
            }
        }).install();


    return{
        Raven: Raven
    };
});
