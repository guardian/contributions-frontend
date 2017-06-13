import React from 'react';
import ProgressBar from './ProgressBar';
import Counter from './Counter';

const duration = 5;

export default class Ticker extends React.Component {
    constructor(props) {
        super(props);

        this.state = {count: 0};
    }

    render() {
        return (
            <div className='ticker'>
                <div className="ticker__wrapper l-constrained">
                    <Counter total={this.props.total} duration={duration} />
                    <h4 className="ticker__label">pledged so far</h4>
                    <ProgressBar
                        total={this.props.total}
                        target={this.props.target}
                        overfund={this.props.overfund}
                        duration={duration}
                    />
                </div>
            </div>
        );
    }
}
