export function formatCurrency(value) {
    if (String(value).indexOf('.') === -1) return value;
    else return parseFloat(value).toFixed(2);
}
