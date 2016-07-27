import { createStore } from 'redux';
import { GO_BACK, GO_FORWARD } from './actions'

const initialState = {
    page: 1
};

function contribute(state = initialState, action) {
    switch (action.type) {
        case GO_BACK:
            if (state.page === 1) return state;
            else return { page: state.page - 1};

        case GO_FORWARD:
            if (state.page === 3) return state;
            else return {page: state.page + 1};

        default:
            return state;
    }
}

const store = createStore(contribute);
export default store;
