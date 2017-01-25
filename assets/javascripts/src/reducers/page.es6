import { GO_BACK, GO_FORWARD, SUBMIT_PAYMENT, OPEN_STRIPE, CLOSE_STRIPE, TOKEN_RECEIVED, PAYMENT_COMPLETE, PAYMENT_ERROR, PAYPAL_PAY, CARD_PAY, JUMP_TO_PAGE, UPDATE_DETAILS, CLEAR_PAYMENT_FLAGS } from 'src/actions';
import { PAGES } from 'src/constants';

const initialState = {
    page: 1,
    stripeReceived: false,
    processing: false,
    paypalPay: false,
    cardPay: false,
    paymentError: {
        show: false,
        kind: 'card',
        message: ''
    }
};

export default function pageReducer(state = initialState, action) {
    switch (action.type) {
        case GO_BACK:
            if (state.page === PAGES.CONTRIBUTION) return state;
            else return Object.assign({}, state, { page: state.page - 1, paymentError: { show: false } });
        case JUMP_TO_PAGE:
            if (state.page === PAGES.CONTRIBUTION) return state;
            else return Object.assign({}, state, { page: action.page, paymentError: { show: false } });

        case GO_FORWARD:
            if (state.page === PAGES.PAYMENT) return state; //this would be safer as details post stripe ab test
            else return Object.assign({}, state, { page: state.page + 1, paymentError: { show: false } });

        case OPEN_STRIPE:
            return Object.assign({}, state, { processing: true, paymentError: { show: false } });

        case CLOSE_STRIPE:
            if (state.stripeReceived){
                return state
            }
            return Object.assign({}, state, { processing: false, paymentError: { show: false } });

        case SUBMIT_PAYMENT:
            return Object.assign({}, state, { processing: true, stripeReceived: true, paymentError: { show: false } });

        case PAYMENT_COMPLETE:
            window.location.href = action.response.redirect;
            return state;

        case PAYMENT_ERROR:
            return Object.assign({}, state, {
                processing: false,
                paypalPay: false,
                cardPay: false,
                stripeReceived: false,
                paymentError: {
                    show: true,
                    message: action.error.message,
                    kind: action.kind
                }
            });

        case CLEAR_PAYMENT_FLAGS:
            return Object.assign({}, state, { paypalPay: false, cardPay:false });
        case PAYPAL_PAY:
            if (state.page != PAGES.CONTRIBUTION) return state;
            else return Object.assign({}, state, { paypalPay: true });
        case UPDATE_DETAILS:
            if (state.page == PAGES.DETAILS) return state;
            else return Object.assign({}, state, { page: PAGES.DETAILS });
        case CARD_PAY:
            if (state.page == PAGES.CONTRIBUTION) return state;
            else return Object.assign({}, state, { cardPay: true });

        default:
            return state;
    }
}
