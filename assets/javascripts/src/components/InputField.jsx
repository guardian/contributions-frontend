import React from 'react';

export default class InputField extends React.Component {
    render() {
        const { halfWidth, ...props } = this.props;

        return <div className={halfWidth ? 'half-width' : ''}>
            <label htmlFor={this.props.id} className="label">{this.props.label}</label>
            <input {...props} className='input-text contribute-controls__input' />
        </div>;
    }
}
