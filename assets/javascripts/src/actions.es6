import checkout from 'src/stripe'

export const GO_BACK = "GO_BACK"
export const GO_FORWARD = "GO_FORWARD"
export const UPDATE_DETAILS = "UPDATE_DETAILS"
export const UPDATE_CARD = "UPDATE_CARD"
export const PAY = "PAY"

export function stripeCheckout(){
    return (dispatch, getState) => {
        dispatch({type: PAY});
        const state = getState();
        const card = state.card;
        const promise = checkout(card,success)
    }
}
