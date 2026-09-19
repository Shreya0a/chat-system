import { useState, useRef, useCallback, useEffect } from 'react'

/**
 * The HISTORY message from the Java server contains tab-delimited
 * serialised Message objects — one per line — produced by Message.serialize().
 * This decoder mirrors Message.deserialize() on the Java side.
 */
function decodeWireMessage(line) {
  const parts = line.split('\t')
  if (parts.length < 5) return null
  return {
    type: parts[0],
    sender: unescape(parts[1]),
    recipient: unescape(parts[2]),
    timestamp: parseInt(parts[3]) || Date.now(),
    content: unescape(parts[4]),
  }
}

function unescape(value) {
  if (!value) return ''
  let result = ''
  for (let i = 0; i < value.length; i++) {
    if (value[i] === '\\' && i + 1 < value.length) {
      const next = value[++i]
      if (next === 't') result += '\t'
      else if (next === 'n') result += '\n'
      else if (next === '\\') result += '\\'
      else result += next
    } else {
      result += value[i]
    }
  }
  return result
}

/**
 * Renders a JSON message received over WebSocket into the same structure
 * the rest of the React app expects.
 */
function normalise(raw) {
  return {
    id: `${raw.timestamp}-${raw.sender}-${Math.random().toString(36).slice(2, 6)}`,
    type: raw.type,
    sender: raw.sender,
    recipient: raw.recipient,
    timestamp: raw.timestamp,
    content: raw.content,
  }
}

export default function useChat() {
  const [messages, setMessages] = useState([])
  const [users, setUsers] = useState([])
  const [connected, setConnected] = useState(false)
  const [username, setUsername] = useState('')
  const wsRef = useRef(null)

  const connect = useCallback((name, wsUrl) => {
    if (wsRef.current) wsRef.current.close()

    const ws = new WebSocket(wsUrl)
    wsRef.current = ws

    ws.onopen = () => {
      ws.send(JSON.stringify({
        type: 'JOIN',
        sender: name,
        recipient: '',
        timestamp: Date.now(),
        content: name,
      }))
    }

    ws.onmessage = (event) => {
      let raw
      try {
        raw = JSON.parse(event.data)
      } catch {
        return
      }

      switch (raw.type) {
        case 'SERVER': {
          const msg = normalise(raw)
          setMessages((prev) => [...prev, msg])
          // "Welcome, Rahul!" means we successfully joined
          if (msg.content.startsWith('Welcome, ')) {
            setConnected(true)
            setUsername(msg.content.replace('Welcome, ', '').replace('!', ''))
          }
          break
        }

        case 'PUBLIC': {
          const msg = normalise(raw)
          setMessages((prev) => [...prev, msg])
          break
        }

        case 'PRIVATE': {
          const msg = normalise(raw)
          setMessages((prev) => [...prev, msg])
          break
        }

        case 'USERLIST': {
          const names = raw.content.split(',').filter(Boolean).map((n) => n.trim())
          setUsers(names)
          break
        }

        case 'HISTORY': {
          const lines = raw.content.split('\n').filter(Boolean)
          const historic = lines.map((line) => {
            const decoded = decodeWireMessage(line)
            return decoded ? normalise(decoded) : null
          }).filter(Boolean)
          setMessages((prev) => [...historic, ...prev])
          break
        }

        default:
          break
      }
    }

    ws.onclose = () => {
      setConnected(false)
      setMessages((prev) => [
        ...prev,
        { id: Date.now().toString(), type: 'SERVER', sender: 'SERVER', recipient: '', timestamp: Date.now(), content: 'You were disconnected.' },
      ])
      wsRef.current = null
    }

    ws.onerror = () => {}
  }, [])

  const sendPublic = useCallback((text) => {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return
    wsRef.current.send(JSON.stringify({
      type: 'PUBLIC',
      sender: username,
      recipient: '',
      timestamp: Date.now(),
      content: text,
    }))
  }, [username])

  const sendPrivate = useCallback((recipient, text) => {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return
    wsRef.current.send(JSON.stringify({
      type: 'PRIVATE',
      sender: username,
      recipient,
      timestamp: Date.now(),
      content: text,
    }))
  }, [username])

  const disconnect = useCallback(() => {
    if (wsRef.current) {
      wsRef.current.send(JSON.stringify({
        type: 'LEAVE',
        sender: username,
        recipient: '',
        timestamp: Date.now(),
        content: '',
      }))
      wsRef.current.close()
      wsRef.current = null
    }
    setConnected(false)
  }, [username])

  useEffect(() => {
    return () => {
      if (wsRef.current) {
        wsRef.current.close()
        wsRef.current = null
      }
    }
  }, [])

  return { messages, users, connected, username, connect, sendPublic, sendPrivate, disconnect }
}