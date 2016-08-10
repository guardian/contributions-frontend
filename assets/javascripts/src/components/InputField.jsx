import React from 'react';

export default class InputField extends React.Component {
    render() {
        return <div>
            <label htmlFor={this.props.id} className="label">{this.props.label}</label>
            <input {...this.props} className={'input-text contribute-controls__input ' + (this.props.inputClassName || '')}/>
        </div>;
    }
}
