const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const morgan = require('morgan');

const config = require('./src/config/env');
const { connectDB, getDBStatus, stopDB } = require('./src/config/db');
const { initSocketIO } = require('./src/services/socketService');
const apiRouter = require('./src/routes/api');
const errorHandler = require('./src/middleware/errorHandler');
const { seedInitialData } = require('./src/seed/seedData');

const app = express();
const server = http.createServer(app);

// 1. CORS & Middlewares
app.use(
  cors({
    origin: '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With'],
  })
);

app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

if (config.env !== 'test') {
  app.use(morgan('dev'));
}

// 2. Initialize Socket.IO Real-time Engine
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST'],
  },
});
initSocketIO(io);

// 3. Root Interactive Welcome & API Discovery Page
app.get('/', (req, res) => {
  res.status(200).json({
    name: 'BarberCraft Backend API',
    version: '1.0.0',
    description: 'REST & Real-time WebSockets API for BarberCraft Mobile Application',
    status: 'online',
    timestamp: new Date(),
    database: getDBStatus(),
    endpoints: {
      health: 'GET /api/v1/health',
      auth: {
        register: 'POST /api/v1/auth/register',
        login: 'POST /api/v1/auth/login',
        me: 'GET /api/v1/auth/me',
        profile: 'PUT /api/v1/auth/profile',
      },
      shops: {
        list: 'GET /api/v1/shops (filters: search, category, maxDistanceKm, minRating, maxPrice, openNow, sortBy)',
        detail: 'GET /api/v1/shops/:id',
        favorite: 'POST /api/v1/shops/:id/favorite',
        slots: 'GET /api/v1/shops/:id/slots',
        reviews: 'GET & POST /api/v1/shops/:id/reviews',
      },
      bookings: {
        hold: 'POST /api/v1/bookings/hold (10-min temporary slot lock with 409 conflict detection)',
        create: 'POST /api/v1/bookings',
        list: 'GET /api/v1/bookings',
        detail: 'GET /api/v1/bookings/:id',
        cancel: 'POST /api/v1/bookings/:id/cancel (Triggers Smart Recovery / Slot Offer)',
        reschedule: 'POST /api/v1/bookings/:id/reschedule',
        noShow: 'POST /api/v1/bookings/:id/no-show',
        reminder: 'POST /api/v1/bookings/:id/reminder (15-min notification & SMS trigger)',
      },
      realtimeQueue: {
        position: 'GET /api/v1/queue/position?bookingId=BK10234&barberName=Rahul%20Sharma&scheduledTime=5:30%20PM',
        barberQueue: 'GET /api/v1/queue/barber/:barberName',
        summaries: 'GET /api/v1/queue/summaries',
        sseStream: 'GET /api/v1/queue/stream (Server-Sent Events live stream)',
        startService: 'POST /api/v1/queue/start',
        completeService: 'POST /api/v1/queue/complete',
        checkIn: 'POST /api/v1/queue/checkin',
        delay: 'POST /api/v1/queue/delay',
        walkIn: 'POST /api/v1/queue/walkin',
        reset: 'POST /api/v1/queue/reset (Benchmark queue state)',
      },
      smartRecovery: {
        activeOffers: 'GET /api/v1/offers/active',
        claim: 'POST /api/v1/offers/:id/claim',
        evaluateCandidates: 'GET /api/v1/offers/candidates/evaluate',
        simulate: 'POST /api/v1/offers/trigger-simulation',
      },
      notifications: {
        list: 'GET /api/v1/notifications',
        markRead: 'PUT /api/v1/notifications/:id/read',
        markAllRead: 'PUT /api/v1/notifications/read-all',
      },
      thirdPartyPayments: {
        createOrder: 'POST /api/v1/payments/create-order',
        verify: 'POST /api/v1/payments/verify',
        configStatus: 'GET /api/v1/payments/config/third-party',
      },
    },
  });
});

// 4. Mount API Routes
app.use('/api/v1', apiRouter);
app.use('/api', apiRouter); // Alias

// 5. 404 Handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: `Cannot ${req.method} ${req.originalUrl}. Route not found.`,
  });
});

// 6. Centralized Error Handler
app.use(errorHandler);

// 7. Start Server
const PORT = config.port;
server.listen(PORT, async () => {
  console.log(`=============================================================================`);
  console.log(`🚀 BarberCraft API Server running in ${config.env.toUpperCase()} mode on port ${PORT}`);
  console.log(`📡 Base URL: http://localhost:${PORT}`);
  console.log(`⚡ WebSocket & SSE Engine: ACTIVE`);
  console.log(`=============================================================================`);

  // Connect to DB and seed initial data if needed
  try {
    const conn = await connectDB();
    if (conn) {
      await seedInitialData();
    }
  } catch (err) {
    console.error('[Startup DB Error]', err.message);
  }
});

// Process Handlers
const gracefulShutdown = async () => {
  console.log('[Server] Gracefully shutting down...');
  server.close(async () => {
    await stopDB();
    console.log('[Server] Closed successfully.');
    process.exit(0);
  });
};

process.on('SIGINT', gracefulShutdown);
process.on('SIGTERM', gracefulShutdown);

process.on('unhandledRejection', (err) => {
  console.error('[Unhandled Rejection]', err);
});

process.on('uncaughtException', (err) => {
  console.error('[Uncaught Exception]', err);
});

module.exports = { app, server };
