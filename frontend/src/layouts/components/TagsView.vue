<template>
  <div id="tags-view-container" class="tags-view-container">
    <el-scrollbar class="tags-view-wrapper">
      <div class="tags-view-track">
        <router-link
          v-for="tag in visitedViews"
          :key="tag.path"
          :to="{ path: tag.path }"
          class="tags-view-item"
          :class="{ active: isActive(tag) }"
          @click.middle="!tag.affix && closeSelectedTag(tag)"
          @contextmenu.prevent="openMenu(tag, $event)"
        >
          <span class="tag-title">{{ tag.title }}</span>
          <span
            v-if="!tag.affix"
            class="tag-close-icon"
            @click.prevent.stop="closeSelectedTag(tag)"
          >
            &times;
          </span>
        </router-link>
      </div>
    </el-scrollbar>

    <ul
      v-show="contextVisible"
      :style="{ left: contextLeft + 'px', top: contextTop + 'px' }"
      class="contextmenu"
    >
      <li @click="refreshSelectedTag(selectedTag)">Làm mới trang</li>
      <li v-if="selectedTag && !selectedTag.affix" @click="closeSelectedTag(selectedTag)">Đóng tab này</li>
      <li @click="closeOthersTags">Đóng các tab khác</li>
      <li @click="closeAllTags">Đóng tất cả</li>
    </ul>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTagsViewStore } from '@/stores/tagsView'

const emit = defineEmits(['refresh'])

const route = useRoute()
const router = useRouter()
const tagsStore = useTagsViewStore()

const visitedViews = computed(() => tagsStore.visitedViews)
const contextVisible = ref(false)
const contextTop = ref(0)
const contextLeft = ref(0)
const selectedTag = ref(null)

function isActive(tag) {
  return tag.path === route.path
}

function addCurrentTag() {
  if (route.path.startsWith('/admin') || route.path.startsWith('/pos')) {
    tagsStore.addView(route)
  }
}

watch(
  () => route.fullPath,
  () => {
    addCurrentTag()
  },
  { immediate: true }
)

function closeSelectedTag(view) {
  const remaining = tagsStore.delView(view)
  if (isActive(view)) {
    const latest = remaining[remaining.length - 1]
    router.push(latest ? latest.fullPath : '/admin/dashboard')
  }
}

function closeOthersTags() {
  if (!selectedTag.value) return
  tagsStore.delOthersViews(selectedTag.value)
  if (!isActive(selectedTag.value)) {
    router.push(selectedTag.value.fullPath)
  }
}

function closeAllTags() {
  const remaining = tagsStore.delAllViews()
  const latest = remaining[remaining.length - 1]
  router.push(latest ? latest.fullPath : '/admin/dashboard')
}

function refreshSelectedTag(view) {
  if (view && !isActive(view)) {
    router.push(view.fullPath)
  }
  emit('refresh')
}

function openMenu(tag, e) {
  const maxLeft = window.innerWidth - 150
  contextLeft.value = Math.min(e.clientX, maxLeft)
  contextTop.value = e.clientY
  selectedTag.value = tag
  contextVisible.value = true
}

function closeMenu() {
  contextVisible.value = false
}

watch(contextVisible, (val) => {
  if (val) {
    document.body.addEventListener('click', closeMenu)
  } else {
    document.body.removeEventListener('click', closeMenu)
  }
})

onMounted(() => {
  addCurrentTag()
})

onUnmounted(() => {
  document.body.removeEventListener('click', closeMenu)
})
</script>

<style scoped lang="scss">
.tags-view-container {
  height: 34px;
  width: 100%;
  background: #ffffff;
  border-bottom: 1px solid #d8dce5;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.08), 0 0 3px 0 rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
  position: relative;
  z-index: 9;

  .tags-view-wrapper {
    height: 34px;
    width: 100%;

    :deep(.el-scrollbar__view) {
      height: 100%;
    }
  }

  .tags-view-track {
    display: flex;
    align-items: center;
    height: 34px;
    padding: 0 14px;
    gap: 5px;
    white-space: nowrap;
  }

  .tags-view-item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    position: relative;
    cursor: pointer;
    height: 26px;
    line-height: 26px;
    border: 1px solid #d8dce5;
    color: #495060;
    background: #ffffff;
    padding: 0 9px;
    font-size: 12px;
    text-decoration: none;
    border-radius: 2px;
    transition: all 0.15s ease;
    flex-shrink: 0;

    &:hover {
      color: var(--vea-theme, #409eff);
      border-color: var(--vea-theme-light-7, #c6e2ff);
    }

    &.active {
      background-color: var(--vea-theme, #42b983);
      color: #ffffff;
      border-color: var(--vea-theme, #42b983);

      &::before {
        content: '';
        background: #ffffff;
        display: inline-block;
        width: 7px;
        height: 7px;
        border-radius: 50%;
      }
    }

    .tag-close-icon {
      width: 15px;
      height: 15px;
      border-radius: 50%;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      line-height: 1;
      transition: all 0.2s ease;

      &:hover {
        background-color: rgba(0, 0, 0, 0.18);
        color: #ffffff;
      }
    }
  }

  .contextmenu {
    margin: 0;
    background: #ffffff;
    z-index: 3000;
    position: fixed;
    list-style-type: none;
    padding: 5px 0;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 400;
    color: #333333;
    box-shadow: 2px 2px 10px 0 rgba(0, 0, 0, 0.18);
    border: 1px solid #e6ebf5;

    li {
      margin: 0;
      padding: 7px 16px;
      cursor: pointer;
      &:hover {
        background: #ecf5ff;
        color: var(--vea-theme, #409eff);
      }
    }
  }
}
</style>
