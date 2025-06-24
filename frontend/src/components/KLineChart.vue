<template>
  <div ref="chartEl" class="kline-chart"></div>
</template>

<script setup>
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { useSocket } from '@/composables/useSocket'
import { getFutureHistory } from '@/api/futures'

const props = defineProps({
  symbol: {
    type: String,
    required: true
  }
})

const chartEl = ref(null)
let chartInstance = null

// 初始化图表
onMounted(() => {
  if (chartEl.value) {
    chartInstance = echarts.init(chartEl.value)
    loadChartData()
    window.addEventListener('resize', resizeChart)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  if (chartInstance) chartInstance.dispose()
})

function resizeChart() {
  chartInstance?.resize()
}

// 监听期货变化
watch(() => props.symbol, (newSymbol) => {
  if (newSymbol) loadChartData()
})

async function loadChartData() {
  if (!props.symbol) return
  
  try {
    const data = await getFutureHistory(props.symbol, 30)
    
    const option = {
      title: { text: `${props.symbol} K线图`, left: 'center' },
      tooltip: { trigger: 'axis' },
      xAxis: { data: data.dates },
      yAxis: { scale: true },
      series: [{
        type: 'candlestick',
        data: data.prices,
        itemStyle: {
          color: '#ec0000',
          color0: '#00da3c'
        }
      }]
    }
    
    chartInstance?.setOption(option)
  } catch (error) {
    console.error('加载K线图数据失败:', error)
  }
}

// 实时价格更新
useSocket('price-update', (update) => {
  if (update.symbol === props.symbol) {
    // 这里可以添加实时更新逻辑
    console.log('收到实时价格更新:', update)
  }
})
</script>

<style scoped>
.kline-chart {
  width: 100%;
  height: 500px;
  background: #fff;
  border-radius: 8px;
}
</style> 