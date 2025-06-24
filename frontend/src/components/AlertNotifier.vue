<template>
  <el-dialog 
    v-model="visible" 
    title="价格警报" 
    width="500px"
    :close-on-click-modal="false"
  >
    <div class="alert-content">
      <el-alert :title="alertMessage" type="error" show-icon />
    </div>
    <template #footer>
      <el-button type="primary" @click="visible = false">确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useSocket } from '@/composables/useSocket'

const visible = ref(false)
const alertMessage = ref('')

// 监听警报消息
useSocket('alerts', (message) => {
  alertMessage.value = message
  visible.value = true
})
</script>

<style scoped>
.alert-content {
  padding: 20px;
  font-size: 16px;
}
</style> 