import { hasPermi } from '@/utils/permission'

export default {
  mounted(el, binding) {
    const { value } = binding
    if (value && !hasPermi(value)) {
      // 无权限：直接移除 DOM（比 v-if 更彻底）
      if (el.parentNode) {
        el.parentNode.removeChild(el)
      }
    }
  }
}
