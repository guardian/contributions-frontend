import React from 'react';

const urls = {
    'uk': 'https://www.theguardian.com/info/2016/apr/04/contribution-terms-and-conditions',
    'eur': 'https://www.theguardian.com/info/2016/apr/04/contribution-terms-and-conditions',
    'us': 'https://www.theguardian.com/info/2016/apr/07/us-contribution-terms-and-conditions',
    'au': 'https://www.theguardian.com/info/2016/apr/08/australia-contribution-terms-and-conditions'
}

const defaultURL = 'https://www.theguardian.com/info/2016/apr/04/contribution-terms-and-conditions';

export default class LegalNotice extends React.Component {
    render() {
        const termsURL = urls[this.props.countryGroup.id] || defaultURL;

        return (
            <div className="fieldset__note">
                <p>By proceeding, you are agreeing to our <a href={termsURL} className="text-link" target="_blank">Terms & Conditions</a> and <a href="http://www.theguardian.com/help/privacy-policy" className="text-link" target="_blank">Privacy Policy</a></p>
            </div>
        );
    }
}
