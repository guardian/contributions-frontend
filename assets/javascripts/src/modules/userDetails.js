define([
    '$',
    'src/utils/user'
], function ($, userUtil) {
    'use strict';

    var AVAILABLE_DETAILS = ['displayname', 'firstName'];
    var CLASS_NAMES = {
        signedOut: 'id--signed-out',
        signedIn: 'id--signed-in',
        hasTier: 'member--has-tier'
    };

    /**
     * Given a user object, checks which details we're interested in
     * and inserts into matching HTML elements.
     */
    function populateUserDetails(obj) {
        AVAILABLE_DETAILS.forEach(function(detail) {
            var detailSelector = '.js-user-' + detail;
            var detailText = obj[detail];
            if (detailText) {
                $(detailSelector).text(detailText).removeClass('u-h');
            }
        });
    }

    /**
     * Updates class on HTML element, adds member tier class as appropriate.
     * Queries cookie and user/me for User details.
     */
    function init() {
        var htmlElement = $('html');
        if (userUtil.isLoggedIn()) {
            htmlElement
                .removeClass(CLASS_NAMES.signedOut)
                .addClass(CLASS_NAMES.signedIn);

            populateUserDetails(userUtil.getUserFromCookie());
        }
    }

    return {
        init: init,
        _: {
            CLASS_NAMES: CLASS_NAMES
        }
    };
});
