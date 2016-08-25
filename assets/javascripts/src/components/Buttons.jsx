import React from 'react';

export class Forward extends React.Component {
    render() {
        const { className, ...props } = this.props;

        return (
            <button className={'action action--button action--button--forward ' + (className ? className : '')} {...props}>
                <span>{this.props.children}</span>

                <svg className="action--button__arrow" xmlns="http://www.w3.org/2000/svg" width="20" height="17.89" viewBox="0 0 20 17.89">
                    <path d="M20 9.35l-9.08 8.54-.86-.81 6.54-7.31H0V8.12h16.6L10.06.81l.86-.81L20 8.51v.84zm20-.81L49.08 0l.86.81-6.54 7.31H60v1.65H43.4l6.54 7.31-.86.81L40 9.39v-.85z"/>
                </svg>
            </button>
        );
    }
}

export class Back extends React.Component {
    render() {
        const { className, ...props } = this.props;

        return (
            <button className={'action action--button action--button--back ' + (className ? className : '')} {...props}>
                <svg className="action--button__arrow" xmlns="http://www.w3.org/2000/svg" width="20" height="17.89" viewBox="0 0 20 17.89">
                    <path d="M-20 9.35l-9.08 8.54-.86-.81 6.54-7.31H-40V8.12h16.6L-29.94.81l.86-.81L-20 8.51v.84zm20-.81L9.08 0l.86.81L3.4 8.12H20v1.65H3.4l6.54 7.31-.86.81L0 9.39v-.85z"/>
                </svg>

                <span>{this.props.children}</span>
            </button>
        );
    }
}
