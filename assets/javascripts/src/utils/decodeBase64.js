/*global escape*/
define(['src/utils/atob'], function (AtoB) {
    'use strict';

    return function(str) {
        /**
         * Wrap in try/catch because AtoB will return a fatal error if we try to decode a non-base64 value
         * Don't try to catch this with Raven, or you'll get a circular dependency
         * No-op in catch because of eslint :(
         * Global escape: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/escape
         */
        var decoded;
        try {
            decoded = decodeURIComponent(escape(new AtoB()(str.replace(/-/g, '+').replace(/_/g, '/').replace(/,/g, '='))));
        } catch(e){
            decoded = '';
        }
        return decoded;

    };

});
