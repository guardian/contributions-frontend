import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';

import Main from 'src/components/Main.jsx';
import store from 'src/store';
import { SET_DATA, SET_COUNTRY_GROUP, SET_AMOUNT, GO_FORWARD } from 'src/actions';
import ophan from 'src/modules/analytics/ophan';

export function init() {
    const container = document.getElementById('contribute');
    const presetAmount = getUrlParameter('amount');

    store.dispatch({ type: SET_DATA, data: appDataFrom(container) });

    if (presetAmount) {
        store.dispatch({ type: SET_AMOUNT, amount: parseInt(presetAmount) });
        store.dispatch({ type: GO_FORWARD });
    }

    ReactDOM.render(
        React.createElement(Provider, { store: store },
            React.createElement(Main)
        ),
        container
    );

    attachCurrencyListeners();
    setOphanId();
    attachErrorDialogListener();
}

/**
 * Convert data attributes on the container element to an object
 *
 * @param container
 * @returns {{abTests, maxAmount: number, countryGroup, currency}}
 */
function appDataFrom(container) {
    const { currency, ...countryGroup } = JSON.parse(container.dataset.countryGroup)

    return {
        abTests: JSON.parse(container.dataset.abTests),
        maxAmount: Number(JSON.parse(container.dataset.maxAmount)),
        countryGroup: countryGroup,
        currency: currency,
        cmpCode: container.dataset.cmpCode,
        intCmpCode: container.dataset.intCmpCode
    };
}

function getUrlParameter(rawName, url) {
    const name = rawName.replace(/[\[\]]/g, "\\$&");
    const regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)")
    const results = regex.exec(url || window.location.href);

    if (!results) return null;
    if (!results[2]) return '';

    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

function attachCurrencyListeners() {
    const currencyLinks = [].slice.call(document.getElementById('js-country-switcher').getElementsByTagName('a'));
    const heading = document.getElementById('js-country-name');

    currencyLinks.forEach(el => el.addEventListener('click', event => {
        const countryGroup = JSON.parse(event.target.dataset.countryGroup);

        event.preventDefault();

        store.dispatch({
            type: SET_COUNTRY_GROUP,
            countryGroup: countryGroup
        });

        store.dispatch({
            type: SET_DATA,
            data: {
                maxAmount: event.target.dataset.maxAmount
            }
        });

        heading.innerText = `${countryGroup.name} (${countryGroup.currency.symbol})`;
    }));
}
function attachErrorDialogListener() {
    document.getElementById('errorDialogButton').onclick=function(){
        console.log(document.getElementById('errorDialog'))
        document.getElementById('errorDialog').style.visibility= 'hidden';
    }
}

function setOphanId() {
    ophan.loaded.then(o => store.dispatch({
        type: SET_DATA,
        data: { ophanId: o.viewId }
    }));
}
