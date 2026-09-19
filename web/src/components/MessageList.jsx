import { useEffect, useRef } from 'react'

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

function isMe(sender, myUsername) {
  return sender && sender.toLowerCase() === myUsername.toLowerCase()
}

function MessageRow({ msg, username }) {
  // Server notices
  if (msg.type === 'SERVER') {
    return (
      <div className="text-center my-2">
        <span className="text-slate-500 text-xs bg-slate-800/60 px-3 py-1 rounded-full">
          {msg.content}
        </span>
      </div>
    )
  }

  const me = isMe(msg.sender, username)

  // Public messages
  if (msg.type === 'PUBLIC') {
    return (
      <div className={`group flex gap-3 px-4 py-1.5 hover:bg-slate-800/40 ${me ? '' : ''}`}>
        <span className="text-slate-500 text-xs w-12 pt-0.5 flex-shrink-0 text-right">
          {formatTime(msg.timestamp)}
        </span>
        <div className="min-w-0 flex-1">
          <span className={`font-medium text-sm ${me ? 'text-blue-400' : 'text-slate-300'}`}>
            {me ? 'You' : msg.sender}
          </span>
          <span className="text-slate-200 text-sm ml-2 break-words">{msg.content}</span>
        </div>
      </div>
    )
  }

  // Private messages
  if (msg.type === 'PRIVATE') {
    const sendingTo = me
    return (
      <div className="flex gap-3 px-4 py-1.5 bg-purple-900/20 hover:bg-purple-900/30">
        <span className="text-slate-500 text-xs w-12 pt-0.5 flex-shrink-0 text-right">
          {formatTime(msg.timestamp)}
        </span>
        <div className="min-w-0 flex-1">
          <span className="text-purple-400 text-xs font-medium">
            {sendingTo ? `You → ${msg.recipient}` : `${msg.sender} (private)`}
            :
          </span>
          <span className="text-slate-200 text-sm ml-2 break-words">{msg.content}</span>
        </div>
      </div>
    )
  }

  return null
}

export default function MessageList({ messages, username }) {
  const bottomRef = useRef(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  return (
    <div className="flex-1 overflow-y-auto py-4 space-y-1">
      {messages.map((msg) => (
        <MessageRow key={msg.id} msg={msg} username={username} />
      ))}
      <div ref={bottomRef} />
    </div>
  )
}