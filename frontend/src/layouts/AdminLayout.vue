<template>
  <div class="admin-layout">
    <!-- Mobile Backdrop Overlay -->
    <transition name="fade-backdrop">
      <div
        v-if="mobileDrawerOpen"
        class="mobile-sidebar-backdrop"
        @click="mobileDrawerOpen = false"
      />
    </transition>

    <!-- Sidebar -->
    <aside
      class="sidebar"
      :class="{ collapsed: isCollapsed && !isMobile, 'mobile-open': mobileDrawerOpen }"
    >
      <!-- Brand Logo -->
      <div class="brand-logo" @click="goDashboard">
        <AdhaLogoMark :size="34" variant="light" />
        <div v-if="!isCollapsed || isMobile" class="logo-text">
          <span class="title">ADHA SNEAKER</span>
          <span class="subtitle">MANAGEMENT SYSTEM</span>
        </div>
        <button
          v-if="isMobile"
          type="button"
          class="mobile-close-btn"
          @click.stop="mobileDrawerOpen = false"
          aria-label="Đóng menu"
        >
          ✕
        </button>
      </div>

      <!-- Navigation Menu -->
      <el-scrollbar class="menu-scroll">
        <nav class="nav-menu">
          <div
            v-for="group in visibleMenuGroups"
            :key="group.groupTitle"
            class="menu-group"
          >
            <div v-if="!isCollapsed || isMobile" class="group-label">{{ group.groupTitle }}</div>
            <router-link
              v-for="item in group.items"
              :key="item.path"
              :to="item.path"
              class="menu-item"
              :class="{ active: $route.path.startsWith(item.path) }"
              @click="mobileDrawerOpen = false"
            >
              <el-icon class="menu-icon"><component :is="item.icon" /></el-icon>
              <span v-if="!isCollapsed || isMobile" class="menu-title">{{ item.title }}</span>
              <span v-if="(!isCollapsed || isMobile) && item.badge" class="menu-badge">{{ item.badge }}</span>
            </router-link>
          </div>
        </nav>
      </el-scrollbar>
    </aside>

    <!-- Main Content Area -->
    <div class="main-wrapper">
      <!-- Top Header -->
      <header class="top-header">
        <div class="header-left">
          <button
            class="collapse-btn"
            @click="toggleSidebar"
            title="Mở / Thu gọn menu"
          >
            <el-icon :size="18">
              <Expand v-if="(isMobile && !mobileDrawerOpen) || (!isMobile && isCollapsed)" />
              <Fold v-else />
            </el-icon>
          </button>

          <el-breadcrumb separator="/" class="admin-breadcrumb">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }" class="crumb-root">QUẢN TRỊ</el-breadcrumb-item>
            <el-breadcrumb-item class="crumb-current">{{ $route.meta?.title ? $route.meta.title.toUpperCase() : 'TỔNG QUAN' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- Quick link to storefront -->
          <router-link to="/" class="header-action-btn btn-store-view" title="Xem Website">
            <el-icon><Shop /></el-icon>
            <span class="btn-label">XEM WEBSITE</span>
          </router-link>

          <!-- POS Quick Button -->
          <button
            class="header-action-btn btn-pos"
            @click="$router.push('/pos')"
            title="Bán hàng POS"
          >
            <el-icon><ShoppingCart /></el-icon>
            <span class="btn-label">BÁN HÀNG POS</span>
          </button>

          <!-- Roles Badge -->
          <div class="role-badges">
            <span
              v-for="r in authStore.roles"
              :key="r"
              class="role-pill"
              :class="r.toLowerCase()"
            >
              {{ r }}
            </span>
          </div>

          <!-- User Profile Dropdown -->
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-profile">
              <div class="avatar">{{ (authStore.user?.fullName || 'U').charAt(0).toUpperCase() }}</div>
              <div class="user-info">
                <span class="name">{{ authStore.user?.fullName || authStore.user?.username }}</span>
                <span class="username">@{{ authStore.user?.username }}</span>
              </div>
              <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="user-dropdown-menu">
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> Thông tin tài khoản
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Key /></el-icon> Đổi mật khẩu
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon> Đăng xuất hệ thống
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- Main Content Route View -->
      <main class="content-area">
        <router-view />
      </main>
    </div>

    <!-- User Profile Modal -->
    <UserProfileDialog v-model="profileDialogVisible" />

    <!-- Change Password Modal -->
    <ChangePasswordDialog v-model="pwdDialogVisible" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AdhaLogoMark from '@/components/common/AdhaLogoMark.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ShoppingCart,
  Shop,
  Fold,
  Expand,
  ArrowDown,
  User,
  Key,
  SwitchButton
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { MENU_GROUPS } from '@/constants/navigation'
import ChangePasswordDialog from './components/ChangePasswordDialog.vue'
import UserProfileDialog from './components/UserProfileDialog.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const isCollapsed = ref(false)
const isMobile = ref(typeof window !== 'undefined' ? window.innerWidth <= 1024 : false)
const mobileDrawerOpen = ref(false)
const profileDialogVisible = ref(false)
const pwdDialogVisible = ref(false)

function handleResize() {
  const mobile = window.innerWidth <= 1024
  isMobile.value = mobile
  if (!mobile) {
    mobileDrawerOpen.value = false
  }
}

function toggleSidebar() {
  if (isMobile.value) {
    mobileDrawerOpen.value = !mobileDrawerOpen.value
  } else {
    isCollapsed.value = !isCollapsed.value
  }
}

function goDashboard() {
  mobileDrawerOpen.value = false
  router.push('/admin/dashboard')
}

watch(
  () => route.fullPath,
  () => {
    mobileDrawerOpen.value = false
  }
)

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

const visibleMenuGroups = computed(() => {
  return MENU_GROUPS
    .map((g) => ({
      ...g,
      items: g.items.filter((i) => authStore.hasAnyRole(i.roles))
    }))
    .filter((g) => g.items.length > 0)
})

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
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background-color: #fafafa;
}

/* Sidebar */
.sidebar {
  width: 250px;
  background: #000000;
  color: #ffffff;
  display: flex;
  flex-direction: column;
  transition: width 0.22s cubic-bezier(0.16, 1, 0.3, 1);
  border-right: 1px solid #1a1a1a;
  flex-shrink: 0;

  &.collapsed {
    width: 68px;
  }

  .brand-logo {
    height: 64px;
    padding: 0 18px;
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;
    border-bottom: 1px solid #1a1a1a;

    .logo-mark {
      width: 32px;
      height: 32px;
      background: #ffffff;
      color: #000000;
      font-weight: 900;
      font-size: 17px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 0;
      flex-shrink: 0;
      letter-spacing: -0.05em;
    }

    .logo-text {
      display: flex;
      flex-direction: column;
      line-height: 1.15;

      .title {
        font-size: 14px;
        font-weight: 900;
        letter-spacing: 0.08em;
        color: #ffffff;
      }

      .subtitle {
        font-size: 9px;
        font-weight: 700;
        color: #737373;
        letter-spacing: 0.12em;
        margin-top: 2px;
      }
    }
  }

  .menu-scroll {
    flex: 1;
    padding: 14px 10px;
  }

  .menu-group {
    margin-bottom: 22px;

    .group-label {
      font-size: 10px;
      font-weight: 800;
      color: #525252;
      padding: 6px 12px;
      letter-spacing: 0.12em;
      text-transform: uppercase;
    }
  }

  .menu-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    border-radius: 0;
    color: #888888;
    text-decoration: none;
    font-size: 12.5px;
    font-weight: 700;
    margin-bottom: 3px;
    border-left: 2px solid transparent;
    transition: all 0.15s ease;

    .menu-icon {
      font-size: 16px;
      flex-shrink: 0;
      color: #737373;
      transition: color 0.15s ease;
    }

    .menu-title {
      flex: 1;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      letter-spacing: 0.02em;
    }

    .menu-badge {
      background: #1a1a1a;
      color: #d4d4d4;
      font-size: 9px;
      font-weight: 800;
      padding: 1px 6px;
      border-radius: 0;
    }

    &:hover {
      background: #111111;
      color: #ffffff;
      .menu-icon {
        color: #ffffff;
      }
    }

    &.active {
      background: #171717;
      color: #ffffff;
      font-weight: 800;
      border-left-color: #ffffff;

      .menu-icon {
        color: #ffffff;
      }

      .menu-badge {
        background: #ffffff;
        color: #000000;
      }
    }
  }

  .sidebar-footer {
    padding: 14px 16px;
    border-top: 1px solid #1a1a1a;
    background: #000000;

    .db-status {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 11px;
      color: #737373;

      .dot {
        width: 6px;
        height: 6px;
        background: #10b981;
      }

      .db-name {
        font-family: var(--font-mono);
        font-weight: 700;
        letter-spacing: 0.04em;
      }
    }
  }
}

/* Main Area */
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

/* Header */
.top-header {
  height: 64px;
  background: #ffffff;
  border-bottom: 1px solid #e5e5e5;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .collapse-btn {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 1px solid #000000;
      background: #ffffff;
      border-radius: 0;
      color: #000000;
      cursor: pointer;
      transition: all 0.15s ease;

      &:hover {
        background: #000000;
        color: #ffffff;
      }
    }

    .admin-breadcrumb {
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.06em;
      text-transform: uppercase;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;

    .header-action-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 8px 14px;
      border-radius: 0;
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.06em;
      text-decoration: none;
      cursor: pointer;
      text-transform: uppercase;
      transition: all 0.15s ease;

      &.btn-store-view {
        background: #ffffff;
        border: 1px solid #000000;
        color: #000000;

        &:hover {
          background: #000000;
          color: #ffffff;
        }
      }

      &.btn-pos {
        background: #000000;
        border: 1px solid #000000;
        color: #ffffff;

        &:hover {
          background: #262626;
        }
      }
    }

    .role-badges {
      display: flex;
      gap: 6px;

      .role-pill {
        font-size: 9px;
        font-weight: 900;
        letter-spacing: 0.08em;
        padding: 4px 8px;
        border-radius: 0;
        border: 1px solid #000000;
        background: #ffffff;
        color: #000000;
        text-transform: uppercase;

        &.admin {
          background: #000000;
          color: #ffffff;
        }
      }
    }

    .user-profile {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 0;
      border: 1px solid transparent;
      transition: all 0.15s ease;

      &:hover {
        border-color: #000000;
      }

      .avatar {
        width: 30px;
        height: 30px;
        border-radius: 0;
        background: #000000;
        color: #ffffff;
        font-weight: 900;
        font-size: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .user-info {
        display: flex;
        flex-direction: column;
        line-height: 1.2;

        .name {
          font-size: 12px;
          font-weight: 800;
          color: #000000;
        }

        .username {
          font-size: 10px;
          color: #737373;
        }
      }

      .arrow-icon {
        font-size: 11px;
        color: #737373;
      }
    }
  }
}

.content-area {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  background-color: #fafafa;
}

.mobile-sidebar-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.fade-backdrop-enter-active,
.fade-backdrop-leave-active {
  transition: opacity 0.2s ease;
}
.fade-backdrop-enter-from,
.fade-backdrop-leave-to {
  opacity: 0;
}

.mobile-close-btn {
  margin-left: auto;
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid #333333;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
  flex-shrink: 0;

  &:hover {
    background: #ffffff;
    color: #000000;
  }
}

/* =========================================================
   ADMIN RESPONSIVE (TABLET & MOBILE)
   ========================================================= */
@media (max-width: 1024px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 264px !important;
    z-index: 1001;
    transform: translateX(-100%);
    transition: transform 0.24s cubic-bezier(0.16, 1, 0.3, 1);
    box-shadow: none;

    &.mobile-open {
      transform: translateX(0);
      box-shadow: 12px 0 40px rgba(0, 0, 0, 0.35);
    }
  }

  .top-header {
    padding: 0 16px;
    height: 58px;
    gap: 10px;

    .header-left {
      gap: 10px;
      min-width: 0;
      flex: 1;

      .collapse-btn {
        flex-shrink: 0;
      }

      .admin-breadcrumb {
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
      }
    }

    .header-right {
      gap: 8px;
      flex-shrink: 0;

      .role-badges {
        display: none;
      }

      .user-profile {
        padding: 4px;
        .user-info,
        .arrow-icon {
          display: none;
        }
      }
    }
  }
}

@media (max-width: 640px) {
  .top-header {
    padding: 0 12px;
    height: 54px;

    .header-left {
      .collapse-btn {
        width: 34px;
        height: 34px;
      }

      .admin-breadcrumb {
        font-size: 10.5px;

        :deep(.crumb-root) {
          display: none;
        }
      }
    }

    .header-right {
      gap: 6px;

      .header-action-btn {
        padding: 0;
        width: 34px;
        height: 34px;
        justify-content: center;

        .btn-label {
          display: none;
        }
      }
    }
  }
}
</style>
