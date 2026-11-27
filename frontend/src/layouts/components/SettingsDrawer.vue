<template>
  <!-- Floating Settings Handle Button (vue-element-admin RightPanel style) -->
  <button
    type="button"
    class="vea-settings-handle"
    title="Cấu hình giao diện Admin"
    @click="drawerOpen = true"
  >
    <el-icon :size="18"><Setting /></el-icon>
  </button>

  <el-drawer
    v-model="drawerOpen"
    title="Cấu hình Giao diện Hệ thống"
    size="285px"
    direction="rtl"
    append-to-body
  >
    <div class="vea-settings-body">
      <div class="setting-section-title">Chủ đề &amp; Màu sắc</div>

      <div class="drawer-item">
        <span>Màu chủ đạo (Theme)</span>
        <el-color-picker
          v-model="settings.theme"
          :predefine="predefineColors"
          size="small"
          @change="(c) => settings.setTheme(c)"
        />
      </div>

      <div class="theme-swatches">
        <button
          v-for="color in predefineColors"
          :key="color"
          type="button"
          class="swatch-dot"
          :class="{ active: settings.theme?.toLowerCase() === color.toLowerCase() }"
          :style="{ backgroundColor: color }"
          :title="color"
          @click="settings.setTheme(color)"
        />
      </div>

      <div class="drawer-item">
        <span>Giao diện Sidebar</span>
        <el-radio-group v-model="settings.sidebarTheme" size="small">
          <el-radio-button value="dark">Tối</el-radio-button>
          <el-radio-button value="light">Sáng</el-radio-button>
        </el-radio-group>
      </div>

      <el-divider />

      <div class="setting-section-title">Bố cục Trang Quản trị</div>

      <div class="drawer-item">
        <span>Thanh tab Tags-View</span>
        <el-switch v-model="settings.showTagsView" />
      </div>

      <div class="drawer-item">
        <span>Cố định Header</span>
        <el-switch v-model="settings.fixedHeader" />
      </div>

      <div class="drawer-item">
        <span>Hiển thị Logo Sidebar</span>
        <el-switch v-model="settings.sidebarLogo" />
      </div>

      <div class="drawer-item">
        <span>Cỡ hiển thị Element</span>
        <el-select v-model="settings.elementSize" size="small" style="width: 108px">
          <el-option label="Mặc định" value="default" />
          <el-option label="Lớn" value="large" />
          <el-option label="Nhỏ gọn" value="small" />
        </el-select>
      </div>

      <el-divider />

      <el-button plain style="width: 100%" @click="settings.resetDefaults">
        Khôi phục mặc định vue-element-admin
      </el-button>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref } from 'vue'
import { Setting } from '@element-plus/icons-vue'
import { useAppSettingsStore } from '@/stores/appSettings'

const settings = useAppSettingsStore()
const drawerOpen = ref(false)

const predefineColors = [
  '#409EFF', // vue-element-admin classic blue
  '#1890FF', // Ant Design blue
  '#304156', // Dark slate
  '#11A983', // Emerald green
  '#13C2C2', // Cyan
  '#6959CD', // Royal violet
  '#E6A23C', // Amber
  '#F5222D'  // Crimson
]
</script>

<style scoped lang="scss">
.vea-settings-handle {
  position: fixed;
  right: 0;
  bottom: 36px;
  width: 40px;
  height: 40px;
  background-color: var(--vea-theme, #409eff);
  color: #ffffff;
  border: none;
  border-radius: 6px 0 0 6px !important;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 999;
  box-shadow: -2px 2px 10px rgba(0, 0, 0, 0.16);
  transition: opacity 0.2s ease, transform 0.2s ease;

  &:hover {
    opacity: 0.92;
    transform: translateX(-2px);
  }
}

.vea-settings-body {
  padding: 4px 2px;
  font-size: 13.5px;
  color: #303133;

  .setting-section-title {
    font-size: 13px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 14px;
  }

  .drawer-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
    font-size: 13px;
    color: #606266;
  }

  .theme-swatches {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 10px;

    .swatch-dot {
      width: 22px;
      height: 22px;
      border-radius: 3px !important;
      border: 2px solid transparent;
      cursor: pointer;
      padding: 0;
      transition: transform 0.15s ease;

      &:hover {
        transform: scale(1.1);
      }

      &.active {
        border-color: #303133;
        box-shadow: 0 0 0 2px #ffffff inset;
      }
    }
  }
}

@media (max-width: 768px) {
  .vea-settings-handle {
    bottom: 16px;
    width: 34px;
    height: 34px;
    opacity: 0.85;
  }
}
</style>
