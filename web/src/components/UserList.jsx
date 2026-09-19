import { useState } from 'react'

export default function UserList({ users, currentUser, onSelectUser, dmRecipient, onClearDm }) {
  const otherUsers = users.filter((u) => u !== currentUser)

  return (
    <aside className="bg-slate-800 border-r border-slate-700 w-56 flex-shrink-0 flex flex-col overflow-hidden">
      <div className="p-3 border-b border-slate-700">
        <h2 className="text-xs font-semibold text-slate-400 uppercase tracking-wide">
          Online ({users.length})
        </h2>
      </div>

      <div className="flex-1 overflow-y-auto py-2">
        {otherUsers.length === 0 && (
          <p className="text-slate-500 text-xs px-3 italic">No one else online</p>
        )}
        {otherUsers.map((user) => (
          <button
            key={user}
            onClick={() => onSelectUser(user)}
            className={`w-full text-left px-3 py-1.5 text-sm flex items-center gap-2 transition-colors
                        ${user === dmRecipient
                          ? 'bg-blue-600/30 text-blue-200'
                          : 'hover:bg-slate-700 text-slate-200'
                        }`}
          >
            <span className="w-2 h-2 rounded-full bg-emerald-400 flex-shrink-0" />
            {user}
          </button>
        ))}
      </div>

      {dmRecipient && (
        <div className="p-3 border-t border-slate-700 text-xs">
          <span className="text-purple-400">Private chat with </span>
          <span className="text-white font-medium">{dmRecipient}</span>
          <button
            onClick={onClearDm}
            className="ml-1 text-slate-400 hover:text-white transition-colors"
          >
            ✕
          </button>
        </div>
      )}
    </aside>
  )
}
