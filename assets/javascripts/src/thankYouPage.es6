import * as analytics from 'src/modules/analytics/setup'
import {pageView, setCheckoutStep} from 'src/modules/analytics/ga';

analytics.init().then(() => {
    setCheckoutStep(4);
    pageView();
});

