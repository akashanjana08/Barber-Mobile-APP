const { server } = require('./server');
const config = require('./src/config/env');
const fs = require('fs');

async function startWANTunnel() {
  try {
    const { tunnelmole } = require('tunnelmole');
    console.log('[WAN] Initializing persistent WAN Tunnel...');
    const url = await tunnelmole({ port: config.port });

    console.log(`\n=============================================================================`);
    console.log(`🌐 BARBERCRAFT API IS PUBLISHED ON WAN / INTERNET!`);
    console.log(`🔗 Public URL: ${url}`);
    console.log(`📡 Health Check: ${url}/api/v1/health`);
    console.log(`🏪 Shops API: ${url}/api/v1/shops`);
    console.log(`⏰ Queue Summaries: ${url}/api/v1/queue/summaries`);
    console.log(`=============================================================================\n`);

    fs.writeFileSync('/tmp/wan_public_url.txt', url);
  } catch (err) {
    console.error('[WAN Tunnel Error]', err.message);
  }
}

// Start tunnel after server starts
setTimeout(startWANTunnel, 2000);
