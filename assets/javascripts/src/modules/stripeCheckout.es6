var handler;

export function init(){
    if(!("StripeCheckout" in window && "guardian" in window)){
       return;
    }
    handler = StripeCheckout.configure(guardian.stripeCheckout);
    // Close Checkout on page navigation:
    window.addEventListener('popstate', function() {
        handler.close();
    });

}

export function showCheckout(token, closed, email, amount, currency){
    if(!handler){
        return;
    }

    handler.open({
        email: email,
        amount: amount * 100,
        currency: currency,
        token: token,
        closed: closed
    })
}
