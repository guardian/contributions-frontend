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
        target = 0,
        overfund = 0,
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
                }
            }
        };
        xhr.open('GET', path, true);
        xhr.send();
    }

    function getData() {
        loadJSON('https://interactive.guim.co.uk/docsdata-test/1no5r1O5A0omDkz4HALce4SrFWDGzSuR_jdB2MYOsPt4.json', function(data) {
            pledged = parseInt(data.sheets.Sheet1[0].total);
            target = parseInt(data.sheets.Sheet1[0].target);
            overfund = parseInt(data.sheets.Sheet1[0].overfund);

            populateText();
            positionTarget();
            animateTicker();
        });
    }

    function populateText() {
        $('.ticker__progress-label--target').text('$' + toK(target) + ' goal');
        $('.ticker__progress-label--last').text('$' + toK(overfund));
    }

    function positionTarget() {
        var percentage = target / overfund * 100;
        $('.ticker__progress-label--target').attr('style', 'left: ' + percentage + '%');
    }

    function toK(val) {
        return val / 1000 + 'K';
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
        var percentage = pledged / overfund * 100 - 100;

        if (percentage > 0) {
            percentage = 0;
        }

        return Math.floor(percentage) + '%';
    }

    return {
        init: init
    };
});
