import { SET_DATA, SET_STRIPE_HANDLER, SET_COUNTRY_GROUP, SUBMIT_PAYMENT } from 'src/actions';

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
    refererPageviewId: '',
    refererUrl: '',
    ophan: {
        pageviewId: null,
        browserId: null
    },
    stripe: {
        handler: null,
        token: null,
    }
};

/**
 * Core data for the app. This should only be set once on initialisation
 * with values provided by the server and when changing country group.
 */
export default function dataReducer(state = initialState, action) {
    switch (action.type) {
        case SET_DATA:
            return Object.assign({}, state, action.data);

        case SET_STRIPE_HANDLER:
            return Object.assign({}, state, { stripe:
                { handler: action.handler }
            });

        case SET_COUNTRY_GROUP:
            const { currency, ...countryGroup } = action.countryGroup;
            return Object.assign({}, state, { currency: currency, countryGroup: countryGroup });

        case SUBMIT_PAYMENT:
            return Object.assign({}, state, { ophan: {
                pageviewId: state.ophan.pageviewId,
                browserId: state.ophan.browserId,
                visitId: state.ophan.visitId
            }});

        default:
            return state;
    }
}
