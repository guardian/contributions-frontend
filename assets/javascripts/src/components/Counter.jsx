import React from 'react';

export default class Counter extends React.Component {
    constructor(props) {
        super(props);

        this.state = {count: 0};
    }

    render() {
        return (
            <h3 className="ticker__count">${this.state.count.toLocaleString()}</h3>
        );
    }

    componentDidMount() {
        const rate = this.props.duration * 1000 / 60;

        const interval = setInterval(() => {
           if (this.state.count >= this.props.total) {
                this.setState({ count: this.state.count});
                clearInterval(this.interval);
           } else {
               this.setState( { count: this.state.count + Math.floor(this.props.total / 60)} )
           }
        }, rate);
    }
}
