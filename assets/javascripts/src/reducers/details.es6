import { UPDATE_DETAILS, AUTOFILL} from 'src/actions';

const initialState = {
    name: '',
    email: '',
    postcode: '',
    optIn: false
};

export default function detailsReducer(state = initialState, action) {
    switch (action.type) {
        case UPDATE_DETAILS:
        case AUTOFILL:
            return Object.assign({}, state, action.details);

        default:
            return state;
    }
}
