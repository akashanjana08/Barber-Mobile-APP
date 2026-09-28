require('dotenv').config();

const config = {
  env: process.env.NODE_ENV || 'development',
  isProduction: process.env.NODE_ENV === 'production',
  port: parseInt(
    process.env.API_PORT ||
    process.env.BACKEND_PORT ||
    (process.env.PORT && !['8080', '8000', '8081'].includes(process.env.PORT) ? process.env.PORT : '5000'),
    10
  ),
  clientUrl: process.env.CLIENT_URL || '*',

  // Database
  database: {
    uri: process.env.MONGODB_URI || 'mongodb+srv://akashbarberdb:akashbarberdb@cluster0.2yoqhwe.mongodb.net/barbercraft?retryWrites=true&w=majority&appName=Cluster0',
    options: {
      autoIndex: true,
      serverSelectionTimeoutMS: 8000,
      connectTimeoutMS: 15000,
    }
  },

  // JWT
  jwt: {
    secret: process.env.JWT_SECRET || 'barbercraft_super_secret_jwt_key_2026_x89q!',
    expiresIn: process.env.JWT_EXPIRES_IN || '7d',
    refreshSecret: process.env.REFRESH_TOKEN_SECRET || 'barbercraft_refresh_token_secret_key_2026_z89y!',
    refreshExpiresIn: process.env.REFRESH_TOKEN_EXPIRES_IN || '30d',
  },

  // Third-Party Resources & Services
  thirdParty: {
    // Payment Gateway
    payment: {
      provider: (process.env.PAYMENT_PROVIDER || 'mock').toLowerCase(),
      currency: process.env.PAYMENT_CURRENCY || 'INR',
      razorpay: {
        keyId: process.env.RAZORPAY_KEY_ID || '',
        keySecret: process.env.RAZORPAY_KEY_SECRET || '',
        webhookSecret: process.env.RAZORPAY_WEBHOOK_SECRET || '',
      },
      stripe: {
        secretKey: process.env.STRIPE_SECRET_KEY || '',
        publishableKey: process.env.STRIPE_PUBLISHABLE_KEY || '',
        webhookSecret: process.env.STRIPE_WEBHOOK_SECRET || '',
      }
    },

    // Push Notifications & SMS
    notifications: {
      provider: (process.env.NOTIFICATION_PROVIDER || 'mock').toLowerCase(),
      firebase: {
        serverKey: process.env.FCM_SERVER_KEY || '',
        projectId: process.env.FIREBASE_PROJECT_ID || 'barbercraft-app-prod',
      },
      twilio: {
        accountSid: process.env.TWILIO_ACCOUNT_SID || '',
        authToken: process.env.TWILIO_AUTH_TOKEN || '',
        phoneNumber: process.env.TWILIO_PHONE_NUMBER || '+1234567890',
      }
    },

    // Maps & Geolocation
    maps: {
      provider: (process.env.MAPS_PROVIDER || 'mock').toLowerCase(),
      googleApiKey: process.env.GOOGLE_MAPS_API_KEY || '',
      defaultLat: parseFloat(process.env.DEFAULT_LATITUDE || '28.6500'),
      defaultLng: parseFloat(process.env.DEFAULT_LONGITUDE || '77.4500'),
    },

    // Email Service
    email: {
      provider: (process.env.EMAIL_PROVIDER || 'mock').toLowerCase(),
      sendgridApiKey: process.env.SENDGRID_API_KEY || '',
      from: process.env.EMAIL_FROM || 'notifications@barbercraft.app',
    }
  },

  // Business Rules & Mobile App Logic
  businessRules: {
    realtimeTransportMode: process.env.REALTIME_TRANSPORT_MODE || 'WEBSOCKET',
    slotHoldTimeoutMinutes: parseInt(process.env.SLOT_HOLD_TIMEOUT_MINUTES || '10', 10),
    slotRecoveryDiscountPercent: parseInt(process.env.SLOT_RECOVERY_DISCOUNT_PERCENT || '20', 10),
    appointmentReminderMinutesBefore: parseInt(process.env.APPOINTMENT_REMINDER_MINUTES_BEFORE || '15', 10),
    maxSearchRadiusKm: parseFloat(process.env.MAX_SEARCH_RADIUS_KM || '15.0'),
  }
};

module.exports = config;
