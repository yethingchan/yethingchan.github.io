---
title: "快捷命令：Vue 3 Snippets"
description: ""
date: "2026-05-31"
tags: []
share: true
cover: https://public.ysjf.com/mediastorm/material/material/%E8%87%AA%E7%84%B6%E9%A3%8E%E5%85%89_%E4%B8%9C%E5%8C%97_5_%E5%85%A8%E6%99%AF.jpg
---
下面我把 **html.json、javascript.json、pug.json、vue.json** 里所有 **prefix（快捷命令）+ body（生成代码）** 整理成一份**总表**，直接复制就能查、能背。

## 一、html.json（模板指令）

| 快捷命令                 | 生成代码                                                |
| -------------------- | --------------------------------------------------- |
| template             | `<template>\n\t<div>\n\t\t\n\t</div>\n</template>`  |
| vText                | `v-text="msg"`                                      |
| vHtml                | `v-html="html"`                                     |
| vShow                | `v-show="condition"`                                |
| vIf                  | `v-if="condition"`                                  |
| vElse                | `v-else`                                            |
| vElseIf              | `v-else-if="condition"`                             |
| vForWithoutKey       | `v-for="item in items"`                             |
| vFor                 | `v-for="(item, index) in items" :key="index"`       |
| vOn                  | `v-on:event="handle"`                               |
| vBind                | `v-bind=""`                                         |
| vModel               | `v-model="something"`                               |
| vSlot                | `v-slot=""`                                         |
| vPre                 | `v-pre`                                             |
| vCloak               | `v-cloak`                                           |
| vOnce                | `v-once`                                            |
| key                  | `:key="key"`                                        |
| ref                  | `ref="reference"`                                   |
| slotA                | `slot=""`                                           |
| slotE                | `<slot></slot>`                                     |
| slotScope            | `slot-scope=""`                                     |
| teleport             | `<teleport to='' />`                                |
| scope                | `scope="this api replaced by slot-scope in 2.5.0+"` |
| component            | `<component :is="componentId"></component>`         |
| keepAlive            | `<keep-alive>\n\n</keep-alive>`                     |
| transition           | `<transition>\n\n</transition>`                     |
| transitionGroup      | `<transition-group>\n\n</transition-group>`         |
| enterClass           | `enter-class=""`                                    |
| leaveClass           | `leave-class=""`                                    |
| appearClass          | `appear-class=""`                                   |
| enterToClass         | `enter-to-class=""`                                 |
| leaveToClass         | `leave-to-class=""`                                 |
| appearToClass        | `appear-to-class=""`                                |
| enterActiveClass     | `enter-active-class=""`                             |
| leaveActiveClass     | `leave-active-class=""`                             |
| appearActiveClass    | `appear-active-class=""`                            |
| beforeEnterEvent     | `@before-enter=""`                                  |
| beforeLeaveEvent     | `@before-leave=""`                                  |
| beforeAppearEvent    | `@before-appear=""`                                 |
| enterEvent           | `@enter=""`                                         |
| leaveEvent           | `@leave=""`                                         |
| appearEvent          | `@appear=""`                                        |
| afterEnterEvent      | `@after-enter=""`                                   |
| afterLeaveEvent      | `@after-leave=""`                                   |
| afterAppearEvent     | `@after-appear=""`                                  |
| enterCancelledEvent  | `@enter-cancelled=""`                               |
| leaveCancelledEvent  | `@leave-cancelled=""`                               |
| appearCancelledEvent | `@appear-cancelled=""`                              |
| routerLink           | `<router-link></router-link>`                       |
| routerLinkTo         | `<router-link to=""></router-link>`                 |
| to                   | `to=""`                                             |
| tag                  | `tag=""`                                            |
| routerView           | `<router-view></router-view>`                       |
| nuxt                 | `<nuxt/>`                                           |
| nuxtChild            | `<nuxt-child/>`                                     |
| nuxtLink             | `<nuxt-link to=""></nuxt-link>`                     |
| componentIs          | `<component :is=''></component>`                    |

## 二、javascript.json（JS/setup）

| 快捷命令 | 生成代码 |
|---|---|
| import | `import $1 from '$2'` |
| importFromVue | `import { } from 'vue'` |
| newVue | `new Vue({\n\t\n})` |
| VueConfigSilent | `Vue.config.silent = true` |
| VueConfigOptionMergeStrategies | `app.config.optionMergeStrategies. = (parent, child, vm) => {\n\treturn \n}` |
| VueConfigDevtools | `Vue.config.devtools = true` |
| VueConfigErrorHandler | `app.config.errorHandler = (err, vm, info) => {\n\t// handle error\n}` |
| VueConfigWarnHandler | `app.config.warnHandler = (msg, vm, trace) => {\n\t// handle warn\n}` |
| AppConfigGlobalProperties | `app.config.globalProperties. = ` |
| AppConfigIsCustomElement | `app.config.isCustomElement = ` |
| VueConfigIgnoredElements | `Vue.config.ignoredElements = [\n\t''\n]` |
| VueConfigKeyCodes | `Vue.config.keyCodes = {\n\t// camelCase won`t work\n}` |
| VueConfigPerformance | `app.config.performance = true` |
| VueConfigProductionTip | `Vue.config.productionTip = false` |
| defineComponent | `defineComponent({\n\t\n})` |
| defineAsyncComponent | `defineAsyncComponent` |
| defineAsyncComponentWithObj | `const AsyncComp = defineAsyncComponent({\n\tloader: () => import('')\n\tloadingComponent: loadingComponent,\n\terrorComponent: errorComponent,\n\tdelay: 20,\n\ttimeout: 3000,\n\tsuspensible: false,\n\tonError(error, retry, fail, attempts) {\n\t\tif (error.message.match(/fetch/) && attempts <= 3) {\n\t\t\tretry()\n\t\t} else {\n\t\t\tfail()\n\t\t}\n\t},\n})` |
| resolveComponentExpression | `const MyComponent = resolveComponent('MyComponent')` |
| resolveDynamicComponentExpression | `const MyComponent = resolveDynamicComponent(MyComponent)` |
| resolveDirective | `const highlightDirective = resolveDirective('highlight')` |
| vueExtend | `Vue.extend({\n\ttemplate:template\n})` |
| VueNextTick | `Vue.nextTick({\n\t\n})` |
| VueNextTickThen | `Vue.nextTick({\n\t\n}).then(function () {\n\t\n})` |
| VueSet | `Vue.set(target, key, value)` |
| VueDelete | `Vue.delete(target, key)` |
| VueDirective | `Vue.directive(id)` |
| VueFilter | `Vue.filter(id)` |
| VueComponent | `Vue.component(id)` |
| VueUse | `Vue.use(plugin)` |
| VueMixin | `Vue.mixin({mixin})` |
| VueCompile | `Vue.compile(template)` |
| VueObservable | `Vue.observable({object})` |
| VueVersion | `Vue.version` |
| data | `data() {\n\treturn {\n\t\t\n\t}\n},` |
| props | `props` |
| propsData | `propsData` |
| scopedSlots | `scopedSlots` |
| computedV2 | `computed: {\n\t\n},` |
| methods | `methods: {\n\t\n},` |
| watchV2 | `watch: {\n\t\n},` |
| watchWithOptions | `key: {\n\tdeep: true,\n\timmediate: true,\n\thandler: function (val, oldVal) {\n\t\t\n\t},\n},` |
| el | `el` |
| template | `template` |
| render | `render(h) {\n\t\n},` |
| renderError | `renderError(h, err) {\n\t\n},` |
| beforeCreate | `beforeCreate() {\n\t\n},` |
| created | `created() {\n\t\n},` |
| beforeMount | `beforeMount() {\n\t\n},` |
| mounted | `mounted() {\n\t\n},` |
| beforeUpdate | `beforeUpdate() {\n\t\n},` |
| updated | `updated() {\n\t\n},` |
| activated | `activated() {\n\t\n},` |
| deactivated | `deactivated() {\n\t\n},` |
| beforeUnmount | `beforeUnmount() {\n\t\n},` |
| unmounted | `unmounted() {\n\t\n},` |
| beforeDestroy | `beforeDestroy() {\n\t\n},` |
| destroyed | `destroyed() {\n\t\n},` |
| errorCaptured | `errorCaptured: (err, vm, info) => {\n\t},` |
| renderTracked | `renderTracked({key, target, type}) {\n\t},` |
| renderTriggered | `renderTriggered({key, target, type}) {\n\t},` |
| directives | `directives` |
| filters | `filters` |
| component | `component` |
| components | `components` |
| parent | `parent` |
| mixins | `mixins` |
| extends | `extends` |
| provide | `provide` |
| inject | `inject` |
| name | `name` |
| delimiters | `delimiters` |
| functional | `functional` |
| model | `model` |
| inheritAttrs | `inheritAttrs` |
| comments | `comments` |
| deep | `deep` |
| immediate | `immediate` |
| vmData | `this.$data` |
| vmProps | `this.$props` |
| vmEl | `this.$el` |
| vmOptions | `this.$options` |
| vmParent | `this.$parent` |
| vmRoot | `this.$root` |
| vmChildren | `this.$children` |
| vmSlots | `this.$slots` |
| vmScopedSlots | `this.$scopedSlots.default({})` |
| vmRefs | `this.$refs` |
| vmIsServer | `this.$isServer` |
| vmAttrs | `this.$attrs` |
| vmListeners | `this.$listeners` |
| vmWatch | `this.$watch(expOrFn, callback)` |
| vmSet | `this.$set(target, key, value)` |
| vmDelete | `this.$delete(target, key)` |
| vmOn | `this.$on('event', callback)` |
| vmOnce | `this.$once('event', callback)` |
| vmOff | `this.$off('event', callback)` |
| vmEmit | `this.$emit('event')` |
| vmMount | `this.$mount('')` |
| vmForceUpdate | `this.$forceUpdate()` |
| vmNextTick | `this.$nextTick(callback)` |
| vmDestroy | `this.$destroy()` |
| renderer | `const renderer = require('vue-server-renderer').createRenderer()` |
| createRenderer | `createRenderer({\n\t\n})` |
| renderToString | `renderToString` |
| renderToStream | `renderToStream` |
| createBundleRenderer | `createBundleRenderer` |
| bundleRendererRenderToString | `bundleRenderer.renderToString` |
| bundleRendererRenderToStream | `bundleRenderer.renderToStream` |
| preventDefault | `preventDefault();` |
| stopPropagation | `stopPropagation();` |
| importVueRouter | `import VueRouter from 'vue-router'` |
| newVueRouter | `const router = new VueRouter({\n\t\n})` |
| routerBeforeEach | `router.beforeEach((to, from, next) => {\n\t// to and from are both route objects. must call `next`.\n})` |
| routerBeforeResolve | `router.beforeResolve((to, from, next) => {\n\t// to and from are both route objects. must call `next`.\n})` |
| routerAfterEach | `router.afterEach((to, from) => {\n\t// to and from are both route objects.\n})` |
| routerPush | `router.push()` |
| routerReplace | `router.replace()` |
| routerGo | `router.go()` |
| routerBack | `router.back()` |
| routerForward | `router.forward()` |
| routerGetMatchedComponents | `router.getMatchedComponents()` |
| routerResolve | `router.resolve()` |
| routerAddRoutes | `router.addRoutes()` |
| routerOnReady | `router.onReady()` |
| routerOnError | `router.onError()` |
| routes | `routes: []` |
| beforeEnter | `beforeEnter: (to, from, next) => {\n\t// ...\n}` |
| beforeRouteEnter | `beforeRouteEnter (to, from, next) {\n\t// ...\n}` |
| beforeRouteLeave | `beforeRouteLeave (to, from, next) {\n\t// ...\n}` |
| scrollBehavior | `scrollBehavior (to, from, savedPosition) {\n\t// ...\n}` |
| path | `path` |
| alias | `alias` |
| mode | `mode` |
| children | `children` |
| meta | `meta` |
| newVuexStore | `const store = new Vuex.Store({\n\t// ...\n})` |
| state | `state` |
| getters | `getters` |
| mutations | `mutations` |
| actions | `actions` |
| modules | `modules` |
| plugins | `plugins` |
| commit | `commit` |
| dispatch | `dispatch` |
| replaceState | `replaceState` |
| subscribe | `subscribe` |
| registerModule | `registerModule` |
| unregisterModule | `unregisterModule` |
| hotUpdate | `hotUpdate` |
| mapState | `mapState` |
| mapGetters | `mapGetters` |
| mapActions | `mapActions` |
| mapMutations | `mapMutations` |
| asyncData | `asyncData({isDev, route, store, env, params, query, req, res, redirect, error}) {\n\t\n},` |
| VueCreateApp | `const app = Vue.createApp({})` |
| reactive | `const obj = reactive()` |
| readonly | `const copy = readonly()` |
| isProxy | `isProxy` |
| isReactive | `isReactive` |
| isReadonly | `isReadonly` |
| toRaw | `toRaw` |
| markRaw | `markRaw` |
| shallowReactive | `shallowReactive` |
| ref | `ref` |
| unref | `unref` |
| toRef | `toRef` |
| toRefs | `toRefs` |
| isRef | `isRef` |
| customRef | `customRef` |
| shallowRef | `shallowRef` |
| triggerRef | `triggerRef` |
| computed | `computed` |
| watchEffect | `watchEffect()` |
| watch | `watch` |
| setup | `setup(props) {\n\t\n}` |
| onBeforeMount | `onBeforeMount(() => {\n\t\n}),` |
| onMounted | `onMounted(() => {\n\t\n}),` |
| onBeforeUpdate | `onBeforeUpdate(() => {\n\t\n}),` |
| onUpdated | `onUpdated(() => {\n\t\n}),` |
| onBeforeUnmount | `onBeforeUnmount(() => {\n\t\n}),` |
| onUnmounted | `onUnmounted(() => {\n\t\n}),` |
| onErrorCaptured | `onErrorCaptured(() => {\n\t\n}),` |
| onRenderTracked | `onRenderTracked(() => {\n\t\n}),` |
| onRenderTriggered | `onRenderTriggered(() => {\n\t\n}),` |

## 三、pug.json（Pug模板，和html.json几乎一样）

| 快捷命令 | 生成代码 |
|---|---|
| vText | `v-text="msg"` |
| vHtml | `v-html="html"` |
| vShow | `v-show="condition"` |
| vIf | `v-if="condition"` |
| vElse | `v-else` |
| vElseIf | `v-else-if="condition"` |
| vForWithoutKey | `v-for="item in items"` |
| vFor | `v-for="item in items" :key="item.id"` |
| vOn | `v-on:event="handle"` |
| vBind | `v-bind=""` |
| vModel | `v-model="something"` |
| vPre | `v-pre` |
| vCloak | `v-cloak` |
| vOnce | `v-once` |
| key | `:key="key"` |
| ref | `ref="reference"` |
| slotA | `slot=""` |
| slotE | `slot` |
| slotScope | `slot-scope=""` |
| scope | `scope="this api replaced by slot-scope in 2.5.0+"` |
| component | `component(:is="componentId")` |
| keepAlive | `keep-alive` |
| transition | `transition` |
| transitionGroup | `transition-group` |
| enterClass | `enter-class=""` |
| leaveClass | `leave-class=""` |
| appearClass | `appear-class=""` |
| enterToClass | `enter-to-class=""` |
| leaveToClass | `leave-to-class=""` |
| appearToClass | `appear-to-class=""` |
| enterActiveClass | `enter-active-class=""` |
| leaveActiveClass | `leave-active-class=""` |
| appearActiveClass | `appear-active-class=""` |
| beforeEnterEvent | `@before-enter=""` |
| beforeLeaveEvent | `@before-leave=""` |
| beforeAppearEvent | `@before-appear=""` |
| enterEvent | `@enter=""` |
| leaveEvent | `@leave=""` |
| appearEvent | `@appear=""` |
| afterEnterEvent | `@after-enter=""` |
| afterLeaveEvent | `@after-leave=""` |
| afterAppearEvent | `@after-appear=""` |
| enterCancelledEvent | `@enter-cancelled=""` |
| leaveCancelledEvent | `@leave-cancelled=""` |
| appearCancelledEvent | `@appear-cancelled=""` |
| routerLink | `router-link` |
| routerLinkTo | `router-link (to="")` |
| to | `to=""` |
| tag | `tag=""` |
| routerView | `router-view` |

## 四、vue.json（Vue组件骨架）

| 快捷命令         | 生成代码                                                                                                                                    |
| ------------ | --------------------------------------------------------------------------------------------------------------------------------------- |
| VueInit      | `<template lang="">\n\t<div>\n\t\t\n\t</div>\n</template>\n<script>\nexport default {\n\t\n}\n</script>\n<style lang="">\n\t\n</style>` |
| templateLang | `<template lang="">\n\t<div>\n\t\t\n\t</div>\n</template>`                                                                              |
| script       | `<script>\nexport default {\n\t\n}\n</script>`                                                                                          |
| styleLang    | `<style lang="">\n\t\n</style>`                                                                                                         |

要不要我把这些**整理成一份精简版速查表**，只保留高频常用的，方便你快速记忆？