import { useUserStore } from '@/store/modules/user'

/** 前端按钮权限判断（真正防线是后端 @PreAuthorize） */
export function hasPermi(permission) {
  const userStore = useUserStore()
  if (!userStore.permissions) {
    return false
  }
  return userStore.permissions.includes(permission)
}
