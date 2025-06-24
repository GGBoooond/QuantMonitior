<template>
  <el-container class="main-container">
    <el-header class="header">
      <h1>期货自动化监控系统</h1>
    </el-header>
    <el-main>
      <el-card>
        <div class="control-panel">
          <span class="control-label">期货品种ID:</span>
          <el-input
              v-model="symbolInput"
              placeholder="例如: RU00.SHF"
              @keyup.enter="handleQuery"
              style="width: 200px; margin-right: 20px;"
          />
          <span class="control-label">日期范围:</span>
          <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="margin-right: 20px;"
          />
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>

          <el-tag :type="connectionStatus.type" class="connection-status-tag">
            <el-icon><component :is="connectionStatus.icon" /></el-icon>
            {{ connectionStatus.text }}
          </el-tag>
        </div>
        <FuturesChart
            :key="chartKey"
            :symbol="activeSymbol"
            :start-date="queryStartDate"
            :end-date="queryEndDate"
            ref="futuresChartRef"
        />
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue';
import { ElNotification, ElMessage } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import FuturesChart from './components/FuturesChart.vue';

const symbolInput = ref('RU00.SHF');
const dateRange = ref([]);

const activeSymbol = ref('RU00.SHF');
const queryStartDate = ref('');
const queryEndDate = ref('');
const chartKey = ref(0); // 用于强制重新渲染图表组件

const futuresChartRef = ref(null);
let websocket = null;
const isConnected = ref(false);

const connectionStatus = computed(() => {
  if (isConnected.value) {
    return { text: '监控已连接', type: 'success', icon: 'SuccessFilled' };
  }
  return { text: '监控已断开', type: 'danger', icon: 'CircleCloseFilled' };
});

const initWebSocket = () => {
  const wsUrl = `ws://${window.location.hostname}:8080/ws/monitor`;
  websocket = new WebSocket(wsUrl);

  websocket.onopen = () => {
    console.log('WebSocket connected');
    isConnected.value = true;
    subscribeToSymbol(activeSymbol.value);
  };

  websocket.onmessage = (event) => {
    const data = JSON.parse(event.data);

    if (data.type === 'ALERT_HIGH' || data.type === 'ALERT_LOW') {
      ElNotification({
        title: data.type === 'ALERT_HIGH' ? '价格突破警报' : '价格跌破警报',
        message: `${data.time} - ${data.message}`,
        type: 'warning',
        duration: 0,
        position: 'top-right',
      });
    } else if (data.type === 'LATEST_PRICE') {
      // 如果当前监控的品种和图表展示的品种一致，则更新图表
      if (data.symbol === activeSymbol.value && futuresChartRef.value) {
        futuresChartRef.value.updateWithRealTimeData(data);
      }
    }
  };

  websocket.onclose = () => {
    console.log('WebSocket disconnected');
    isConnected.value = false;
    setTimeout(initWebSocket, 5000);
  };

  websocket.onerror = (error) => {
    console.error('WebSocket error:', error);
    isConnected.value = false;
    websocket.close();
  };
};

const subscribeToSymbol = (symbol) => {
  if (websocket && websocket.readyState === WebSocket.OPEN && symbol) {
    const message = { type: 'subscribe', symbol: symbol };
    websocket.send(JSON.stringify(message));
    console.log(`Subscribed to ${symbol}`);
  }
};

const handleQuery = () => {
  if (!symbolInput.value) {
    ElMessage.error('请输入期货品种ID');
    return;
  }
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.error('请选择日期范围');
    return;
  }

  activeSymbol.value = symbolInput.value;
  queryStartDate.value = dateRange.value[0];
  queryEndDate.value = dateRange.value[1];

  // 订阅新的品种
  subscribeToSymbol(activeSymbol.value);

  // 通过改变key来强制重新挂载和请求数据
  chartKey.value++;
};

const setDefaultDateRange = () => {
  const end = new Date();
  const start = new Date();
  start.setTime(start.getTime() - 3600 * 1000 * 24 * 90); // 默认90天

  const formatDate = (date) => {
    const y = date.getFullYear();
    const m = (date.getMonth() + 1).toString().padStart(2, '0');
    const d = date.getDate().toString().padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  dateRange.value = [formatDate(start), formatDate(end)];
  queryStartDate.value = dateRange.value[0];
  queryEndDate.value = dateRange.value[1];
};


onMounted(() => {
  setDefaultDateRange();
  initWebSocket();
});

onUnmounted(() => {
  if (websocket) {
    websocket.close();
  }
});

</script>

<style>
body {
  margin: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  background-color: #f2f3f5;
}
.main-container {
  height: 100vh;
}
.header {
  background-color: #409EFF;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,.1);
}
.header h1 {
  margin: 0;
}
.control-panel {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}
.control-label {
  margin-right: 15px;
  font-size: 16px;
  color: #606266;
}
.connection-status-tag {
  margin-left: auto;
  font-size: 14px;
}
body { margin: 0; font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif; background-color: #f2f3f5; }
.main-container { height: 100vh; }
.header { background-color: #409EFF; color: white; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 12px 0 rgba(0,0,0,.1); }
.header h1 { margin: 0; }
.control-panel { display: flex; align-items: center; margin-bottom: 20px; flex-wrap: wrap; }
.control-label { margin-right: 10px; font-size: 16px; color: #606266; }
.connection-status-tag { margin-left: auto; font-size: 14px; }
</style>
