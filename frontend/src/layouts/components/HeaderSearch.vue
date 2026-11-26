<template>
  <div :class="{ show: isOpen }" class="header-search">
    <button
      type="button"
      class="search-trigger-btn"
      title="Tìm nhanh chức năng quản trị"
      @click.stop="toggleOpen"
    >
      <el-icon :size="17"><Search /></el-icon>
    </button>

    <el-select
      ref="selectRef"
      v-model="selectedPath"
      filterable
      placeholder="Tìm nhanh trang quản trị..."
      class="header-search-select"
      size="small"
      @change="onSelectRoute"
      @blur="closeSearch"
    >
      <el-option
        v-for="item in searchableItems"
        :key="item.path"
        :label="`${item.groupTitle} > ${item.title}`"
        :value="item.path"
      />
    </el-select>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { MENU_GROUPS } from '@/constants/navigation'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const isOpen = ref(false)
const selectedPath = ref('')
const selectRef = ref(null)

const searchableItems = computed(() => {
  const list = []
  for (const g of MENU_GROUPS) {
    for (const item of g.items) {
      if (authStore.hasAnyRole(item.roles)) {
        list.push({
          groupTitle: g.groupTitle,
          title: item.title,
          path: item.path
        })
      }
    }
  }
  return list
})

function toggleOpen() {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    nextTick(() => {
      selectRef.value?.focus()
    })
  }
}

function closeSearch() {
  isOpen.value = false
  selectedPath.value = ''
}

function onSelectRoute(path) {
  if (path) {
    router.push(path)
  }
  closeSearch()
}
</script>

<style scoped lang="scss">
.header-search {
  display: inline-flex;
  align-items: center;

  .search-trigger-btn {
    background: transparent;
    border: none;
    width: 34px;
    height: 34px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #5a5e66;
    border-radius: 4px;
    transition: background 0.15s ease, color 0.15s ease;

    &:hover {
      background: rgba(0, 0, 0, 0.04);
      color: var(--vea-theme, #409eff);
    }
  }

  .header-search-select {
    width: 0;
    overflow: hidden;
    transition: width 0.22s ease;

    :deep(.el-select__wrapper) {
      border: none !important;
      border-bottom: 1px solid #d9d9d9 !important;
      border-radius: 0 !important;
      background: transparent !important;
      box-shadow: none !important;
    }
  }

  &.show {
    .header-search-select {
      width: 210px;
      margin-left: 6px;
    }
  }
}
</style>
