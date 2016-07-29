import { GO_BACK, GO_FORWARD } from 'src/actions'

export default function pageReducer(state = 1, action) {
    switch (action.type) {
        case GO_BACK:
            if (state === 1) return state;
            else return state - 1;

        case GO_FORWARD:
            if (state === 3) return state;
            else return state + 1;

        default:
            return state;
    }
}
