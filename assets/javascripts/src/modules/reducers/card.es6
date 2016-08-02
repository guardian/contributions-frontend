import UPDATE_CARD from 'src/actions'
import stripe from 'src/stripe'

export default function cardReducer(state={number:"",cvc:"",exp:"",error:""},action){
    console.log(action);
    if(action.type !== UPDATE_CARD){
        return(state);
    }
    if(!stripe){
        return(Object.assign({}, state, {error:"Stripe did not load."}));
    }

    return(Object.assign({}, state, action.details));

}
