import React from 'react';

export class AmountButton extends React.Component {
    render() {
        return <button type="button"
                       tabIndex="0"
                       key={this.props.amount}
                       className={'contribute-controls__button option-button ' + (this.props.highlight && this.props.currentAmount === this.props.amount ? ' active' : '')}
                       onClick={this.props.onClick}
                       data-amount={this.props.amount}>{this.props.children}</button>
    }
}

export class ButtonGroup extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            active: null
        }
    }

    handleClick(option, i) {
        this.props.onClick(option, i);

        this.setState({
            active: i
        });
    }

    render() {
        return <div className={'option-button-group ' + this.props.className}>
            {this.props.options.map((option, i) => {
                const active = this.state.active === i;

                return <a key={i}
                          className={'option-button ' + (active ? 'active' : '')}
                          onClick={this.handleClick.bind(this, option, i)}>
                    {option}
                </a>;
            })}
        </div>;
    }
}

export class Button extends React.Component {
    render() {
        const { className, ...props } = this.props;

        return <button className={(className ? className : '')} {...props}>
            {this.props.children}
        </button>;
    }
}

export class Forward extends React.Component {
    render() {
        const { className, ...props } = this.props;

        return <Button className={'action action--button action--button--forward ' + (className ? className : '')} {...props}>
            <span>{this.props.children}</span>

            <svg className="action--button__arrow" xmlns="http://www.w3.org/2000/svg" width="20" height="17.89" viewBox="0 0 20 17.89">
                <path d="M20 9.35l-9.08 8.54-.86-.81 6.54-7.31H0V8.12h16.6L10.06.81l.86-.81L20 8.51v.84zm20-.81L49.08 0l.86.81-6.54 7.31H60v1.65H43.4l6.54 7.31-.86.81L40 9.39v-.85z"/>
            </svg>
        </Button>;
    }
}

export class Back extends React.Component {
    render() {
        const { className, ...props } = this.props;

        return <Button className={'action action--button action--button--back ' + (className ? className : '')} {...props}>
            <svg className="action--button__arrow" xmlns="http://www.w3.org/2000/svg" width="20" height="17.89" viewBox="0 0 20 17.89">
                <path d="M-20 9.35l-9.08 8.54-.86-.81 6.54-7.31H-40V8.12h16.6L-29.94.81l.86-.81L-20 8.51v.84zm20-.81L9.08 0l.86.81L3.4 8.12H20v1.65H3.4l6.54 7.31-.86.81L0 9.39v-.85z"/>
            </svg>

            <span>{this.props.children}</span>
        </Button>;
    }
}
