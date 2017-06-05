/**
 * This file:
 * Populates the This Land is Your Land tracker
 * Triggers Animation and increases the count
 */
define([
    'src/utils/$'
], function ($) {
    'use strict';

    var pledged = 0,
        target = 50000,
        count = 0,
        interval;

    function init() {
        if (hasTicker()) {
            getData();
        }
    }

    function hasTicker() {
        return $('.ticker').length > 0 ? true : false; 
    }

    function loadJSON(path, success) {
        var xhr = new XMLHttpRequest();
        xhr.onreadystatechange = function() {
            if (xhr.readyState === XMLHttpRequest.DONE) {
                if (xhr.status === 200) {
                    if (success) {
                        success(JSON.parse(xhr.responseText));
                    }
                } else {
                    if (error) {
                        error(xhr);
                    }
                }
            }
        };
        xhr.open('GET', path, true);
        xhr.send();
    }

    function getData() {
        loadJSON('https://interactive.guim.co.uk/docsdata-test/1no5r1O5A0omDkz4HALce4SrFWDGzSuR_jdB2MYOsPt4.json', function(data) {
            pledged = data.sheets.Sheet1[0].total;
            animateTicker();
        });
    }

    function animateTicker() {
        setTimeout(function() {
            if (interval === undefined) {
                interval = setInterval(increaseCounter, 30);
            }

            animateProgressBar();
        }, 500);
    }

    function increaseCounter() {
        count += Math.floor(pledged / 100);
        $('.ticker__count').text('$' + count.toLocaleString());

        if (count >= pledged) {
            clearInterval(interval);
            $('.ticker__count').text('$' + count.toLocaleString());
        }
    }

    function animateProgressBar() {
        $('.ticker__filled-progress').attr('style', 'transform: translateX(' + getPercentageAsNegativeTranslate() + ')');
    }

    function getPercentageAsNegativeTranslate() {
        var percentage = pledged / target * 100 - 100;

        if (percentage > 0) {
            percentage = 0;
        }

        return Math.floor(percentage) + '%';
    }

    return {
        init: init
    };
});
