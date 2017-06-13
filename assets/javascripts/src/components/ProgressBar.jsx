import React from 'react';

const positionTarget = (target, overfund) => {
    return (target / overfund * 100) + '%';
};

const percentageAsNegativeTranslate = (total, overfund) => {
    return Math.floor(total / overfund * 100 - 100) + '%';
};

const toK = (val) => {
    return val / 1000;
};

export default class ProgressBar extends React.Component {
    constructor(props) {
        super(props);

        this.state = {filledProgress: '-100%'};
    }

    render() {
        return (
            <div className="ticker__progress-wrapper">
                <div className="ticker__progress">
                    <div className="ticker__filled-progress"
                         style={{
                            transition: 'transform ' + this.props.duration + 's cubic-bezier(.25,.55,.2,.85)',
                            transform: 'translateX(' + this.state.filledProgress + ')'
                         }}>
                    </div>
                </div>
                <div className="ticker__progress-labels">
                    <span className='ticker__progress-label ticker__progress-label--first'>$0</span>
                    <span className='ticker__progress-label ticker__progress-label--target'
                          style={{left: positionTarget(this.props.taget, this.props.overfund)}}
                    >${toK(this.props.target)}K</span>
                <span className='ticker__progress-label ticker__progress-label--last'>${toK(this.props.overfund)}K</span>
                </div>
            </div>
        );
    };

    componentDidMount() {
        setTimeout(() => this.setState({filledProgress: percentageAsNegativeTranslate(this.props.total, this.props.overfund)}), 500);
    }
}
