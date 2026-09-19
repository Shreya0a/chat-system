import { useState } from 'react'

export default function LoginForm({ onConnect, serverHost, setServerHost }) {
  const [name, setName] = useState('')

  const handleSubmit = (e) => {
    e.preventDefault()
    const trimmed = name.trim()
    if (trimmed) onConnect(trimmed)
  }

  return (
    <div className="bg-slate-800 rounded-xl shadow-xl p-8 w-full max-w-sm space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-bold text-white">LAN Messenger</h1>
        <p className="text-slate-400 text-sm mt-1">Chat with anyone on your local network</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-xs text-slate-400 mb-1">Server IP</label>
          <input
            type="text"
            value={serverHost}
            onChange={(e) => setServerHost(e.target.value)}
            className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-sm text-white
                       focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="e.g. 192.168.1.10"
          />
        </div>

        <div>
          <label className="block text-xs text-slate-400 mb-1">Username</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-sm text-white
                       focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="Pick a name"
            autoFocus
            maxLength={20}
          />
        </div>

        <button
          type="submit"
          disabled={!name.trim()}
          className="w-full bg-blue-600 hover:bg-blue-500 disabled:bg-slate-600 disabled:cursor-not-allowed
                     text-white font-medium rounded-lg px-4 py-2 text-sm transition-colors"
        >
          Join Chat
        </button>
      </form>
    </div>
  )
}