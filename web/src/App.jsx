import { useState } from 'react'
import useChat from './hooks/useChat'
import LoginForm from './components/LoginForm'
import ChatRoom from './components/ChatRoom'

const WS_PORT = 8080

export default function App() {
  const { messages, users, connected, username, connect, sendPublic, sendPrivate, disconnect } = useChat()
  const [serverHost, setServerHost] = useState(window.location.hostname )

  const handleConnect = (name) => {
    const wsUrl = `ws://${serverHost}:${WS_PORT}/ws`
    connect(name, wsUrl)
  }

  if (!connected) {
    return (
      <div className="w-full h-full flex items-center justify-center">
        <LoginForm
          onConnect={handleConnect}
          serverHost={serverHost}
          setServerHost={setServerHost}
        />
      </div>
    )
  }

  return (
    <ChatRoom
      username={username}
      messages={messages}
      users={users}
      onSendPublic={sendPublic}
      onSendPrivate={sendPrivate}
      onDisconnect={disconnect}
    />
  )
}