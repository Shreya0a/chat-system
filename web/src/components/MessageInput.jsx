import { useState } from 'react'

export default function MessageInput({ onSendPublic, onSendPrivate, dmRecipient, onClearDm }) {
  const [text, setText] = useState('')

  const handleSubmit = (e) => {
    e.preventDefault()
    const value = text.trim()
    if (!value) return
    if (dmRecipient) {
      onSendPrivate(dmRecipient, value)
    } else {
      onSendPublic(value)
    }
    setText('')
  }

  return (
    <form onSubmit={handleSubmit} className="bg-slate-800 border-t border-slate-700 p-3 flex gap-2 items-center">
      {dmRecipient ? (
        <button
          type="button"
          onClick={onClearDm}
          className="flex-shrink-0 px-3 py-2 rounded-lg bg-purple-600/30 text-purple-300 text-xs
                     hover:bg-purple-600/50 transition-colors"
          title="Stop private chat"
        >
          @{dmRecipient} ✕
        </button>
      ) : (
        <span className="flex-shrink-0 text-slate-500 text-lg px-1">▶</span>
      )}

      <input
        type="text"
        value={text}
        onChange={(e) => setText(e.target.value)}
        className={`flex-1 bg-slate-700 border border-slate-600 rounded-lg px-4 py-2 text-sm text-white
                    focus:outline-none focus:ring-2 focus:bg-slate-600/50 transition-all
                    ${dmRecipient ? 'focus:ring-purple-500 border-purple-500/50' : 'focus:ring-blue-500'}`}
        placeholder={dmRecipient ? `Message ${dmRecipient}...` : 'Type a message...'}
        autoFocus
        maxLength={2000}
      />

      <button
        type="submit"
        disabled={!text.trim()}
        className="flex-shrink-0 bg-blue-600 hover:bg-blue-500 disabled:bg-slate-600 disabled:cursor-not-allowed
                   text-white font-medium rounded-lg px-5 py-2 text-sm transition-colors"
      >
        Send
      </button>
    </form>
  )
}