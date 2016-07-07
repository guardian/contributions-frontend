define(
    [
        '$'
    ],
    function($) {
        'use strict';

        var $SUBMIT_BUTTON = $('.js-submit-input-cta');

        function render() {
            if ($SUBMIT_BUTTON.length > 0) {
              //is this ever used? I sure hope not
                console.warn('submitButton.js actually does need lodash');
                /// /  $SUBMIT_BUTTON.html(template(submitButtonTemplate)(guardian.membership.checkoutForm));
            }
        }

        return {
            init: render,
            render: render
        };
    }
);
