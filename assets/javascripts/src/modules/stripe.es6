export function init() {
    Stripe.setPublishableKey(guardian.stripePublicKey);
}

export function createToken(card) {
    return new Promise((resolve, reject) => {
        Stripe.card.createToken({
            number: card.number,
            cvc: card.cvc,
            exp: card.expiry
        }, (status, response) => {
            if (response.error) reject(response.error);
            else resolve(response.id);
        });
    });
};
