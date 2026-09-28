const mongoose = require('mongoose');
const config = require('./env');

let isConnected = false;
let memoryServerInstance = null;
let activeUri = config.database.uri;

const connectDB = async () => {
  // 1. First attempt connecting to the configured MONGODB_URI (e.g. MongoDB Atlas)
  try {
    const isAtlas = config.database.uri.includes('mongodb+srv://') || config.database.uri.includes('.mongodb.net');
    console.log(`[MongoDB] Connecting to ${isAtlas ? 'MongoDB Atlas Cluster' : 'MongoDB'}...`);
    const conn = await mongoose.connect(config.database.uri, {
      ...config.database.options,
      serverSelectionTimeoutMS: isAtlas ? 8000 : 3000,
    });
    isConnected = true;
    activeUri = config.database.uri;
    console.log(`[MongoDB] Connected successfully to host: ${conn.connection.host}, database: ${conn.connection.name}`);
    return conn;
  } catch (error) {
    const isAtlas = config.database.uri.includes('mongodb+srv://');
    console.warn(`[MongoDB Warning] Configured URI unreachable (${error.message}).`);
    if (isAtlas) {
      console.warn(`[MongoDB Atlas] ⚠️ ACTION REQUIRED IN MONGODB ATLAS CONSOLE:`);
      console.warn(`  1. Log in to https://cloud.mongodb.com`);
      console.warn(`  2. In the left menu, click 'Network Access' under Security`);
      console.warn(`  3. Click '+ ADD IP ADDRESS'`);
      console.warn(`  4. Click 'ALLOW ACCESS FROM ANYWHERE' (0.0.0.0/0) and click 'Confirm'`);
      console.warn(`  5. Once saved, the Atlas cluster will allow remote connections immediately!`);
    }
    console.log(`[MongoDB Engine] Starting built-in standalone MongoMemoryServer as fallback...`);

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
  type: memoryServerInstance ? 'Built-in MongoDB Engine' : 'MongoDB Atlas Cloud Cluster',
  targetCluster: 'cluster0.2yoqhwe.mongodb.net',
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

const reconnectDB = async (customUri) => {
  const uriToUse = customUri || config.database.uri;
  console.log(`[MongoDB Reconnect] Attempting direct connection to: ${uriToUse.replace(/\/\/([^:]+):([^@]+)@/, '//***:***@')}`);

  try {
    if (mongoose.connection.readyState !== 0) {
      await mongoose.disconnect();
    }

    const conn = await mongoose.connect(uriToUse, {
      ...config.database.options,
      serverSelectionTimeoutMS: 10000,
    });

    isConnected = true;
    activeUri = uriToUse;
    console.log(`[MongoDB Atlas] Successfully connected to host: ${conn.connection.host}, database: ${conn.connection.name}`);

    // Stop fallback in-memory server if it was active
    if (memoryServerInstance) {
      try {
        await memoryServerInstance.stop();
        memoryServerInstance = null;
      } catch (_) {}
    }

    // Auto-seed if database is freshly created
    try {
      const { seedDatabase } = require('../seed/seeder');
      await seedDatabase();
    } catch (seedErr) {
      console.warn('[MongoDB Seed]', seedErr.message);
    }

    return { success: true, message: 'Connected to MongoDB Atlas', host: conn.connection.host, database: conn.connection.name };
  } catch (err) {
    console.warn(`[MongoDB Reconnect Error] ${err.message}`);
    // If Atlas connection fails, reconnect back to in-memory fallback so server stays up
    if (!isConnected) {
      await connectDB();
    }
    return { success: false, error: err.message, status: getDBStatus() };
  }
};

module.exports = {
  connectDB,
  reconnectDB,
  getDBStatus,
  stopDB,
};

