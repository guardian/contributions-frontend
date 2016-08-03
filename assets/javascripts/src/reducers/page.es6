import { GO_BACK, GO_FORWARD, PAY } from 'src/actions';
import { PAGES } from 'src/constants';

export default function pageReducer(state = 1, action) {
    switch (action.type) {
        case GO_BACK:
            if (state === PAGES.CONTRIBUTION) return state;
            else return state - 1;

        case GO_FORWARD:
            if (state === PAGES.PAYMENT) return state;
            else return state + 1;

        case PAY:
            return PAGES.PROCESSING;

        default:
            return state;
    }
}
