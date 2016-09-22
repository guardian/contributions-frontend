import { TRACK_STEP, GA_ENABLED } from 'src/actions';

const initialState = {
    enabled: true,
    steps: [false, false, false, false]
};

// return a copy of `array` with element `i` having its value replaced with `value`
const replace = (array, i, value) => {
    let newArray = array.slice();

    if (i >= 0 && i < array.length) {
        newArray[i] = value
    }

    return newArray;
};

export default function trackingReducer(state = initialState, action) {
    switch (action.type) {
        case TRACK_STEP:
            return Object.assign({}, state, {
                steps: replace(state.steps, action.step, true)
            });

        case GA_ENABLED:
            return Object.assign({}, state, {
                enabled: action.enabled
            });

        default:
            return state;
    }
}
