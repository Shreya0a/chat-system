import { useState } from 'react'
import UserList from './UserList'
import MessageList from './MessageList'
import MessageInput from './MessageInput'

export default function ChatRoom({ username, messages, users, onSendPublic, onSendPrivate, onDisconnect }) {
  const [dmRecipient, setDmRecipient] = useState(null)

  const handleLeave = () => {
    onDisconnect()
    window.location.reload()
  }

  return (
    <div className="w-full h-full flex">
      <UserList
        users={users}
        currentUser={username}
        onSelectUser={setDmRecipient}
        dmRecipient={dmRecipient}
        onClearDm={() => setDmRecipient(null)}
      />

      <div className="flex-1 flex flex-col min-w-0">
        <header className="bg-slate-800 border-b border-slate-700 px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <h1 className="text-white font-semibold">LAN Messenger</h1>
          </div>
          <button
            onClick={handleLeave}
            className="flex items-center gap-2 text-red-400 hover:bg-red-500/10 border border-red-500/40
                       rounded-lg px-3 py-1.5 text-xs transition-colors"
          >
            Leave
          </button>
        </header>

        <MessageList messages={messages} username={username} />

        <MessageInput
          onSendPublic={onSendPublic}
          onSendPrivate={onSendPrivate}
          dmRecipient={dmRecipient}
          onClearDm={() => setDmRecipient(null)}
        />
      </div>
    </div>
  )
}