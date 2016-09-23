import { TRACK_STEP, GA_ENABLED } from 'src/actions';

const initialState = {
    enabled: true,
    steps: [false, false, false, false]
};

export default function trackingReducer(state = initialState, action) {
    switch (action.type) {
        case TRACK_STEP:
            return Object.assign({}, state, {
                steps: state.steps.map((bool, i) =>
                    i === action.step ? true : bool
                )
            });

        case GA_ENABLED:
            return Object.assign({}, state, {
                enabled: action.enabled
            });

        default:
            return state;
    }
}
