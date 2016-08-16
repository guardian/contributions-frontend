import { SET_AB_TESTS } from 'src/actions';

const initialState = [];

export default function testReducer(state = initialState, action) {
    switch (action.type) {
        case SET_AB_TESTS:
            return action.tests;
        default:
            return state;
    }
}
