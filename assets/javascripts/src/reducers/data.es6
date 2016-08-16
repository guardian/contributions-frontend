import { SET_DATA } from 'src/actions';

const initialState = {
    abTests: [],
    maxAmount: Infinity,
    countryGroup: {},
    currency: {
        symbol: '',
        prefix: ''
    }
};

/**
 * Core data for the app. This should only be set once on initialisation
 * with values provided by the server.
 */
export default function dataReducer(state = initialState, action) {
    switch (action.type) {
        case SET_DATA:
            return Object.assign({}, state, action.data);
        default:
            return state;
    }
}
