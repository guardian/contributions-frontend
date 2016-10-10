import { SET_DATA, SET_COUNTRY_GROUP } from 'src/actions';

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
    ophanId: null
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

        default:
            return state;
    }
}
