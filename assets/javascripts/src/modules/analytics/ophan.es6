import ophan from 'ophan-tracker-js/build/ophan.contribution';
import cookie from 'src/utils/cookie';

export const viewId = ophan.viewId;
export const browserId = cookie.getCookie('bwid');
export const visitId = cookie.getCookie('vsid');

export const record = event => new Promise(resolve => ophan.record(event, resolve));
