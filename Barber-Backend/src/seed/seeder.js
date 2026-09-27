const mongoose = require('mongoose');
const { connectDB } = require('../config/db');
const { seedInitialData } = require('./seedData');

const run = async () => {
  try {
    await connectDB();
    await seedInitialData();
    console.log('[Seeder] Seeding process complete.');
    process.exit(0);
  } catch (error) {
    console.error('[Seeder] Error during seeding:', error);
    process.exit(1);
  }
};

run();
