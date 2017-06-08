import React from 'react';

function toK(val) {
    return val / 1000;
}

function positionTarget(props) {
    return (props.target / props.overfund * 100) + '%';
}

function percentageAsNegativeTranslate(props) {
    return Math.floor(props.total / props.overfund * 100 - 100) + '%';
}

export default class Ticker extends React.Component {
    constructor(props) {
        super(props);

        this.state = {count: 0, filledProgress: '-100%'};
    }

    render() {
        return (
            <div className='ticker'>
                <div className="ticker__wrapper l-constrained">
                    <h3 className="ticker__count">${this.state.count}</h3>
                    <h4 className="ticker__label">pledged so far</h4>
                    <div className="ticker__progress">
                        <div className="ticker__filled-progress" style={{transform: 'translateX(' + this.state.filledProgress + ')'}}></div>
                    </div>
                    <div className='ticker__progress-labels'>
                        <span className='ticker__progress-label ticker__progress-label--first'>$0</span>
                        <span className='ticker__progress-label ticker__progress-label--target' style={{left: positionTarget(this.props)}}>${toK(this.props.target)}K</span>
                        <span className='ticker__progress-label ticker__progress-label--last'>${toK(this.props.overfund)}K</span>
                    </div>
                </div>
            </div>
        );
    }

    componentDidMount() {
        setTimeout(() => this.setState({filledProgress: percentageAsNegativeTranslate(this.props)}), 500);

        this.interval = setInterval(() => {
            this.setState( { count: this.state.count + Math.floor(this.props.total / 60)} )
                if (this.state.count >= this.props.total) {
                    this.setState({ count: this.state.count});
                    clearInterval(this.interval);
                }
            }
        , 30);
    }
}
