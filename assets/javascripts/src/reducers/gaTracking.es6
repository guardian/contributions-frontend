import { TRACK_STEP, GA_ENABLED } from 'src/actions';

const initialState = {
    enabled: true,
    steps: [false, false, false, false]
};

export default function trackingReducer(state = initialState, action) {
    if (action.type === TRACK_STEP && action.step > 0 && action.step < state.steps.length) {
        state.steps[action.step] = true;
        return state;
    } else if (action.type == GA_ENABLED) {
        state.enabled = action.enabled;
        return state;
    } else {
        return state;
    }
}
