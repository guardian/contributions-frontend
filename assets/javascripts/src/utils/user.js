define([
    'src/utils/atob',
    'ajax',
    'src/utils/cookie'
], function(AtoB, ajax, cookie){
    'use strict';

    var isLoggedIn = function(){
        return !!getUserFromCookie();
    };

    var idCookieAdapter = function (data, rawCookieString) {
        return {
            id: data[0],
            displayname: decodeURIComponent(data[2]),
            accountCreatedDate: data[6],
            emailVerified: data[7],
            rawResponse: rawCookieString
        };
    };

    var getUserFromCookie = function(){
        var cookieData = cookie.getCookie('GU_U');
        var userData = cookie.decodeCookie(cookieData);
        var userFromCookieCache;
        if (userData) {
            userFromCookieCache = idCookieAdapter(userData, cookieData);
        }
        return userFromCookieCache;
    };


    return {
        isLoggedIn: isLoggedIn,
        getUserFromCookie: getUserFromCookie,
        idCookieAdapter: idCookieAdapter
    };
});
