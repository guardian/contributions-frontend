import * as raven from 'src/modules/raven'

//Stripe should have loaded before this, because we include it the HTML before main.
export const stripe = (()=>{
    if (!("Stripe" in Window)) {
        //We can't find Stripe in the window.
        raven.Raven.captureMessage("Stripe wasn't in the window, I guess it didn't load.");
        return false
    }
    return (Window.Stripe);
})();

export function init() {
    if (!("guardian" in Window)){
        raven.Raven.captureMessage("Guardian wasn't in the window, check firststeps. It really should be.")
        return false
    }
    let guardian = Window.guardian;
    Stripe.setPublishableKey(guardian.stripePublicKey);
}

