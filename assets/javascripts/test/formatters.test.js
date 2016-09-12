import {formatCurrency, formatCardNumber} from 'src/utils/formatters';

const stripeMock = {
    cardType: number => {
        switch (parseInt(number[0])) {
            case 3:
                return 'American Express';
            case 4:
                return 'Visa';
            case 5:
                return 'Mastercard';
        }
    }
}

window.Stripe = stripeMock;

test('currency formatting', () => {
    expect(formatCurrency(3)).toBe(3);
    expect(formatCurrency(3.12)).toBe(3.12);
    expect(formatCurrency(3.12345678)).toBe(3.12);
});

test('card number formatting', () => {
    // visa
    expect(formatCardNumber('4242424242424242')).toBe('4242 4242 4242 4242');
    expect(formatCardNumber('4 2 4 2 4 2 4 2 4 2 4 2 4 2 4 2')).toBe('4242 4242 4242 4242');

    // mastercard
    expect(formatCardNumber('5555555555554444')).toBe('5555 5555 5555 4444');
    expect(formatCardNumber('5 5 5 5 5 5 5 5 5 5 5 5 4 4 4 4')).toBe('5555 5555 5555 4444');

    // amex
    expect(formatCardNumber('371449635398431')).toBe('3714 496353 98431');
    expect(formatCardNumber('3 7 1 4 4 9 6 3 5 3 9 8 4 3 1')).toBe('3714 496353 98431');
});
