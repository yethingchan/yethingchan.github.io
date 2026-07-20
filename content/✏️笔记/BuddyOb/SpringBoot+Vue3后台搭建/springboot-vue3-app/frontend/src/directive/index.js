import hasPermi from './hasPermi'

export default {
  install(app) {
    // 用法：v-hasPermi="'system:user:add'"
    app.directive('hasPermi', hasPermi)
  }
}
