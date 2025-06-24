import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

export const getFutures = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/futures`)
    return response.data
  } catch (error) {
    console.error('获取期货列表失败:', error)
    return [
      { symbol: 'CL', name: '原油期货' },
      { symbol: 'GC', name: '黄金期货' },
      { symbol: 'SI', name: '白银期货' }
    ]
  }
}

export const getFutureHistory = async (symbol, days = 30) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/futures/${symbol}/history?days=${days}`)
    return response.data
  } catch (error) {
    console.error('获取历史数据失败:', error)
    
    // 生成模拟数据
    const dates = []
    const prices = []
    const today = new Date()
    
    for (let i = days; i >= 0; i--) {
      const date = new Date(today)
      date.setDate(date.getDate() - i)
      dates.push(date.toISOString().split('T')[0])
      
      // 生成随机价格数据
      const open = 100 + Math.random() * 50
      const close = open + (Math.random() - 0.5) * 10
      const high = Math.max(open, close) + Math.random() * 5
      const low = Math.min(open, close) - Math.random() * 5
      
      prices.push([open, close, low, high])
    }
    
    return { dates, prices }
  }
} 