import raven from 'src/modules/raven';
import 'whatwg-fetch';

export let ophan;

export function init() {
    const ophanUrl = '//j.ophan.co.uk/membership.js';

    return fetch(ophanUrl)
        .then(res => ophan = res)
        .catch(err => raven.Raven.captureException(err));
}
