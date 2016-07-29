import {UPDATE_DETAILS} from 'src/actions'

export default function detailsReducer(state = {name: "", email: "", postcode: ""}, action) {
    if (action.type !== UPDATE_DETAILS) {
        return(state);
    }

    return(Object.assign({}, state, action.details));
}
