import { UPDATE_DETAILS } from 'src/actions';

const initialState = {
    name: '',
    email: '',
    postcode: '',
    optIn: true
};

export default function detailsReducer(state = initialState, action) {
    switch (action.type) {
        case UPDATE_DETAILS:
            return Object.assign({}, state, action.details);

        default:
            return state;
    }
}
