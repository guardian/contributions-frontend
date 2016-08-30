var Uglify = require("webpack/lib/optimize/UglifyJsPlugin");
var path = require('path');

module.exports = {
    resolve: {
        root: ["assets/javascripts", "node_modules"],
        extensions: ["", ".js", ".es6"],
        alias: {
            '$$': 'src/utils/$',
            //'lodash': 'lodash-amd/modern',
            'bean': 'bean/bean',
            'bonzo': 'bonzo/bonzo',
            'qwery': 'qwery/qwery',
            'reqwest': 'reqwest/reqwest',
            'respimage': 'respimage/respimage',
            'lazySizes': 'lazysizes/lazysizes',
            'gumshoe': 'gumshoe/dist/js/gumshoe',
            'smoothScroll': 'smooth-scroll/dist/js/smooth-scroll',
            'ajax': 'src/utils/ajax'
        }
    },

    module: {
        loaders: [
            {
                test: /\.es6$/,
                exclude: /node_modules/,
                loader: 'babel',
                query: {
                    presets: ['es2015'],
                    cacheDirectory: ''
                }
            },

            {
                test: /\.jsx$/,
                exclude: /node_modules/,
                loader: 'babel',
                query: {
                    presets: ['react', 'es2015'],
                    cacheDirectory: ''
                }
            }
        ]
    },

    resolveLoader: {
        root: path.join(__dirname, "node_modules")
    },

    plugins: [new Uglify({ compress: { warnings: false }})],

    progress: true,
    failOnError: true,
    watch: false,
    keepalive: false,
    inline: true,
    hot: false,

    stats: {
        modules: true,
        reasons: true,
        colors: true
    },

    context: 'assets/javascripts',
    debug: false,
    devtool: 'source-map'
};
