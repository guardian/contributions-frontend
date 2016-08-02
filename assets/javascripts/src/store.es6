import { createStore, combineReducers, applyMiddleware } from 'redux'
import thunk from 'redux-thunk'

import pageReducer from 'src/modules/reducers/page'
import detailsReducer from 'src/modules/reducers/details'
import cardReducer from 'src/modules/reducers/card'

let reducer = combineReducers({
    page: pageReducer,
    details: detailsReducer,
    card: cardReducer,
});

const store = createStore(
    reducer,
    applyMiddleware(thunk)
);
export default store;
