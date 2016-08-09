//global abTests
import * as ophan from 'src/modules/analytics/ophan';

export function init(){
    if ("abTests" in window){
            var data = {};
            for (var test of abTests){
                data[test.testSlug] = {
                    'variantName': test.variantSlug,
                    'complete': 'true'
                }
            }
            ophan.loaded.then(function(ophan){
                ophan.record({
                abTestRegister:data
            })});

    }
}
