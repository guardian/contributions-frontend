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
            const formatted = formatCurrency(action.amount);
            return Object.assign({}, state, { amount: isNaN(formatted) ? 0 : formatted });

        default:
            return state;
    }
}
