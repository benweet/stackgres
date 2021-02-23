module.exports = {
    runtimeCompiler: true,
    publicPath: '/admin/',
    lintOnSave: false,
    devServer:{
      proxy: {
        '^/stackgres': {
          target: process.env.VUE_APP_API_PROXY_URL,
          pathRewrite: { '^/stackgres' : '' },
          changeOrigin: true,
          secure: false,
          logLevel: 'debug'
        },
      }
    }
  }
  