<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <AdhaLogoMark :size="44" variant="dark" />
        <div class="brand-text">
          <h1>ADHA SNEAKER</h1>
          <p>HỆ THỐNG QUẢN LÝ CỬA HÀNG & BÁN LẺ POS</p>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="handleLogin"
        class="login-form"
      >
        <el-form-item label="Tài khoản truy cập" prop="username">
          <el-input
            v-model="form.username"
            size="large"
            placeholder="Tên đăng nhập (vd: admin, manager, staff)"
            :prefix-icon="User"
            class="input-custom"
          />
        </el-form-item>

        <el-form-item label="Mật khẩu bảo mật" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            size="large"
            show-password
            placeholder="Nhập mật khẩu"
            :prefix-icon="Lock"
            class="input-custom"
          />
        </el-form-item>

        <button
          type="button"
          class="btn-login-submit"
          :disabled="loading"
          @click="handleLogin"
        >
          <span v-if="loading">ĐANG XÁC THỰC...</span>
          <span v-else>ĐĂNG NHẬP VÀO HỆ THỐNG →</span>
        </button>
      </el-form>

      <div class="back-store-row">
        <router-link to="/" class="back-link">
          ← Quay lại Website Bán Hàng (ADHA Sneaker)
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import AdhaLogoMark from '@/components/common/AdhaLogoMark.vue'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: 'Vui lòng nhập tên đăng nhập', trigger: 'blur' }],
  password: [{ required: true, message: 'Vui lòng nhập mật khẩu', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await authStore.login(form.username.trim(), form.password)
      ElMessage.success(`Đăng nhập Quản trị thành công: ${authStore.user?.fullName || form.username}!`)
      router.push('/admin/dashboard')
    } catch (err) {
      if (!err.response && err.message) {
        ElMessage.error(err.message)
      }
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2d3a4b;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 440px;
  background: #ffffff;
  border: 1px solid #e6ebf5;
  border-radius: 6px;
  padding: 40px 36px;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.22);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;

  .logo-mark {
    width: 38px;
    height: 38px;
    background: var(--vea-theme, #409eff);
    color: #ffffff;
    font-weight: 900;
    font-size: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 6px;
    letter-spacing: -0.05em;
    flex-shrink: 0;
  }

  .brand-text {
    h1 {
      font-size: 18px;
      font-weight: 700;
      letter-spacing: 0.04em;
      color: #303133;
      margin: 0;
      line-height: 1.1;
    }

    p {
      font-size: 10px;
      font-weight: 600;
      letter-spacing: 0.08em;
      color: #909399;
      margin: 4px 0 0;
    }
  }
}

.login-form {
  :deep(.el-form-item__label) {
    font-size: 12px;
    font-weight: 600;
    color: #606266;
    padding-bottom: 6px;
  }
}

.btn-login-submit {
  width: 100%;
  height: 44px;
  background: var(--vea-theme, #409eff);
  color: #ffffff;
  border: 1px solid var(--vea-theme, #409eff);
  border-radius: 4px;
  font-family: inherit;
  font-size: 13.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
  cursor: pointer;
  margin-top: 10px;
  transition: background 0.15s ease;

  &:hover:not(:disabled) {
    background: var(--vea-theme-light-3, #66b1ff);
    border-color: var(--vea-theme-light-3, #66b1ff);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.demo-accounts {
  margin-top: 26px;
  padding-top: 18px;
  border-top: 1px dashed #dcdfe6;

  .demo-title {
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.05em;
    color: #909399;
    margin-bottom: 10px;
  }

  .demo-buttons {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .demo-pill {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 12px;
      background: #f5f7fa;
      border: 1px solid #e6ebf5;
      border-radius: 4px;
      font-family: inherit;
      cursor: pointer;
      transition: all 0.15s ease;

      .pill-role {
        font-size: 10px;
        font-weight: 700;
        letter-spacing: 0.05em;
        padding: 3px 7px;
        border-radius: 3px;

        &.admin {
          background: var(--vea-theme, #409eff);
          color: #ffffff;
        }

        &.manager {
          background: #fff8e6;
          color: #e6a23c;
          border: 1px solid #f5dab1;
        }

        &.staff {
          background: #e7faf0;
          color: #13ce66;
          border: 1px solid #d0f5e0;
        }
      }

      .pill-acc {
        font-size: 12px;
        font-family: var(--font-mono);
        font-weight: 600;
        color: #606266;
      }

      &:hover {
        background: var(--vea-theme-light-9, #ecf5ff);
        border-color: var(--vea-theme-light-7, #c6e2ff);
        .pill-acc {
          color: var(--vea-theme, #409eff);
        }
      }
    }
  }
}

.back-store-row {
  margin-top: 22px;
  text-align: center;

  .back-link {
    font-size: 12px;
    font-weight: 600;
    color: #909399;
    text-decoration: none;
    transition: color 0.15s ease;

    &:hover {
      color: var(--vea-theme, #409eff);
    }
  }
}
</style>
