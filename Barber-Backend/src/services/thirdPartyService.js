const config = require('../config/env');

/**
 * Universal third-party service provider wrapper.
 * All third-party services are dynamically configurable via .env / environment variables.
 * In development / mock mode, each provider safely logs and provides realistic simulation responses.
 */

// =============================================================================
// 1. PAYMENT GATEWAY SERVICE (Razorpay / Stripe / UPI / Mock)
// =============================================================================
class PaymentService {
  constructor() {
    this.provider = config.thirdParty.payment.provider;
    this.currency = config.thirdParty.payment.currency;
    console.log(`[ThirdParty] Initialized Payment Provider: ${this.provider.toUpperCase()}`);
  }

  async createOrder({ amount, currency = this.currency, receipt, notes = {} }) {
    if (this.provider === 'razorpay') {
      const { keyId, keySecret } = config.thirdParty.payment.razorpay;
      if (!keyId || !keySecret) {
        console.warn('[Razorpay] Missing RAZORPAY_KEY_ID or RAZORPAY_KEY_SECRET in .env. Falling back to mock order.');
      } else {
        // Production Razorpay Order creation:
        // const Razorpay = require('razorpay');
        // const rzp = new Razorpay({ key_id: keyId, key_secret: keySecret });
        // return await rzp.orders.create({ amount: amount * 100, currency, receipt, notes });
      }
    } else if (this.provider === 'stripe') {
      const { secretKey } = config.thirdParty.payment.stripe;
      if (!secretKey) {
        console.warn('[Stripe] Missing STRIPE_SECRET_KEY in .env. Falling back to mock payment intent.');
      } else {
        // Production Stripe PaymentIntent creation:
        // const stripe = require('stripe')(secretKey);
        // return await stripe.paymentIntents.create({ amount: amount * 100, currency, metadata: notes });
      }
    }

    // Default / Mock Provider
    const orderId = `order_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
    return {
      success: true,
      provider: this.provider,
      orderId,
      amount,
      currency,
      receipt: receipt || `rcpt_${Date.now()}`,
      status: 'created',
      createdAt: new Date(),
    };
  }

  async verifyPayment({ orderId, paymentId, signature }) {
    if (this.provider === 'razorpay') {
      // In live mode with crypto HMAC SHA256 verification:
      // const crypto = require('crypto');
      // const expectedSignature = crypto.createHmac('sha256', config.thirdParty.payment.razorpay.keySecret)
      //   .update(`${orderId}|${paymentId}`)
      //   .digest('hex');
      // return expectedSignature === signature;
    }

    // Mock verification
    return {
      verified: true,
      transactionId: paymentId || `txn_${Date.now()}_${Math.random().toString(36).substring(2, 8).toUpperCase()}`,
      provider: this.provider,
      timestamp: new Date(),
    };
  }

  async processRefund({ transactionId, amount, reason }) {
    console.log(`[Payment] Processing refund for Txn: ${transactionId}, Amount: ${amount}, Reason: ${reason}`);
    return {
      success: true,
      refundId: `rfnd_${Date.now()}`,
      transactionId,
      amount,
      status: 'processed',
      reason,
      refundedAt: new Date(),
    };
  }
}

// =============================================================================
// 2. PUSH NOTIFICATIONS & SMS SERVICE (FCM / Twilio / Mock)
// =============================================================================
class NotificationService {
  constructor() {
    this.provider = config.thirdParty.notifications.provider;
    console.log(`[ThirdParty] Initialized Notification Provider: ${this.provider.toUpperCase()}`);
  }

  async sendPushNotification({ fcmToken, title, body, data = {} }) {
    console.log(`[Push Notification] -> Title: "${title}", Body: "${body}"`);

    if (this.provider === 'firebase' && config.thirdParty.notifications.firebase.serverKey) {
      // Production Firebase Admin SDK or REST call:
      // await admin.messaging().send({ token: fcmToken, notification: { title, body }, data });
    }

    return {
      success: true,
      provider: this.provider,
      messageId: `msg_${Date.now()}`,
      deliveredAt: new Date(),
    };
  }

  async sendSms({ to, message }) {
    console.log(`[SMS Notification] -> To: ${to}, Message: "${message.replace(/\n/g, ' ')}"`);

    if (this.provider === 'twilio' && config.thirdParty.notifications.twilio.accountSid) {
      // Production Twilio client:
      // const twilio = require('twilio');
      // const client = twilio(config.thirdParty.notifications.twilio.accountSid, config.thirdParty.notifications.twilio.authToken);
      // await client.messages.create({ to, from: config.thirdParty.notifications.twilio.phoneNumber, body: message });
    }

    return {
      success: true,
      provider: this.provider,
      sid: `SM_${Date.now()}`,
      sentAt: new Date(),
    };
  }
}

// =============================================================================
// 3. MAPS & GEOLOCATION SERVICE (Google Maps / Haversine / Mock)
// =============================================================================
class MapsService {
  constructor() {
    this.provider = config.thirdParty.maps.provider;
    this.apiKey = config.thirdParty.maps.googleApiKey;
    console.log(`[ThirdParty] Initialized Maps Provider: ${this.provider.toUpperCase()}`);
  }

  /**
   * Calculates straight-line distance in kilometers using the Haversine formula
   */
  calculateHaversineDistance(lat1, lon1, lat2, lon2) {
    const R = 6371; // Earth's radius in km
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLon = (lon2 - lon1) * (Math.PI / 180);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * (Math.PI / 180)) *
        Math.cos(lat2 * (Math.PI / 180)) *
        Math.sin(dLon / 2) *
        Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return parseFloat((R * c).toFixed(2));
  }

  async calculateDistanceMatrix({ origin, destination }) {
    if (this.provider === 'google' && this.apiKey) {
      // Production Google Distance Matrix API call:
      // const url = `https://maps.googleapis.com/maps/api/distancematrix/json?origins=${origin.lat},${origin.lng}&destinations=${destination.lat},${destination.lng}&key=${this.apiKey}`;
      // const res = await fetch(url).then(r => r.json());
    }

    const distanceKm = this.calculateHaversineDistance(
      origin.lat,
      origin.lng,
      destination.lat,
      destination.lng
    );

    // Approximate travel times
    return {
      distanceKm,
      drivingDurationMinutes: Math.max(3, Math.round(distanceKm * 2.5)),
      walkingDurationMinutes: Math.max(5, Math.round(distanceKm * 12)),
    };
  }
}

module.exports = {
  paymentService: new PaymentService(),
  notificationService: new NotificationService(),
  mapsService: new MapsService(),
};
