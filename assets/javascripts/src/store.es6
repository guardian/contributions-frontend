import { createStore, combineReducers, applyMiddleware, compose } from 'redux';
import thunk from 'redux-thunk';

import pageReducer from 'src/reducers/page';
import detailsReducer from 'src/reducers/details';
import cardReducer from 'src/reducers/card';
import dataReducer from 'src/reducers/data';

let reducer = combineReducers({
    page: pageReducer,
    details: detailsReducer,
    card: cardReducer,
    data: dataReducer
});

const store = createStore(
    reducer,
    applyMiddleware(thunk),
    window.devToolsExtension ? window.devToolsExtension() : f => f
);
export default store;
