import React from 'react';

export default class Spinner extends React.Component {
    render() {
        return <div className="spinner-outer">
            <div className="spinner">
                <div>
                    <div></div>
                </div>
                <div>
                    <div></div>
                </div>
                <div>
                    <div></div>
                </div>
                <div>
                    <div></div>
                </div>
                <div>
                    <div></div>
                </div>
                <div>
                    <div></div>
                </div>
                <div>
                    <div></div>
                </div>
                <div>
                    <div></div>
                </div>
            </div>

            <span className="spinner-text">{this.props.text}</span>
        </div>;
    }
}
