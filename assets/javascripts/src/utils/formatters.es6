export function formatCurrency(value) {
    if (String(value).indexOf('.') === -1) return Number(value);
    else return Number(parseFloat(value).toFixed(2));
}

export function formatCardNumber(number) {

}
