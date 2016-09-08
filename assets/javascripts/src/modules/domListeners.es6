import store from 'src/store';
import {SET_DATA, SET_COUNTRY_GROUP, SET_AMOUNT, GO_FORWARD} from 'src/actions';

export function attachCurrencyListeners() {
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

export function attachErrorDialogListener() {
    let errorDialog = document.getElementById('errorDialogButton');
    if (errorDialog) {
        errorDialog.onclick = function () {
            document.getElementById('errorDialog').style.visibility = 'hidden';
        }
    }
}
