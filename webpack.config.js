var path = require('path');
var webpack = require('webpack');

module.exports = {
    entry: {
        contributePage: 'src/contributePage',
        thankYouPage: 'src/thankYouPage',
        inlinePayment: 'src/inlinePayment'
    },
    target: 'web',

    output: {
        path: path.resolve(__dirname, 'public'),
        chunkFilename: 'webpack/[chunkhash].js',
        filename: "javascripts/[name].js",
        publicPath: '/assets/'
    },

    resolve: {
        modules: [
            path.resolve(__dirname, "assets/javascripts"),
            path.resolve(__dirname, "node_modules")
        ],
        extensions: [".js", ".jsx", ".es6"],
        alias: {
            'respimage': 'respimage/respimage',
            'lazySizes': 'lazysizes/lazysizes'
        }
    },

    module: {
        rules: [
            {
                test: /\.es6$/,
                exclude: /node_modules/,
                loader: 'babel-loader',
                options: {
                    presets: ['es2015'],
                    plugins: [
                        'transform-object-rest-spread',
                        'transform-object-assign',
                        'transform-es2015-classes',
                        'transform-runtime'
                    ],
                    cacheDirectory: ''
                }
            },

            {
                test: /\.jsx$/,
                exclude: /node_modules/,
                loader: 'babel-loader',
                options: {
                    presets: ['react', 'es2015'],
                    plugins: [
                        'transform-object-rest-spread',
                        'transform-object-assign',
                        'transform-es2015-classes',
                        'transform-runtime'
                    ],
                    cacheDirectory: ''
                }
            }
        ]
    },

    stats: {
        modules: true,
        reasons: true,
        colors: true
    },

    devtool: 'source-map',

    devServer: {
        proxy: {
            '**': {
                target: 'http://localhost:9112',
                secure: false
            }
        }
    }
};
