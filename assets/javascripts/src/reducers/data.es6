import { SET_DATA, SET_COUNTRY_GROUP, SET_RECURRING_NOTIFIED, DISMISS_RECURRING_NOTIFICATION } from 'src/actions';

const initialState = {
    abTests: [],
    maxAmount: Infinity,
    countryGroup: {},
    currency: {
        symbol: '',
        identifier: '',
        prefix: '',
        code: ''
    },
    cmpCode: '',
    intCmpCode: '',
    ophanId: null,

    /**
     * the state of the recurring test notification:
     *
     *  0: we haven't shown the notification yet
     *  1: show the notification
     *  2: the notification has been dismissed
     */
    recurringNotified: 0
};

/**
 * Core data for the app. This should only be set once on initialisation
 * with values provided by the server and when changing country group.
 */
export default function dataReducer(state = initialState, action) {
    switch (action.type) {
        case SET_DATA:
            return Object.assign({}, state, action.data);

        case SET_COUNTRY_GROUP:
            const { currency, ...countryGroup } = action.countryGroup;

            return Object.assign({}, state, { currency: currency, countryGroup: countryGroup });

        case SET_RECURRING_NOTIFIED:
            return Object.assign({}, state, { recurringNotified: 1 });

        case DISMISS_RECURRING_NOTIFICATION:
            return Object.assign({}, state, { recurringNotified: 2 });

        default:
            return state;
    }
}
