import { UPDATE_DETAILS, SET_RECURRING } from 'src/actions';

const initialState = {
    name: '',
    email: '',
    postcode: '',
    optIn: true,

    /**
     * recurring has 3 possible states:
     *   null: no recurring option set
     *   false: one-off payment chosen
     *   true: recurring payments chosen
     */
    recurring: null
};

export default function detailsReducer(state = initialState, action) {
    switch (action.type) {
        case UPDATE_DETAILS:
            return Object.assign({}, state, action.details);

        case SET_RECURRING:
            return Object.assign({}, state, { recurring: action.enabled })

        default:
            return state;
    }
}
