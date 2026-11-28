<template>
  <el-select
    ref="selectRef"
    :model-value="modelValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled"
    :loading="loading"
    :size="size"
    :no-data-text="emptyText"
    :no-match-text="emptyText"
    popper-class="adha-quick-select-popper"
    class="adha-quick-select"
    @update:model-value="onUpdate"
    @change="onChange"
    @visible-change="onVisibleChange"
  >
    <template #header>
      <div class="qs-header" @click.stop>
        <el-icon class="qs-icon"><Search /></el-icon>
        <input
          ref="searchInputRef"
          v-model="searchQuery"
          type="text"
          class="qs-input"
          :placeholder="searchPlaceholder"
          @keydown.stop="onSearchKeydown"
        />
        <span v-if="searchQuery" class="qs-count">
          {{ filteredOptions.length }}/{{ normalizedOptions.length }}
        </span>
        <button
          v-if="searchQuery"
          type="button"
          class="qs-clear"
          title="Xóa từ khóa"
          @click.stop="clearSearch"
        >
          ×
        </button>
      </div>
    </template>

    <!-- Hidden selected option fallback so Element Plus never flashes raw ID while filtered out -->
    <el-option
      v-if="hiddenSelectedOption"
      :key="`__sel_${hiddenSelectedOption._val}`"
      :label="hiddenSelectedOption._label"
      :value="hiddenSelectedOption._val"
      style="display: none"
    />

    <el-option
      v-for="opt in filteredOptions"
      :key="opt._val"
      :label="opt._label"
      :value="opt._val"
      :disabled="opt._disabled"
    >
      <slot name="option" :item="opt._raw" :label="opt._label" :value="opt._val">
        {{ opt._label }}
      </slot>
    </el-option>

    <template v-if="filteredOptions.length === 0" #empty>
      <div class="qs-empty">{{ emptyText }}</div>
    </template>
  </el-select>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean, Object],
    default: null
  },
  options: {
    type: Array,
    default: () => []
  },
  labelKey: {
    type: [String, Function],
    default: 'label'
  },
  valueKey: {
    type: [String, Function],
    default: 'value'
  },
  searchKeys: {
    type: [Array, Function],
    default: null
  },
  disabledKey: {
    type: [String, Function],
    default: null
  },
  placeholder: {
    type: String,
    default: 'Chọn...'
  },
  searchPlaceholder: {
    type: String,
    default: 'Gõ để tìm nhanh...'
  },
  clearable: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  size: {
    type: String,
    default: 'default'
  },
  emptyText: {
    type: String,
    default: 'Không tìm thấy kết quả phù hợp'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const selectRef = ref(null)
const searchInputRef = ref(null)
const searchQuery = ref('')

function normalizeVi(str) {
  return String(str ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[đĐ]/g, 'd')
    .toLowerCase()
    .trim()
}

function resolveField(item, keyOrFn) {
  if (typeof keyOrFn === 'function') return keyOrFn(item)
  if (item !== null && typeof item === 'object') return item[keyOrFn]
  return item
}

const normalizedOptions = computed(() => {
  return (props.options || []).map((raw) => {
    const val = resolveField(raw, props.valueKey)
    const label = String(resolveField(raw, props.labelKey) ?? '')
    let searchHaystack = label
    if (typeof props.searchKeys === 'function') {
      searchHaystack = `${label} ${props.searchKeys(raw) || ''}`
    } else if (Array.isArray(props.searchKeys) && raw && typeof raw === 'object') {
      const extras = props.searchKeys.map((k) => raw[k] ?? '').join(' ')
      searchHaystack = `${label} ${extras}`
    }
    const disabled = props.disabledKey ? Boolean(resolveField(raw, props.disabledKey)) : false
    return {
      _raw: raw,
      _val: val,
      _label: label,
      _norm: normalizeVi(searchHaystack),
      _disabled: disabled
    }
  })
})

const filteredOptions = computed(() => {
  const q = normalizeVi(searchQuery.value)
  if (!q) return normalizedOptions.value
  const tokens = q.split(/\s+/).filter(Boolean)
  return normalizedOptions.value.filter((opt) => tokens.every((t) => opt._norm.includes(t)))
})

const hiddenSelectedOption = computed(() => {
  if (props.modelValue === null || props.modelValue === undefined || props.modelValue === '') {
    return null
  }
  const inFiltered = filteredOptions.value.some((o) => o._val === props.modelValue)
  if (inFiltered) return null
  return normalizedOptions.value.find((o) => o._val === props.modelValue) || null
})

function onUpdate(val) {
  emit('update:modelValue', val)
}

function onChange(val) {
  emit('change', val)
}

function onVisibleChange(visible) {
  if (visible) {
    searchQuery.value = ''
    nextTick(() => {
      searchInputRef.value?.focus()
    })
  } else {
    searchQuery.value = ''
  }
}

function clearSearch() {
  searchQuery.value = ''
  nextTick(() => searchInputRef.value?.focus())
}

function onSearchKeydown(e) {
  if (e.key === 'Enter') {
    e.preventDefault()
    const first = filteredOptions.value.find((o) => !o._disabled)
    if (first) {
      emit('update:modelValue', first._val)
      emit('change', first._val)
      selectRef.value?.blur()
    }
  } else if (e.key === 'Escape') {
    selectRef.value?.blur()
  }
}
</script>

<style lang="scss">
.adha-quick-select {
  width: 100%;
}

.adha-quick-select-popper {
  border-radius: 6px !important;
  z-index: 3000 !important;

  .el-select-dropdown__header {
    padding: 8px !important;
    border-bottom: 1px solid #e5e5e5 !important;
    background: #fafafa;
  }

  .qs-header {
    display: flex;
    align-items: center;
    gap: 6px;
    background: #ffffff;
    border: 1px solid #d4d4d4;
    border-radius: 4px;
    padding: 4px 8px;
    transition: border-color 0.14s ease;

    &:focus-within {
      border-color: #000000;
    }

    .qs-icon {
      color: #737373;
      font-size: 13px;
      flex-shrink: 0;
    }

    .qs-input {
      flex: 1;
      min-width: 0;
      border: none;
      outline: none;
      background: transparent;
      font-size: 12.5px;
      color: #171717;
      font-family: inherit;

      &::placeholder {
        color: #a3a3a3;
      }
    }

    .qs-count {
      font-size: 10px;
      font-weight: 800;
      color: #525252;
      background: #f5f5f5;
      border: 1px solid #e5e5e5;
      padding: 1px 5px;
      font-family: var(--font-mono);
      flex-shrink: 0;
    }

    .qs-clear {
      border: none;
      background: transparent;
      color: #737373;
      font-size: 15px;
      line-height: 1;
      cursor: pointer;
      padding: 0 2px;
      flex-shrink: 0;

      &:hover {
        color: #000000;
      }
    }
  }

  .qs-empty {
    padding: 14px 10px;
    text-align: center;
    font-size: 12px;
    color: #737373;
  }
}
</style>
