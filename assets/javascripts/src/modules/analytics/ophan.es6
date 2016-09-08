import raven from 'src/modules/raven';

var ophanUrl = '//j.ophan.co.uk/contribution.js';
var ophan = curl(ophanUrl);

export var loaded = ophan;

export function init() {
    return ophan.then(null, raven.Raven.captureException);
}
