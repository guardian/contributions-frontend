import raven from 'src/modules/raven';
import {getCookie} from 'src/utils/cookie';

const ophanUrl = '//j.ophan.co.uk/contribution.js';
const ophan = curl(ophanUrl);

export const loaded = ophan;

export function init() {
    return ophan.then(null, raven.Raven.captureException);
}

export function browserId() {
    return getCookie('bwid');
}
