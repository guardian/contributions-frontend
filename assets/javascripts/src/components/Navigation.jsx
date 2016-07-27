import React from 'react';

export default class Navigation extends React.Component {
    render() {
        return <div>
            <a onClick={this.props.goBack}>back</a> &nbsp;
            <a onClick={this.props.goForward}>forward</a>
        </div>;
    }
}
