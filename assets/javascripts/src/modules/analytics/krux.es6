import 'whatwg-fetch';
import raven from 'src/modules/raven';

export function init() {
    const KRUX_ID = 'JglooLwn';

    return fetch(`https://cdn.krxd.net/controltag?confid=${KRUX_ID}`, {
        credentials: 'include'
    }).catch(raven.Raven.captureException);
}
