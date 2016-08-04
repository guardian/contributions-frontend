import { UPDATE_CARD, SET_AMOUNT } from 'src/actions';

const initialState = {
    number: '',
    cvc: '',
    expiry: '',
    amount: 0,
    valid: false
};

export default function cardReducer(state = initialState, action) {
    switch (action.type) {
        case UPDATE_CARD:
            return Object.assign({}, state, action.card);

        case SET_AMOUNT:
            return Object.assign({}, state, { amount: action.amount });

        default:
            return state;
    }
}
