import { GO_BACK, GO_FORWARD, SUBMIT_PAYMENT } from 'src/actions';
import { PAGES } from 'src/constants';

const initialState = {
    page: 1,
    processing: false
};

export default function pageReducer(state = initialState, action) {
    switch (action.type) {
        case GO_BACK:
            if (state.page === PAGES.CONTRIBUTION) return state;
            else return Object.assign({}, state, { page: state.page - 1 });

        case GO_FORWARD:
            if (state.page === PAGES.PAYMENT) return state;
            else return Object.assign({}, state, { page: state.page + 1 });

        case SUBMIT_PAYMENT:
            return Object.assign({}, state, { processing: true });

        default:
            return state;
    }
}
