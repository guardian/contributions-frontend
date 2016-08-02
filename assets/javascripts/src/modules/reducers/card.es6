import UPDATE_CARD from 'src/actions'
import stripe from 'src/stripe'

const initialState = {
    number: '',
    cvc: '',
    expiry: ''
}

export default function cardReducer(state = initialState, action) {
    if (action.type === UPDATE_CARD)
        return Object.assign({}, state, action.details);
    else
        return state;
}
