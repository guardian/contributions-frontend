import { UPDATE_CARD, SET_AMOUNT } from 'src/actions';

import { formatCurrency } from 'src/utils/formatters';

const initialState = {
    number: '',
    cvc: '',
    expiry: '',
    amount: ''
};

export default function cardReducer(state = initialState, action) {
    switch (action.type) {
        case UPDATE_CARD:
            return Object.assign({}, state, action.card);

        case SET_AMOUNT:
            return Object.assign({}, state, { amount: formatCurrency(action.amount) });

        default:
            return state;
    }
}
