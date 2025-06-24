import { onUnmounted } from 'vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

export function useSocket(topic, callback) {
  let stompClient = null
  let isConnected = false
  let reconnectAttempts = 0
  const maxReconnectAttempts = 5
  
  const connect = () => {
    const socket = new SockJS('http://localhost:8080/ws-alerts')
    stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => console.log(str)
    })
    
    stompClient.onConnect = (frame) => {
      isConnected = true
      reconnectAttempts = 0
      console.log('WebSocket连接成功')
      
      stompClient.subscribe(`/topic/${topic}`, (message) => {
        callback(message.body)
      })
    }
    
    stompClient.onStompError = (frame) => {
      console.error('WebSocket协议错误:', frame.headers['message'])
    }
    
    stompClient.onWebSocketClose = () => {
      isConnected = false
      console.log('WebSocket连接关闭')
      
      if (reconnectAttempts < maxReconnectAttempts) {
        reconnectAttempts++
        console.log(`尝试重新连接 (${reconnectAttempts}/${maxReconnectAttempts})`)
        setTimeout(connect, 5000)
      }
    }
    
    stompClient.activate()
  }
  
  connect()
  
  onUnmounted(() => {
    if (stompClient && isConnected) {
      stompClient.deactivate()
      console.log('WebSocket已断开')
    }
  })
  
  return {
    disconnect: () => {
      if (stompClient) {
        stompClient.deactivate()
      }
    }
  }
} 