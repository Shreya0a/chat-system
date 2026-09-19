# LAN Messenger

A small terminal chat application where two or more computers on the same
local network / Wi-Fi can talk to each other. Built entirely with the Java
standard library — `Socket`, `ServerSocket`, threads and collections, no
external dependencies.

```
                 LAN / Wi-Fi
                     │
        ┌────────────┼────────────┐
        │            │            │
        ▼            ▼            ▼
     Client A     Client B     Client C
        │            │            │
        └────────────┼────────────┘
                     ▼
              Chat Server
```

## Features

- Central server that many clients connect to over TCP
- Multiple concurrent clients (each handled on its own thread)
- Usernames with duplicate-name detection
- Public chat (`Rahul: Hello everyone!`)
- Private messages (`/msg`)
- Join / leave notifications
- Users list (`/users`)
- Chat history for the current session — sent to newcomers when they join
- No external dependencies, works on plain Java 8+

## Repository layout

```
src/com/lanmessenger/
├── Server.java          entry point: runs the chat server
├── Client.java          entry point: runs an interactive client
├── server/
│   ├── ChatServer.java  accepts connections, routes messages, keeps history
│   └── ClientHandler.java  one thread per connected client
├── client/
│   └── ChatClient.java  console UI, reader thread + command parsing
├── model/
│   └── Message.java     message object + line-based wire protocol
└── util/
    └── NetworkUtils.java  local IP detection, port argument parsing
```

## Build

From the repository root:

```
javac -d out src/com/lanmessenger/**/*.java
```

## Run

**1. Start the server** (Computer A):

```
java -cp out com.lanmessenger.Server
# [LAN Messenger] Host IP : 192.168.1.10
# [LAN Messenger] Port    : 5000
```

A custom port works too: `java -cp out com.lanmessenger.Server 8000`

**2. Start clients** (Computer B, C, ... — or several terminals on the same PC):

```
java -cp out com.lanmessenger.Client 192.168.1.10
```

Optional: `java -cp out com.lanmessenger.Client 192.168.1.10 5000 Rahu1`
If no username is given it is prompted for at startup.

## Commands

| Command            | What it does                         |
|--------------------|--------------------------------------|
| `any text`         | Send a public message                |
| `/msg <name> <text>` | Send a private message to `<name>` |
| `/users`           | List currently online users          |
| `/help`            | Show these commands                  |
| `/quit` or `/exit` | Leave the chat                       |

## Example session

```
[SERVER] Welcome, Rahul!
[SERVER] Type /help to see available commands. Users online: Rahul

Rahul:
Hello everyone!

You:
Hi Rahul!

[SERVER] Priya joined the chat.

[14:02] Rahul: Hello everyone!
[14:02] You: Hi Rahul!
```

> Note: the final two timestamped lines above show what a newcomer (Priya)
> receives as **chat history** when she joins mid-session.

## Firewall/network notes

- The server listens on port `5000` (configurable). If clients cannot connect,
  allow the Java process through the Windows/macOS/Linux firewall.
- All machines must be on the same subnet / Wi-Fi network.
- Find the server IP with `ipconfig` (Windows) or `ifconfig` (Linux/macOS) if
  the default-host detection picks the wrong interface.

## Verification (passed)

Backend compiles and the frontend builds cleanly:

```
javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
npm run build
```

Restart the backend and refresh both browser tabs:

```
# backend terminal
Ctrl+C
javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp out com.lanmessenger.Server
```