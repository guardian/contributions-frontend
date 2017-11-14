import React from 'react';

const percentageAsNegativeTranslate = (total, target) => {
    return Math.floor(total / target * 100 - 100) + '%';
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
                <span className='ticker__progress-label ticker__progress-label--last'>${toK(this.props.target)}K</span>
                </div>
            </div>
        );
    };

    componentDidMount() {
        const cappedTotal = Math.min(this.props.total, this.props.target);
        setTimeout(() => this.setState({filledProgress: percentageAsNegativeTranslate(cappedTotal, this.props.target)}), 500);
    }
}
