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
        const rate = 1000 / 40;
        const delay = 1000; //matches the delay for the progress bar
        const increment = Math.floor(this.props.total / (((this.props.duration * 1000) - delay) / rate));

        const interval = setInterval(() => {
           if (this.state.count >= this.props.total) {
                this.setState({ count: parseInt(this.props.total) });
                clearInterval(this.interval);
           } else {
               this.setState( { count: this.state.count + increment } )
           }
        }, rate);
    }
}
