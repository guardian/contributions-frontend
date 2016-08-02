import * as raven from 'src/modules/raven'

//Stripe should have loaded before this, because we include it the HTML before main.
export const stripe = (()=> {
    if (!("Stripe" in Window)) {
        //We can't find Stripe in the window.
        raven.Raven.captureMessage("Stripe wasn't in the window, I guess it didn't load.");
        return false
    }
    return (Window.Stripe);
})();

let initialised = false;

export function init() {
    if (!("guardian" in Window)) {
        raven.Raven.captureMessage("Guardian wasn't in the window, check firststeps. It really should be.")
        return false
    }
    let guardian = Window.guardian;
    Stripe.setPublishableKey(guardian.stripePublicKey);
    initialised = true;
    return true
}

export function checkout(cardData) {
    return new Promise((resolve, reject)=> {
        if (!initialised) {
            reject('Not adequately stripey.');
            return;
        }
        Stripe.card.createToken(cardData, resolve)
    })
}

export function validateCardNumber(number){
    return initialised && stripe.validateCardNumber(number)
}
export function validateCVC(cvc){
    return initialised && stripe.validateCVC(cvc)
}
export function validateExpiry(date){
    return initialised && stripe.validateExpiry(date)
}
export function cardType(number){
    return initialised && stripe.cardType(number)
}

