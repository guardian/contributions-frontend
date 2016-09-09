export function formatCurrency(value) {
    if (String(value).indexOf('.') === -1) return Number(value);
    else return Number(parseFloat(value).toFixed(2));
}

export function formatCardNumber(number) {
    // the positions of spaces in amex/others
    const amexSpaces = [4, 10];
    const defaultSpaces = [4, 8, 12];
    const spaces = parseInt(number[0]) === 3 ? amexSpaces : defaultSpaces;

    return String(number)
        .replace(/\s/g, '')
        .split('')
        .reduce((digits, digit, i) => {
            if (spaces.some(s => s === i)) return digits.concat([' ', digit]);
            return digits.concat(digit);
        }, [])
        .join('')
        .trim();
}
