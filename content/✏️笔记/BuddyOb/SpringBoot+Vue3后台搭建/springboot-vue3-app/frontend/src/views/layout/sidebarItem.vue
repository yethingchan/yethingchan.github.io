<template>
  <el-menu-item v-if="!hasChildren" :index="resolvePath(item.path)">
    <el-icon v-if="iconName"><component :is="iconName" /></el-icon>
    <template #title>{{ item.meta && item.meta.title }}</template>
  </el-menu-item>

  <el-sub-menu v-else :index="resolvePath(item.path)">
    <template #title>
      <el-icon v-if="iconName"><component :is="iconName" /></el-icon>
      <span>{{ item.meta && item.meta.title }}</span>
    </template>
    <sidebar-item
      v-for="child in item.children"
      :key="child.path"
      :item="child"
      :base-path="resolvePath(item.path)"
    />
  </el-sub-menu>
</template>

<script setup>
import { computed } from 'vue'

defineOptions({ name: 'SidebarItem' })

const props = defineProps({
  item: { type: Object, required: true },
  basePath: { type: String, default: '' }
})

const hasChildren = computed(() => props.item.children && props.item.children.length > 0)

const iconName = computed(() => {
  const icon = props.item.meta && props.item.meta.icon
  return icon && icon !== '#' ? icon : ''
})

function resolvePath(p) {
  if (/^https?:\/\//.test(p)) return p
  if (p.startsWith('/')) return p
  const base = props.basePath ? props.basePath.replace(/\/$/, '') : ''
  return (base ? base + '/' : '') + p
}
</script>
