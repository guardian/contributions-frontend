import React from 'react';

export class AmountInput extends React.Component {
    render() {
        const { inputAmount, symbol, refFn, ...props } = this.props;

        return <span className={'contribute-controls__input contribute-controls__input--amount input-text ' + (this.props.small ? 'contribute-controls__input-small':'')}>
            <span className={'symbol ' + (!!inputAmount ? 'active' : '')}>{symbol}</span>
            <input type="number"
                   ref={refFn.bind(this)} // create a reference to this element for validation (see: https://facebook.github.io/react/docs/more-about-refs.html)
                   placeholder="Other amount" maxLength="10"
                   value={inputAmount}
                   onChange={this.props.onChange.bind(this)}
                   className={this.props.small ? 'contribute-controls__small-input': ''}
                   {...props} />
        </span>
    }
}

export class InputField extends React.Component {
    render() {
        const { outerClassName, children, ...props } = this.props;

        return <div className={outerClassName || ''}>
            <label htmlFor={this.props.id} className="label">{this.props.label}
                {!this.props.required && <span className="label-optional">(optional)</span>}
            </label>
            <input {...props} className={'input-text contribute-controls__input ' + (props.className || '')} />
            {children}
        </div>;
    }
}
