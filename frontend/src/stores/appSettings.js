import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const STORAGE_KEY = 'adha_vea_settings'

function loadSaved() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  } catch {
    return {}
  }
}

function mixHex(hex, targetHex, weight) {
  const clean = (h) => h.replace('#', '')
  const c1 = clean(hex)
  const c2 = clean(targetHex)
  if (c1.length !== 6 || c2.length !== 6) return hex
  const r = Math.round(parseInt(c1.slice(0, 2), 16) * (1 - weight) + parseInt(c2.slice(0, 2), 16) * weight)
  const g = Math.round(parseInt(c1.slice(2, 4), 16) * (1 - weight) + parseInt(c2.slice(2, 4), 16) * weight)
  const b = Math.round(parseInt(c1.slice(4, 6), 16) * (1 - weight) + parseInt(c2.slice(4, 6), 16) * weight)
  return '#' + [r, g, b].map((v) => v.toString(16).padStart(2, '0')).join('')
}

export const useAppSettingsStore = defineStore('appSettings', () => {
  const saved = loadSaved()

  const theme = ref(saved.theme || '#409EFF')
  const showTagsView = ref(saved.showTagsView !== undefined ? saved.showTagsView : true)
  const fixedHeader = ref(saved.fixedHeader !== undefined ? saved.fixedHeader : true)
  const sidebarLogo = ref(saved.sidebarLogo !== undefined ? saved.sidebarLogo : true)
  const sidebarTheme = ref(saved.sidebarTheme || 'dark') // 'dark' (#304156) or 'light' (#ffffff)
  const elementSize = ref(saved.elementSize || 'default') // 'large' | 'default' | 'small'
  const sidebarCollapsed = ref(saved.sidebarCollapsed === true)

  function applyThemeVariables(color) {
    if (typeof document === 'undefined' || !color) return
    const root = document.documentElement
    root.style.setProperty('--vea-theme', color)
    root.style.setProperty('--vea-theme-light-3', mixHex(color, '#ffffff', 0.3))
    root.style.setProperty('--vea-theme-light-5', mixHex(color, '#ffffff', 0.5))
    root.style.setProperty('--vea-theme-light-7', mixHex(color, '#ffffff', 0.7))
    root.style.setProperty('--vea-theme-light-8', mixHex(color, '#ffffff', 0.8))
    root.style.setProperty('--vea-theme-light-9', mixHex(color, '#ffffff', 0.9))
    root.style.setProperty('--vea-theme-dark-2', mixHex(color, '#000000', 0.18))
  }

  function persist() {
    try {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          theme: theme.value,
          showTagsView: showTagsView.value,
          fixedHeader: fixedHeader.value,
          sidebarLogo: sidebarLogo.value,
          sidebarTheme: sidebarTheme.value,
          elementSize: elementSize.value,
          sidebarCollapsed: sidebarCollapsed.value
        })
      )
    } catch {
      // ignore
    }
  }

  function setTheme(color) {
    theme.value = color || '#409EFF'
    applyThemeVariables(theme.value)
    persist()
  }

  function setElementSize(size) {
    elementSize.value = size
    persist()
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    persist()
  }

  function resetDefaults() {
    theme.value = '#409EFF'
    showTagsView.value = true
    fixedHeader.value = true
    sidebarLogo.value = true
    sidebarTheme.value = 'dark'
    elementSize.value = 'default'
    sidebarCollapsed.value = false
    applyThemeVariables(theme.value)
    persist()
  }

  watch([theme, showTagsView, fixedHeader, sidebarLogo, sidebarTheme, elementSize, sidebarCollapsed], () => {
    applyThemeVariables(theme.value)
    persist()
  }, { immediate: true })

  return {
    theme,
    showTagsView,
    fixedHeader,
    sidebarLogo,
    sidebarTheme,
    elementSize,
    sidebarCollapsed,
    setTheme,
    setElementSize,
    toggleSidebar,
    applyThemeVariables,
    resetDefaults
  }
})
