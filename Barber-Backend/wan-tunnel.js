const localtunnel = require('localtunnel');
const fs = require('fs');

async function launch() {
  console.log('[WAN Tunnel] Requesting public WAN tunnel on port 5000...');
  try {
    const tunnel = await localtunnel({ port: 5000 });
    console.log(`=============================================================================`);
    console.log(`🌐 BarberCraft API is now accessible on WAN / Internet!`);
    console.log(`🔗 Public URL: ${tunnel.url}`);
    console.log(`📡 Health Check: ${tunnel.url}/api/v1/health`);
    console.log(`🏪 Shops API: ${tunnel.url}/api/v1/shops`);
    console.log(`=============================================================================`);

    fs.writeFileSync('/tmp/wan_url.txt', tunnel.url);

    tunnel.on('close', () => {
      console.log('[WAN Tunnel] Tunnel closed. Reconnecting...');
      setTimeout(launch, 3000);
    });

    tunnel.on('error', (err) => {
      console.error('[WAN Tunnel Error]', err);
    });
  } catch (err) {
    console.error('[WAN Tunnel Startup Error]', err);
    setTimeout(launch, 5000);
  }
}

launch();
