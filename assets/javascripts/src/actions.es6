import checkout from 'src/stripe'

export const GO_BACK = "GO_BACK"
export const GO_FORWARD = "GO_FORWARD"
export const UPDATE_DETAILS = "UPDATE_DETAILS"
export const UPDATE_CARD = "UPDATE_CARD"
export const PAY = "PAY"

export const PAGES = {
    CONTRIBUTION: 1,
    DETAILS: 2,
    PAYMENT: 3,
    PROCESSING: 4,
    GRATS: 5
}

export function stripeCheckout(){
    return (dispatch, getState) => {
        dispatch({type: PAY});
        const state = getState();
        const card = state.card;
        const promise = checkout(card,success)

    }
}
