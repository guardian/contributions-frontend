import { createStore, combineReducers } from 'redux'
import pageReducer from 'src/modules/reducers/page'
import detailsReducer from 'src/modules/reducers/details'
import cardReducer from 'src/modules/reducers/card'

let reducer = combineReducers({
    page: pageReducer,
    details: detailsReducer,
    card: cardReducer,
});

const store = createStore(reducer);
export default store;
