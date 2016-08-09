var path = require('path');

module.exports = {
    resolve: {
        root: ["assets/javascripts", "node_modules"],
        extensions: ["", ".js", ".es6"],
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

    progress: true,
    failOnError: true,
    keepalive: false,
    inline: true,

    stats: {
        modules: true,
        reasons: true,
        colors: true
    },

    context: 'assets/javascripts',

    debug: false,
    devtool: 'source-map',
    entry: 'src/main',

    output: {
        path: path.resolve(__dirname, "public"),
        chunkFilename:  'webpack/[chunkhash].js',
        filename: "javascripts/[name].js",
        publicPath: '/assets/'
    },

    devServer: {
        proxy: {
            '/*': {
                target: 'http://localhost:9000',
                secure: false
            }
        }
    }
};
