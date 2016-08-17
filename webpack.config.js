var path = require('path');
var webpack = require('webpack');

module.exports = {
    context: 'assets/javascripts',
    entry:  'src/main',

    output: {
        path: path.resolve(__dirname, "public"),
        chunkFilename: 'webpack/[chunkhash].js',
        filename: "javascripts/[name].js",
        publicPath: '/assets/'
    },

    resolve: {
        root: [
            path.resolve(__dirname, "assets/javascripts"),
            path.resolve(__dirname, "node_modules")
        ],
        extensions: ["", ".js", ".jsx", ".es6"],
        alias: {
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
                    plugins: ['transform-object-rest-spread'],
                    cacheDirectory: ''
                }
            },

            {
                test: /\.jsx$/,
                exclude: /node_modules/,
                loader: 'babel',
                query: {
                    presets: ['react', 'es2015'],
                    plugins: ['transform-object-rest-spread'],
                    cacheDirectory: ''
                }
            }
        ]
    },

    resolveLoader: {
        root: path.join(__dirname, "node_modules")
    },

    progress: true,
    failOnError: true,
    keepalive: false,
    inline: true,

    stats: {
        modules: true,
        reasons: true,
        colors: true
    },

    debug: false,
    devtool: 'source-map',

    devServer: {
        proxy: {
            '/*': {
                target: 'http://localhost:9111',
                secure: false
            }
        }
    }
};
