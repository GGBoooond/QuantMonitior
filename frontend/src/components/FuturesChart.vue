<template>
  <div ref="chartContainer" v-loading="loading" style="width: 100%; height: 60vh;"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import axios from 'axios';
import { ElMessage } from 'element-plus';

const props = defineProps({
  symbol: { type: String, required: true },
  startDate: { type: String, required: true },
  endDate: { type: String, required: true },
});

const chartContainer = ref(null);
let myChart = null;
const loading = ref(true);
let chartOptions = {};
const isRealTimeUpdateActive = ref(false);

// 获取图表数据和阈值
const fetchData = async () => {
  loading.value = true;
  try {
    const [historyRes, thresholdsRes] = await Promise.all([
      axios.get(`/api/futures/${props.symbol}/history`, {
        params: { startDate: props.startDate, endDate: props.endDate },
      }),
      axios.get(`/api/futures/${props.symbol}/thresholds`),
    ]);

    const chartData = processData(historyRes.data);
    const thresholds = thresholdsRes.data;

    // 检查结束日期是否是今天，以决定是否开启实时更新
    const today = new Date().toISOString().slice(0, 10);
    isRealTimeUpdateActive.value = props.endDate === today;

    initChart(chartData, thresholds);

  } catch (error) {
    console.error('Error fetching data:', error);
    ElMessage.error(`获取数据失败: ${error.response?.data?.message || error.message}`);
  } finally {
    loading.value = false;
  }
};

const processData = (rawData) => {
  const dates = rawData.map(item => item.time);
  const values = rawData.map(item => [item.open, item.close, item.low, item.high]);
  return { dates, values };
};

const initChart = (data, thresholds) => {
  if (!myChart) return;

  const markLines = [];
  if (thresholds.high) {
    markLines.push({
      yAxis: thresholds.high,
      name: '7日最高价',
      lineStyle: { color: '#f56c6c', type: 'dashed' },
      label: { formatter: '7日最高: {c}', position: 'insideEndTop' },
    });
  }
  if (thresholds.low) {
    markLines.push({
      yAxis: thresholds.low,
      name: '7日最低价',
      lineStyle: { color: '#67c23a', type: 'dashed' },
      label: { formatter: '7日最低: {c}', position: 'insideEndBottom' },
    });
  }

  chartOptions = {
    title: {
      text: `${props.symbol} K线图`,
      subtext: `${props.startDate} 至 ${props.endDate}`,
      left: 'center',
    },
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    xAxis: { type: 'category', data: data.dates },
    yAxis: { scale: true, splitArea: { show: true } },
    grid: { bottom: 80 },
    dataZoom: [
      { type: 'inside', start: 50, end: 100 },
      { show: true, type: 'slider', bottom: 10, start: 50, end: 100 },
    ],
    series: [
      {
        name: props.symbol,
        type: 'candlestick',
        data: data.values,
        itemStyle: { color: '#ec0000', color0: '#00da3c', borderColor: '#8A0000', borderColor0: '#008F28' },
        markLine: {
          symbol: ['none', 'none'],
          data: markLines,
        },
      },
    ],
  };

  myChart.setOption(chartOptions);
};

// 实时更新K线图逻辑
const updateWithRealTimeData = (data) => {
  if (!myChart || !isRealTimeUpdateActive.value) return;

  const currentOption = myChart.getOption();
  const seriesData = currentOption.series[0].data;
  const dateData = currentOption.xAxis[0].data;
  const lastDataPoint = seriesData[seriesData.length - 1];
  const lastDate = dateData[dateData.length - 1];

  const incomingDate = data.time.split(' ')[0]; // 只取日期部分 YYYY-MM-DD

  // 如果是新的一天，添加一个新的K线点
  if (incomingDate !== lastDate) {
    dateData.push(incomingDate);
    seriesData.push([data.price, data.price, data.price, data.price]); // [open, close, low, high]
  } else { // 否则，更新当天的K线
    lastDataPoint[1] = data.price; // 更新收盘价
    lastDataPoint[2] = Math.min(lastDataPoint[2], data.price); // 更新最低价
    lastDataPoint[3] = Math.max(lastDataPoint[3], data.price); // 更新最高价
  }

  myChart.setOption({
    xAxis: { data: dateData },
    series: [{ data: seriesData }]
  });
};


onMounted(() => {
  myChart = echarts.init(chartContainer.value);
  fetchData();
  window.addEventListener('resize', () => myChart?.resize());
});

onUnmounted(() => {
  if (myChart) {
    myChart.dispose();
  }
});

defineExpose({
  updateWithRealTimeData,
});
</script>