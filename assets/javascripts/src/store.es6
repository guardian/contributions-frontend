import { createStore, combineReducers, applyMiddleware, compose } from 'redux';
import thunk from 'redux-thunk';

import pageReducer from 'src/reducers/page';
import detailsReducer from 'src/reducers/details';
import cardReducer from 'src/reducers/card';

let reducer = combineReducers({
    page: pageReducer,
    details: detailsReducer,
    card: cardReducer
});

const store = createStore(
    reducer,
    compose(applyMiddleware(thunk), window.devToolsExtension && window.devToolsExtension())
);
export default store;
