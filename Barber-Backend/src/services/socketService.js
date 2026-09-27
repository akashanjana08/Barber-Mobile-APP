/**
 * Real-time event broadcasting engine supporting WebSocket (Socket.io) and SSE (Server-Sent Events).
 * Allows the mobile app to receive instant push events when queue status changes.
 */

let io = null;
const sseClients = new Set();

const initSocketIO = (socketIoInstance) => {
  io = socketIoInstance;

  io.on('connection', (socket) => {
    console.log(`[Socket.IO] Client connected: ${socket.id}`);

    // Join room for a specific barber queue
    socket.on('join_barber_queue', (barberName) => {
      const room = `barber:${barberName.toLowerCase().trim()}`;
      socket.join(room);
      console.log(`[Socket.IO] Client ${socket.id} joined room: ${room}`);
    });

    // Leave room
    socket.on('leave_barber_queue', (barberName) => {
      const room = `barber:${barberName.toLowerCase().trim()}`;
      socket.leave(room);
      console.log(`[Socket.IO] Client ${socket.id} left room: ${room}`);
    });

    // Join user-specific notification room
    socket.on('join_user_channel', (userId) => {
      socket.join(`user:${userId}`);
      console.log(`[Socket.IO] Client ${socket.id} subscribed to user:${userId}`);
    });

    socket.on('disconnect', () => {
      console.log(`[Socket.IO] Client disconnected: ${socket.id}`);
    });
  });

  return io;
};

// SSE Stream Support (/api/v1/queue/stream)
const registerSseClient = (req, res) => {
  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    Connection: 'keep-alive',
  });

  const clientId = Date.now();
  const client = { id: clientId, res };
  sseClients.add(client);
  console.log(`[SSE] Client connected. Total active streams: ${sseClients.size}`);

  // Send initial ping
  res.write(`data: ${JSON.stringify({ type: 'CONNECTED', message: 'SSE Live Stream Active', timestamp: Date.now() })}\n\n`);

  req.on('close', () => {
    sseClients.delete(client);
    console.log(`[SSE] Client disconnected. Remaining streams: ${sseClients.size}`);
  });
};

const broadcastQueueEvent = (barberName, eventData) => {
  const payload = {
    id: `EVT_${Date.now()}_${Math.random().toString(36).substring(2, 6).toUpperCase()}`,
    timestamp: Date.now(),
    barberName,
    ...eventData,
  };

  // 1. WebSocket Broadcast to specific barber room and general queue
  if (io) {
    const room = `barber:${barberName.toLowerCase().trim()}`;
    io.to(room).emit('queue_event', payload);
    io.emit('global_queue_event', payload);
  }

  // 2. Server-Sent Events Broadcast
  const sseData = `data: ${JSON.stringify(payload)}\n\n`;
  for (const client of sseClients) {
    try {
      client.res.write(sseData);
    } catch (err) {
      sseClients.delete(client);
    }
  }

  console.log(`[Realtime Broadcast] Barber: "${barberName}", Event: "${payload.eventType}", Msg: "${payload.message}"`);
  return payload;
};

const emitToUser = (userId, eventName, data) => {
  if (io) {
    io.to(`user:${userId}`).emit(eventName, data);
  }
};

module.exports = {
  initSocketIO,
  registerSseClient,
  broadcastQueueEvent,
  emitToUser,
};
