import { createStore, combineReducers } from 'redux';
import pageReducer from 'src/modules/reducers/page'
import detailsReducer from 'src/modules/reducers/details'

let reducer = combineReducers({
    page: pageReducer,
    details: detailsReducer
});

const store = createStore(reducer);
export default store;
