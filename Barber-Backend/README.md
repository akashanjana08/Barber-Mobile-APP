# BarberCraft Backend API (Node.js + Express + MongoDB)

High-performance RESTful & Real-time WebSockets / SSE backend for the **BarberCraft** Android mobile application.

---

## 📌 Features & Highlights

1. **Configurable MongoDB Database**:
   - Connection string is completely configurable via `MONGODB_URI` in `.env` (supports local MongoDB or cloud MongoDB Atlas).
   - Designed around the exact mobile app flows, models, and real-time state requirements.
   - Built-in automatic initial data seeder and CLI seed script (`npm run seed`).

2. **Configurable Third-Party Resources**:
   - **Payment Gateway**: Configurable provider (`mock`, `razorpay`, `stripe`, `upi`) with API keys, secrets, webhooks, and automatic refund triggers.
   - **Push Notifications & SMS**: Configurable provider (`mock`, `firebase`, `twilio`) for appointment reminders and last-minute deal alerts.
   - **Maps & Geolocation**: Configurable provider (`mock`, `google`, `mapbox`) with Haversine distance calculations and Google Maps Distance Matrix support.
   - **Email Service**: Configurable provider (`mock`, `sendgrid`, `smtp`).

3. **5 Real-Time Queue Engine Business Rules**:
   - **Rule 1 (Barber-Specific)**: Strict filtering ensuring customers booked with other barbers never affect a specific barber's queue.
   - **Rule 2 (Relevant Bookings Only)**: Only active states (`CONFIRMED`, `CHECKED_IN`, `IN_PROGRESS`, `DELAYED`) contribute to waiting counts; completed, cancelled, or expired bookings are instantly excluded.
   - **Rule 3 (Strict Precedence)**: Accurately counts customers scheduled strictly *before* the current customer's appointment time (plus the active customer currently in the chair).
   - **Rule 4 (Service Duration Considered)**: Accumulates actual scheduled durations, in-chair remaining minutes, and delays to produce exact estimated waiting times.
   - **Rule 5 (Interactive Real-Time Mutations)**: Live push broadcasts via **Socket.IO** and **Server-Sent Events (SSE)** when a barber starts a cut, completes a service, a customer checks in, unexpected delays occur, or walk-ins are added.

4. **Smart Recovery & Flash Slot Engine**:
   - Automatic generation of 20% discount flash offers when appointments are cancelled or marked no-show.
   - Algorithmic candidate targeting: filters eligible customers within 5 km active within 48 hours with location permission.
   - 10-minute temporary slot hold (`/api/v1/bookings/hold`) with HTTP 409 conflict detection to prevent double-booking.

---

## 🏗️ Directory Structure

```
Barber-Backend/
├── .env.example                # Template of all configurable environment variables
├── .env                        # Local active environment configuration
├── package.json                # Project dependencies and npm scripts
├── server.js                   # Application entry point, Express & Socket.IO server
└── src/
    ├── config/
    │   ├── db.js               # MongoDB Mongoose connection handler
    │   └── env.js              # Centralized environment configuration loader
    ├── controllers/
    │   ├── authController.js   # JWT authentication & profile management
    │   ├── bookingController.js# Slot hold, checkout, cancellation, reschedule, no-show
    │   ├── notificationController.js # In-app notification management
    │   ├── paymentController.js# Order creation, verification, refunds & third-party status
    │   ├── queueController.js  # Real-time queue positions, summaries & mutations
    │   ├── recoveryController.js # Smart Recovery flash offers & candidate evaluations
    │   └── shopController.js   # Shop listings, filters, details, slots & reviews
    ├── middleware/
    │   ├── authMiddleware.js   # JWT validation & optional demo auth fallback
    │   └── errorHandler.js     # Centralized error handler
    ├── models/
    │   ├── Barber.js           # Barber schema & availability
    │   ├── BarberQueue.js      # Queue items with status, durations & delays
    │   ├── BarberShop.js       # Shop details, geo-location, services & reviews
    │   ├── Booking.js          # Confirmed bookings, payment status & reminders
    │   ├── Notification.js     # In-app notifications
    │   ├── Review.js           # Customer ratings and reviews
    │   ├── Service.js          # Salon grooming services catalog
    │   ├── SlotOffer.js        # Recovered flash slot offers with candidate targeting
    │   ├── SlotReservation.js  # Temporary 10-minute slot locks (TTL)
    │   └── User.js             # User profiles, passwords, locations & favorites
    ├── routes/
    │   ├── api.js              # Master API router mounting all modules
    │   ├── authRoutes.js       # /api/v1/auth
    │   ├── bookingRoutes.js    # /api/v1/bookings
    │   ├── notificationRoutes.js # /api/v1/notifications
    │   ├── paymentRoutes.js    # /api/v1/payments
    │   ├── queueRoutes.js      # /api/v1/queue
    │   ├── recoveryRoutes.js   # /api/v1/offers
    │   └── shopRoutes.js       # /api/v1/shops
    ├── seed/
    │   ├── seedData.js         # Mobile app initial dataset
    │   └── seeder.js           # Database seeder execution script
    └── services/
        ├── queueService.js     # Core queue calculation algorithms & mutations
        ├── recoveryService.js  # Smart slot recovery & candidate evaluation
        ├── socketService.js    # Socket.IO & SSE real-time event broadcaster
        └── thirdPartyService.js# Universal third-party adapters (Payment, SMS, Maps)
```

---

## ⚙️ Configuration & Environment Variables

Copy `.env.example` to `.env` and configure your credentials:

```bash
cp .env.example .env
```

| Key | Description | Default / Example |
| :--- | :--- | :--- |
| `PORT` | HTTP Server port | `5000` |
| `MONGODB_URI` | MongoDB connection string | `mongodb://127.0.0.1:27017/barbercraft` |
| `JWT_SECRET` | Secret key for JWT signing | `barbercraft_super_secret_jwt_key_2026_x89q!` |
| `JWT_EXPIRES_IN` | Access token lifespan | `7d` |
| `PAYMENT_PROVIDER` | Payment provider (`mock` \| `razorpay` \| `stripe` \| `upi`) | `mock` |
| `RAZORPAY_KEY_ID` | Razorpay Key ID (if using Razorpay) | `rzp_test_...` |
| `RAZORPAY_KEY_SECRET` | Razorpay Secret (if using Razorpay) | `...` |
| `STRIPE_SECRET_KEY` | Stripe Secret Key (if using Stripe) | `sk_test_...` |
| `NOTIFICATION_PROVIDER` | Push/SMS provider (`mock` \| `firebase` \| `twilio`) | `mock` |
| `FCM_SERVER_KEY` | Firebase Cloud Messaging server key | `...` |
| `TWILIO_ACCOUNT_SID` | Twilio Account SID | `AC...` |
| `TWILIO_AUTH_TOKEN` | Twilio Auth Token | `...` |
| `TWILIO_PHONE_NUMBER` | Twilio registered phone number | `+1234567890` |
| `MAPS_PROVIDER` | Geolocation provider (`mock` \| `google` \| `mapbox`) | `mock` |
| `GOOGLE_MAPS_API_KEY` | Google Maps Platform API key | `AIzaSy...` |
| `SLOT_HOLD_TIMEOUT_MINUTES`| Duration to hold slot before checkout | `10` |
| `SLOT_RECOVERY_DISCOUNT_PERCENT`| Discount on recovered flash slots | `20` |

---

## 🚀 Getting Started

### 1. Install Dependencies
```bash
npm install
```

### 2. Seed Initial Database Data
```bash
npm run seed
```
This populates MongoDB with the exact initial shops (`Royal Barber Club & Lounge`, `Fade & Blade Studio`, `Urban Gentleman`, `The Barber's Den`), barbers (`Rahul Sharma`, `Amit Verma`, `Devendra Rawat`), initial bookings, and benchmark queue items matching the Android mobile application.

### 3. Start the Server
```bash
npm start
# or for auto-reloading development:
npm run dev
```

The server will start at `http://localhost:5000`.

---

## 📡 API Reference Overview

### Base URL: `http://localhost:5000/api/v1`

### 1. Authentication (`/api/v1/auth`)
- `POST /register`: Register a new user (`name`, `email`, `phone`, `password`, `location`).
- `POST /login`: Log in with email or phone and receive JWT + Refresh Token.
- `GET /me`: Get current authenticated user profile and saved favorites.
- `PUT /profile`: Update name, phone, coordinates, or FCM token.
- `POST /refresh-token`: Exchange refresh token for fresh access token.

### 2. Barber Shops & Services (`/api/v1/shops`)
- `GET /`: List shops with rich filtering and sorting:
  - Query parameters:
    - `search`: Full-text search by shop name, address, or service name.
    - `category`: Filter by service category (`Haircut`, `Beard`, `Facial`, `Hair Spa`, `Hair Color`, `Kids Haircut`).
    - `maxDistanceKm`: Maximum distance in km (e.g. `5.0`).
    - `minRating`: Minimum rating threshold (e.g. `4.5`).
    - `maxPrice`: Maximum starting price (e.g. `500`).
    - `openNow`: `true` to show open shops only.
    - `availableToday`: `true` to filter out shops with no slots today.
    - `sortBy`: `RECOMMENDED`, `NEAREST`, `HIGHEST_RATED`, `LOWEST_PRICE`.
- `GET /:id`: Full shop details, barbers list, services catalog, and reviews.
- `POST /:id/favorite`: Toggle favorite bookmark for current user.
- `GET /:id/slots`: Returns available time slots grouped by Morning, Afternoon, and Evening.
- `GET /:id/reviews`: List customer reviews.
- `POST /:id/reviews`: Post a new review with rating and comment.

### 3. Bookings & Checkout (`/api/v1/bookings`)
- `POST /hold`: Temporary 10-minute slot hold with HTTP 409 conflict detection.
- `POST /`: Confirm booking, calculate 5% tax, and insert into the live Barber Queue.
- `GET /`: List user bookings (filter by `status=UPCOMING`, `COMPLETED`, `CANCELLED`, `NO_SHOW`).
- `GET /:id`: Booking details.
- `POST /:id/cancel`: Cancel booking, trigger refund, and automatically convert the freed slot into a **Smart Recovery 20% OFF Flash Offer**.
- `POST /:id/reschedule`: Reschedule booking date/time and update queue positioning.
- `POST /:id/no-show`: Mark customer as no-show, freeing the chair and generating a recovery offer.
- `POST /:id/reminder`: Trigger 15-minute appointment reminder in-app notification & SMS.

### 4. Real-Time Barber Queue (`/api/v1/queue`)
- `GET /position`: Returns real-time position for a booking:
  ```json
  {
    "bookingId": "BK10234",
    "barberName": "Rahul Sharma",
    "scheduledTime": "5:30 PM",
    "customersBeforeCount": 2,
    "estimatedWaitingMinutes": 30,
    "activeCustomer": { "customerName": "Customer A (Vikram M.)", "remainingMinutes": 15 },
    "customersAhead": [ ... ],
    "totalWaitingForBarber": 3,
    "isYourTurn": false
  }
  ```
- `GET /summaries`: Returns active waiting count and estimated wait minutes for each barber.
- `GET /barber/:barberName`: List active queue items for a specific barber.
- `GET /stream`: Server-Sent Events (SSE) live push stream.
- `POST /start`: Start service for customer in chair (`IN_PROGRESS`).
- `POST /complete`: Mark current service `COMPLETED` and advance next customer.
- `POST /checkin`: Customer arrives and checks in (`CHECKED_IN`).
- `POST /delay`: Broadcast service delay alert and recalibrate wait times.
- `POST /walkin`: Insert walk-in customer into the queue.
- `POST /reset`: Reset queue to benchmark demo state.

### 5. Smart Recovery & Flash Offers (`/api/v1/offers`)
- `GET /active`: List active recovered slot offers (e.g., 20% OFF).
- `GET /candidates/evaluate`: Evaluate nearby customer candidates based on distance and activity.
- `GET /:id`: Offer details.
- `POST /:id/claim`: Claim and convert flash offer into a confirmed booking.
- `POST /:id/reject`: Dismiss offer.
- `POST /trigger-simulation`: Simulate cancellation recovery.

### 6. Notifications & Third-Party Payments (`/api/v1/notifications`, `/api/v1/payments`)
- `GET /notifications`: List user notifications.
- `PUT /notifications/:id/read`: Mark notification read.
- `PUT /notifications/read-all`: Mark all read.
- `POST /payments/create-order`: Create payment order.
- `POST /payments/verify`: Verify payment signature.
- `POST /payments/refund`: Process refund.
- `GET /payments/config/third-party`: View status of all third-party configurations.

---

## ⚡ Real-Time WebSockets (Socket.IO)

Clients can connect to Socket.IO at `ws://localhost:5000`:

```javascript
import { io } from 'socket.io-client';

const socket = io('http://localhost:5000');

// Join a barber's live queue room
socket.emit('join_barber_queue', 'Rahul Sharma');

// Listen for live queue updates
socket.on('queue_event', (event) => {
  console.log('Realtime Queue Event:', event);
  // event.eventType: 'SERVICE_STARTED' | 'SERVICE_COMPLETED' | 'CUSTOMER_CHECKED_IN' | 'DELAY_ALERT' | 'WALK_IN_ADDED'
});

// Listen for personal offers
socket.emit('join_user_channel', 'USER_ID');
socket.on('new_offer', (offer) => {
  console.log('Flash Offer Received:', offer);
});
```
