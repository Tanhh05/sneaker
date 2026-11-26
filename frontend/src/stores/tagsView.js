import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useTagsViewStore = defineStore('tagsView', () => {
  const visitedViews = ref([
    {
      path: '/admin/dashboard',
      fullPath: '/admin/dashboard',
      name: 'Dashboard',
      title: 'Dashboard Tổng quan',
      affix: true
    }
  ])

  function addView(route) {
    if (!route || !route.path || route.path === '/admin/login') return
    const exists = visitedViews.value.some((v) => v.path === route.path)
    if (exists) {
      // Update fullPath/title if query changed
      const idx = visitedViews.value.findIndex((v) => v.path === route.path)
      if (idx !== -1) {
        visitedViews.value[idx] = {
          ...visitedViews.value[idx],
          fullPath: route.fullPath,
          title: route.meta?.title || visitedViews.value[idx].title
        }
      }
      return
    }
    visitedViews.value.push({
      path: route.path,
      fullPath: route.fullPath,
      name: route.name,
      title: route.meta?.title || route.name || 'Trang quản trị',
      affix: route.path === '/admin/dashboard'
    })
  }

  function delView(view) {
    const idx = visitedViews.value.findIndex((v) => v.path === view.path)
    if (idx !== -1 && !visitedViews.value[idx].affix) {
      visitedViews.value.splice(idx, 1)
    }
    return [...visitedViews.value]
  }

  function delOthersViews(view) {
    visitedViews.value = visitedViews.value.filter((v) => v.affix || v.path === view.path)
    return [...visitedViews.value]
  }

  function delAllViews() {
    visitedViews.value = visitedViews.value.filter((v) => v.affix)
    return [...visitedViews.value]
  }

  return {
    visitedViews,
    addView,
    delView,
    delOthersViews,
    delAllViews
  }
})
