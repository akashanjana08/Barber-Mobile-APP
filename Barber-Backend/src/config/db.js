const mongoose = require('mongoose');
const config = require('./env');

let isConnected = false;
let memoryServerInstance = null;
let activeUri = config.database.uri;

const connectDB = async () => {
  // 1. First attempt connecting to the configured MONGODB_URI
  try {
    const conn = await mongoose.connect(config.database.uri, {
      ...config.database.options,
      serverSelectionTimeoutMS: 2000,
    });
    isConnected = true;
    activeUri = config.database.uri;
    console.log(`[MongoDB] Connected successfully to host: ${conn.connection.host}, database: ${conn.connection.name}`);
    return conn;
  } catch (error) {
    console.warn(`[MongoDB Info] Configured URI (${config.database.uri}) unreachable (${error.message}).`);
    console.log(`[MongoDB Engine] Starting built-in standalone MongoMemoryServer for instant execution...`);

    // 2. Seamlessly spin up built-in MongoDB engine
    try {
      const { MongoMemoryServer } = require('mongodb-memory-server');
      memoryServerInstance = await MongoMemoryServer.create();
      activeUri = memoryServerInstance.getUri();

      const conn = await mongoose.connect(activeUri, {
        autoIndex: true,
      });
      isConnected = true;
      console.log(`[MongoDB Engine] Standalone MongoDB running at: ${activeUri}`);
      console.log(`[MongoDB Engine] Connected successfully. Database ready for real Mongoose operations.`);
      return conn;
    } catch (memErr) {
      console.error(`[MongoDB Error] Failed to start MongoMemoryServer: ${memErr.message}`);
      isConnected = false;
      return null;
    }
  }
};

mongoose.connection.on('disconnected', () => {
  isConnected = false;
  console.log('[MongoDB] Connection lost / disconnected');
});

mongoose.connection.on('reconnected', () => {
  isConnected = true;
  console.log('[MongoDB] Reconnected to database');
});

const getDBStatus = () => ({
  isConnected,
  isMemoryEngine: !!memoryServerInstance,
  readyState: mongoose.connection.readyState,
  host: mongoose.connection.host || null,
  name: mongoose.connection.name || null,
  uri: activeUri.replace(/\/\/([^:]+):([^@]+)@/, '//***:***@'),
});

const stopDB = async () => {
  if (mongoose.connection.readyState !== 0) {
    await mongoose.disconnect();
  }
  if (memoryServerInstance) {
    await memoryServerInstance.stop();
  }
};

module.exports = {
  connectDB,
  getDBStatus,
  stopDB,
};

