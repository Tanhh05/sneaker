<template>
  <el-config-provider :size="settings.elementSize">
    <div
      class="admin-layout vea-app-wrapper"
      :class="{
        'hide-sidebar': isCollapsed && !isMobile,
        'mobile-mode': isMobile,
        'light-sidebar': settings.sidebarTheme === 'light'
      }"
    >
      <!-- Mobile Backdrop Overlay -->
      <transition name="fade-backdrop">
        <div
          v-if="isMobile && mobileDrawerOpen"
          class="mobile-sidebar-backdrop"
          @click="mobileDrawerOpen = false"
        />
      </transition>

      <!-- SIDEBAR (vue-element-admin #304156 / #ffffff + el-menu) -->
      <aside
        class="sidebar-container"
        :class="{
          'has-logo': settings.sidebarLogo,
          'mobile-open': mobileDrawerOpen,
          'is-light': settings.sidebarTheme === 'light'
        }"
      >
        <!-- Sidebar Logo (50px height, #2b2f3a in dark mode) -->
        <div v-if="settings.sidebarLogo" class="sidebar-logo-container" @click="goDashboard">
          <AdhaLogoMark :size="28" :variant="settings.sidebarTheme === 'light' ? 'dark' : 'light'" />
          <span v-if="!isCollapsed || isMobile" class="sidebar-title">ADHA Sneaker</span>
          <button
            v-if="isMobile"
            type="button"
            class="mobile-close-btn"
            @click.stop="mobileDrawerOpen = false"
            aria-label="Đóng menu"
          >
            &times;
          </button>
        </div>

        <!-- Element Plus Vertical Accordion Menu -->
        <el-scrollbar wrap-class="scrollbar-wrapper" class="sidebar-scroll">
          <el-menu
            :default-active="activeMenu"
            :collapse="isCollapsed && !isMobile"
            :background-color="sidebarColors.bg"
            :text-color="sidebarColors.text"
            :active-text-color="settings.theme"
            :collapse-transition="false"
            :default-openeds="defaultOpenGroups"
            router
            mode="vertical"
            class="vea-sidebar-menu"
            @select="onMenuSelect"
          >
            <!-- Direct top-level Dashboard item -->
            <el-menu-item index="/admin/dashboard">
              <el-icon><Odometer /></el-icon>
              <template #title>
                <span>Dashboard Tổng quan</span>
              </template>
            </el-menu-item>

            <!-- Collapsible Menu Sub-Groups -->
            <el-sub-menu
              v-for="group in subMenuGroups"
              :key="group.groupKey"
              :index="group.groupKey"
            >
              <template #title>
                <el-icon><component :is="group.icon" /></el-icon>
                <span>{{ group.shortTitle }}</span>
              </template>
              <el-menu-item
                v-for="item in group.items"
                :key="item.path"
                :index="item.path"
              >
                <el-icon><component :is="item.icon" /></el-icon>
                <template #title>
                  <span class="sub-item-label">{{ item.title }}</span>
                  <span v-if="item.badge" class="vea-menu-pill">{{ item.badge }}</span>
                </template>
              </el-menu-item>
            </el-sub-menu>
          </el-menu>
        </el-scrollbar>
      </aside>

      <!-- MAIN CONTAINER -->
      <div
        class="main-container"
        :class="{ 'has-tags-view': settings.showTagsView, 'fixed-header-mode': settings.fixedHeader }"
        :style="{ '--vea-header-h': settings.showTagsView ? '84px' : '50px' }"
      >
        <!-- TOP HEADER WRAPPER (Navbar 50px + TagsView 34px) -->
        <div class="header-stack" :class="{ 'is-fixed': settings.fixedHeader }">
          <!-- NAVBAR (50px height, crisp white, vue-element-admin style) -->
          <header class="vea-navbar">
            <div class="navbar-left">
              <!-- Animated Hamburger Toggle -->
              <div class="hamburger-container" @click="toggleSidebar" title="Thu gọn / Mở rộng Sidebar">
                <svg
                  :class="{ 'is-active': !(isCollapsed && !isMobile) && (!isMobile || mobileDrawerOpen) }"
                  class="hamburger-svg"
                  viewBox="0 0 1024 1024"
                  width="20"
                  height="20"
                >
                  <path d="M408 442h480c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8H408c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8zm-8 204c0 4.4 3.6 8 8 8h480c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8H408c-4.4 0-8 3.6-8 8v56zm504-486H120c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8zm0 632H120c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8zM142.4 642.1L298.7 519a8.84 8.84 0 0 0 0-13.9L142.4 381.9c-5.8-4.6-14.4-.5-14.4 6.9v246.3a8.9 8.9 0 0 0 14.4 7z" />
                </svg>
              </div>

              <!-- Animated Breadcrumb -->
              <el-breadcrumb separator="/" class="app-breadcrumb">
                <transition-group name="breadcrumb">
                  <el-breadcrumb-item key="root" :to="{ path: '/admin/dashboard' }">
                    Dashboard
                  </el-breadcrumb-item>
                  <el-breadcrumb-item
                    v-if="$route.path !== '/admin/dashboard'"
                    :key="$route.path"
                  >
                    <span class="no-redirect">{{ $route.meta?.title || 'Quản trị' }}</span>
                  </el-breadcrumb-item>
                </transition-group>
              </el-breadcrumb>
            </div>

            <!-- Right Menu Utilities (vue-element-admin exact toolbar) -->
            <div class="right-menu">
              <!-- Quick Route Search -->
              <HeaderSearch class="right-menu-item hide-xs" />

              <!-- Fullscreen Toggle -->
              <button
                type="button"
                class="right-menu-item icon-tool-btn hide-xs"
                :title="isFullscreen ? 'Thoát toàn màn hình' : 'Toàn màn hình'"
                @click="toggleFullscreen"
              >
                <el-icon :size="17"><FullScreen /></el-icon>
              </button>

              <!-- Global Element Size Selector -->
              <el-dropdown trigger="click" class="right-menu-item hide-xs" @command="handleSetSize">
                <button type="button" class="icon-tool-btn" title="Cỡ chữ & mật độ hiển thị">
                  <el-icon :size="17"><Rank /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="large" :disabled="settings.elementSize === 'large'">
                      Lớn (Large)
                    </el-dropdown-item>
                    <el-dropdown-item command="default" :disabled="settings.elementSize === 'default'">
                      Mặc định (Default)
                    </el-dropdown-item>
                    <el-dropdown-item command="small" :disabled="settings.elementSize === 'small'">
                      Nhỏ gọn (Small)
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <!-- Quick Store & POS Buttons -->
              <router-link to="/" class="right-pill-btn store-btn" title="Xem trang bán hàng">
                <el-icon><Shop /></el-icon>
                <span class="pill-text">Website</span>
              </router-link>

              <button
                type="button"
                class="right-pill-btn pos-btn"
                :class="{ active: $route.path.startsWith('/pos') }"
                title="Bán hàng tại quầy POS"
                @click="$router.push('/pos')"
              >
                <el-icon><ShoppingCart /></el-icon>
                <span class="pill-text">POS</span>
              </button>

              <!-- User Avatar Dropdown -->
              <el-dropdown trigger="click" placement="bottom-end" class="avatar-container" @command="handleCommand">
                <div class="avatar-wrapper">
                  <div class="user-avatar" :style="{ backgroundColor: settings.theme }">
                    {{ (authStore.user?.fullName || authStore.user?.username || 'A').charAt(0).toUpperCase() }}
                  </div>
                  <div class="avatar-meta hide-sm">
                    <span class="avatar-name">{{ authStore.user?.fullName || authStore.user?.username }}</span>
                    <span class="avatar-role">{{ (authStore.roles || []).join(' · ') }}</span>
                  </div>
                  <el-icon class="caret-icon"><CaretBottom /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu class="user-dropdown">
                    <div class="dropdown-user-header">
                      <strong>{{ authStore.user?.fullName || authStore.user?.username }}</strong>
                      <span>@{{ authStore.user?.username }}</span>
                    </div>
                    <el-dropdown-item command="profile">
                      <el-icon><User /></el-icon> Thông tin cá nhân
                    </el-dropdown-item>
                    <el-dropdown-item command="password">
                      <el-icon><Key /></el-icon> Đổi mật khẩu
                    </el-dropdown-item>
                    <el-dropdown-item divided command="logout">
                      <el-icon><SwitchButton /></el-icon> Đăng xuất
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </header>

          <!-- TAGS-VIEW MULTI-TAB BAR (34px) -->
          <TagsView v-if="settings.showTagsView" @refresh="handleRefreshView" />
        </div>

        <!-- APP MAIN CONTENT AREA -->
        <main class="app-main">
          <router-view v-slot="{ Component }">
            <transition name="fade-transform" mode="out-in">
              <component :is="Component" :key="`${$route.fullPath}-${viewRefreshKey}`" />
            </transition>
          </router-view>
        </main>
      </div>

      <!-- Floating Theme & Layout Settings Drawer -->
      <SettingsDrawer />

      <!-- User Profile Modal -->
      <UserProfileDialog v-model="profileDialogVisible" />

      <!-- Change Password Modal -->
      <ChangePasswordDialog v-model="pwdDialogVisible" />
    </div>
  </el-config-provider>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Odometer,
  ShoppingCart,
  Shop,
  FullScreen,
  Rank,
  CaretBottom,
  User,
  Key,
  SwitchButton,
  Goods,
  Box,
  DataAnalysis,
  Tickets
} from '@element-plus/icons-vue'
import AdhaLogoMark from '@/components/common/AdhaLogoMark.vue'
import { useAuthStore } from '@/stores/auth'
import { useAppSettingsStore } from '@/stores/appSettings'
import { MENU_GROUPS } from '@/constants/navigation'
import TagsView from './components/TagsView.vue'
import HeaderSearch from './components/HeaderSearch.vue'
import SettingsDrawer from './components/SettingsDrawer.vue'
import ChangePasswordDialog from './components/ChangePasswordDialog.vue'
import UserProfileDialog from './components/UserProfileDialog.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const settings = useAppSettingsStore()

const isMobile = ref(typeof window !== 'undefined' ? window.innerWidth <= 992 : false)
const mobileDrawerOpen = ref(false)
const isFullscreen = ref(false)
const viewRefreshKey = ref(0)
const profileDialogVisible = ref(false)
const pwdDialogVisible = ref(false)

const isCollapsed = computed(() => settings.sidebarCollapsed)

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/admin/products')) return '/admin/products'
  if (path.startsWith('/admin/variants')) return '/admin/variants'
  if (path.startsWith('/admin/orders')) return '/admin/orders'
  if (path.startsWith('/admin/invoices')) return '/admin/invoices'
  if (path.startsWith('/admin/vouchers')) return '/admin/vouchers'
  return path
})

const sidebarColors = computed(() => {
  if (settings.sidebarTheme === 'light') {
    return {
      bg: '#ffffff',
      text: '#303133'
    }
  }
  return {
    bg: '#304156',
    text: '#bfcbd9'
  }
})

const groupMetaMap = [
  { key: 'sales', shortTitle: 'Bán hàng', icon: Tickets },
  { key: 'catalog', shortTitle: 'Sản phẩm & Kho', icon: Goods },
  { key: 'partners', shortTitle: 'Nhập hàng & Đối tác', icon: Box },
  { key: 'system', shortTitle: 'Nhân sự & Báo cáo', icon: DataAnalysis }
]

const subMenuGroups = computed(() => {
  return MENU_GROUPS.map((g, idx) => {
    const meta = groupMetaMap[idx] || { key: `grp-${idx}`, shortTitle: g.groupTitle, icon: Goods }
    const items = g.items.filter(
      (i) => i.path !== '/admin/dashboard' && authStore.hasAnyRole(i.roles)
    )
    return {
      groupKey: meta.key,
      shortTitle: meta.shortTitle,
      icon: meta.icon,
      items
    }
  }).filter((g) => g.items.length > 0)
})

const defaultOpenGroups = computed(() => subMenuGroups.value.map((g) => g.groupKey))

function handleResize() {
  const mobile = window.innerWidth <= 992
  isMobile.value = mobile
  if (!mobile) {
    mobileDrawerOpen.value = false
  }
}

function toggleSidebar() {
  if (isMobile.value) {
    mobileDrawerOpen.value = !mobileDrawerOpen.value
  } else {
    settings.toggleSidebar()
  }
}

function onMenuSelect() {
  if (isMobile.value) {
    mobileDrawerOpen.value = false
  }
}

function goDashboard() {
  mobileDrawerOpen.value = false
  router.push('/admin/dashboard')
}

function handleRefreshView() {
  viewRefreshKey.value += 1
}

function handleSetSize(size) {
  settings.setElementSize(size)
  ElMessage.success(`Đã chuyển mật độ hiển thị sang ${size === 'large' ? 'Lớn' : size === 'small' ? 'Nhỏ gọn' : 'Mặc định'}`)
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen?.().then(() => {
      isFullscreen.value = true
    }).catch(() => {})
  } else {
    document.exitFullscreen?.().then(() => {
      isFullscreen.value = false
    }).catch(() => {})
  }
}

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

function handleCommand(cmd) {
  if (cmd === 'profile') {
    profileDialogVisible.value = true
  } else if (cmd === 'password') {
    pwdDialogVisible.value = true
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('Bạn có chắc chắn muốn đăng xuất khỏi hệ thống quản lý?', 'Xác nhận đăng xuất', {
      confirmButtonText: 'Đăng xuất',
      cancelButtonText: 'Hủy',
      type: 'warning'
    }).then(async () => {
      await authStore.logout()
      ElMessage.success('Đã đăng xuất khỏi cổng quản trị')
      router.push('/admin/login')
    }).catch(() => {})
  }
}

watch(
  () => route.fullPath,
  () => {
    mobileDrawerOpen.value = false
  }
)

onMounted(() => {
  document.body.classList.add('vea-admin-mode')
  settings.applyThemeVariables(settings.theme)
  handleResize()
  window.addEventListener('resize', handleResize)
  document.addEventListener('fullscreenchange', onFullscreenChange)
})

onUnmounted(() => {
  document.body.classList.remove('vea-admin-mode')
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
})
</script>

<style scoped lang="scss">
.vea-app-wrapper {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background-color: #f0f2f5;
}

/* =========================================================
   SIDEBAR (Exact PanJiaChen/vue-element-admin #304156)
   ========================================================= */
.sidebar-container {
  width: 240px;
  height: 100%;
  background-color: #304156;
  transition: width 0.28s ease;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  z-index: 1001;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.25);

  &.is-light {
    background-color: #ffffff;
    border-right: 1px solid #e6ebf5;
    box-shadow: 2px 0 8px rgba(0, 21, 41, 0.05);

    .sidebar-logo-container {
      background-color: #ffffff;
      border-bottom: 1px solid #e6ebf5;
      .sidebar-title {
        color: #303133;
      }
    }
  }

  .sidebar-logo-container {
    height: 50px;
    background: #2b2f3a;
    padding: 0 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    cursor: pointer;
    flex-shrink: 0;
    overflow: hidden;

    .sidebar-title {
      color: #ffffff;
      font-weight: 600;
      font-size: 14px;
      letter-spacing: 0.3px;
      white-space: nowrap;
    }
  }

  .sidebar-scroll {
    flex: 1;
    overflow: hidden;
  }

  :deep(.el-menu) {
    border-right: none !important;
    width: 100% !important;
  }

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 46px;
    line-height: 46px;
    font-size: 13.5px;
    padding-right: 32px !important;

    &:hover {
      background-color: #263445 !important;
    }
  }

  :deep(.el-sub-menu .el-menu-item) {
    background-color: #1f2d3d !important;
    min-width: 240px;
    height: 44px;
    line-height: 44px;
    font-size: 13px;
    padding-left: 42px !important;
    padding-right: 14px !important;

    &:hover {
      background-color: #001528 !important;
    }

    &.is-active {
      background-color: #18222f !important;
      font-weight: 600;
    }
  }

  &.is-light {
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      &:hover {
        background-color: #ecf5ff !important;
      }
    }
    :deep(.el-sub-menu .el-menu-item) {
      background-color: #fafafa !important;
      &:hover {
        background-color: #ecf5ff !important;
      }
      &.is-active {
        background-color: var(--vea-theme-light-9, #ecf5ff) !important;
      }
    }
  }

  .sub-item-label {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .vea-menu-pill {
    margin-left: 6px;
    font-size: 10px;
    font-weight: 700;
    padding: 1px 6px;
    border-radius: 10px;
    background: var(--vea-theme, #409eff);
    color: #ffffff;
    line-height: 1.5;
  }
}

.hide-sidebar .sidebar-container {
  width: 54px;
}

/* =========================================================
   MAIN CONTAINER + NAVBAR (50px) + TAGSVIEW (34px)
   ========================================================= */
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background-color: #f0f2f5;
}

.header-stack {
  flex-shrink: 0;
  z-index: 10;
}

.vea-navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 14px;

  .navbar-left {
    display: flex;
    align-items: center;
    height: 100%;
    min-width: 0;
  }

  .hamburger-container {
    height: 50px;
    padding: 0 15px;
    display: flex;
    align-items: center;
    cursor: pointer;
    transition: background 0.25s;

    &:hover {
      background: rgba(0, 0, 0, 0.03);
    }

    .hamburger-svg {
      fill: #5a5e66;
      transition: transform 0.25s ease;
      transform: rotate(0deg);

      &.is-active {
        transform: rotate(180deg);
      }
    }
  }

  .app-breadcrumb {
    margin-left: 6px;
    font-size: 13.5px;
    line-height: 50px;

    .no-redirect {
      color: #97a8be;
      cursor: text;
    }
  }

  .right-menu {
    display: flex;
    align-items: center;
    height: 100%;
    gap: 6px;

    .icon-tool-btn {
      width: 34px;
      height: 34px;
      border: none;
      background: transparent;
      color: #5a5e66;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      border-radius: 4px;
      transition: background 0.15s ease, color 0.15s ease;

      &:hover {
        background: rgba(0, 0, 0, 0.04);
        color: var(--vea-theme, #409eff);
      }
    }

    .right-pill-btn {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      height: 30px;
      padding: 0 11px;
      font-size: 12.5px;
      font-weight: 500;
      border-radius: 4px;
      text-decoration: none;
      cursor: pointer;
      transition: all 0.15s ease;

      &.store-btn {
        border: 1px solid #dcdfe6;
        background: #ffffff;
        color: #606266;
        &:hover {
          color: var(--vea-theme, #409eff);
          border-color: var(--vea-theme-light-7, #c6e2ff);
          background: var(--vea-theme-light-9, #ecf5ff);
        }
      }

      &.pos-btn {
        border: 1px solid var(--vea-theme, #409eff);
        background: var(--vea-theme, #409eff);
        color: #ffffff;
        &:hover {
          opacity: 0.9;
        }
      }
    }

    .avatar-container {
      margin-left: 6px;

      .avatar-wrapper {
        display: flex;
        align-items: center;
        gap: 8px;
        cursor: pointer;
        padding: 4px 6px;
        border-radius: 4px;
        transition: background 0.15s ease;

        &:hover {
          background: rgba(0, 0, 0, 0.03);
        }

        .user-avatar {
          width: 32px;
          height: 32px;
          border-radius: 6px;
          color: #ffffff;
          font-weight: 700;
          font-size: 13px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .avatar-meta {
          display: flex;
          flex-direction: column;
          line-height: 1.2;

          .avatar-name {
            font-size: 12.5px;
            font-weight: 600;
            color: #303133;
          }
          .avatar-role {
            font-size: 10px;
            color: #909399;
          }
        }

        .caret-icon {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }
}

.dropdown-user-header {
  padding: 8px 16px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  strong {
    font-size: 13px;
    color: #303133;
  }
  span {
    font-size: 11.5px;
    color: #909399;
  }
}

.app-main {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  background-color: #f0f2f5;
  position: relative;
}

/* =========================================================
   MOBILE RESPONSIVE
   ========================================================= */
.mobile-sidebar-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 1000;
}

.mobile-close-btn {
  margin-left: auto;
  background: transparent;
  border: none;
  color: #bfcbd9;
  font-size: 20px;
  cursor: pointer;
}

.mobile-mode {
  .sidebar-container {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 256px !important;
    z-index: 1002;
    transform: translateX(-100%);
    transition: transform 0.28s ease;

    &.mobile-open {
      transform: translateX(0);
    }
  }
}

@media (max-width: 768px) {
  .hide-xs {
    display: none !important;
  }

  .vea-navbar {
    padding-right: 8px;

    .navbar-left {
      flex: 1;
      min-width: 0;
      overflow: hidden;
    }

    .hamburger-container {
      padding: 0 12px;
      flex-shrink: 0;
    }

    .app-breadcrumb {
      margin-left: 2px;
      font-size: 13px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      min-width: 0;
    }

    .right-menu {
      gap: 4px;
      flex-shrink: 0;

      .right-pill-btn {
        padding: 0 8px;
        height: 28px;

        .pill-text {
          display: none;
        }
      }

      .avatar-container {
        margin-left: 2px;

        .avatar-wrapper {
          padding: 2px 4px;
          gap: 4px;
        }
      }
    }
  }
}

@media (max-width: 600px) {
  .hide-sm {
    display: none !important;
  }

  .vea-navbar .app-breadcrumb :deep(.el-breadcrumb__item:first-child:not(:last-child)) {
    display: none;
  }
}
</style>
