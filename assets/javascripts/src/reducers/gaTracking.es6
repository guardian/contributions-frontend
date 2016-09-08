import { TRACKING } from 'src/actions';

const initialState = [false, false, false, false];

export default function trackingReducer(state = initialState, action) {
    if (action.type === TRACKING && action.step > 0 && action.step < state.length) {
        state[action.step] = true;
        return state;
    } else {
        return state;
    }
}
