package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.BarberRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BarberViewModel(private val repository: BarberRepository) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            refreshCandidates()
        }
    }

    // =========================================================================
    // REAL-TIME BARBER QUEUE & WAITING CUSTOMER COUNT STATE
    // =========================================================================
    val allQueueItems: StateFlow<List<BarberQueueItem>> = repository.getAllQueueFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val barberSummaries: StateFlow<Map<String, BarberQueueSummary>> = repository.getBarberQueueSummariesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val transportMode: StateFlow<RealtimeTransportMode> = repository.transportMode
    val latestQueueEvent: StateFlow<RealtimeQueueEvent?> = repository.latestQueueEvent
    val realtimeEventQueue = repository.realtimeEventQueue

    val selectedQueueBooking = MutableStateFlow<Booking?>(null)
    val showLiveQueueModal = MutableStateFlow(false)
    val showQueueSimulatorSheet = MutableStateFlow(false)

    // Auth State
    val isLoggedIn = MutableStateFlow(true)
    val userProfile: StateFlow<User> = repository.getUserProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), User("usr_101", "Akash Sharma", "akash.sharma@example.com", "+91 98765 12340"))

    // Location State & Privacy Controls (Section 61)
    val selectedCity = MutableStateFlow("Noida, Uttar Pradesh")
    val isDetectingLocation = MutableStateFlow(false)
    val locationPermissionGranted = MutableStateFlow(true)
    val showLocationPicker = MutableStateFlow(false)

    // Navigation / Tabs: 0: Home, 1: Explore, 2: Bookings, 3: Notifications, 4: Profile
    val currentTab = MutableStateFlow(0)
    // Sub-screen navigation: null means tab root, or "SHOP_DETAIL", "BOOKING_STEP"
    val currentScreen = MutableStateFlow<String?>(null)

    // Catalog & Filters
    val allShops: StateFlow<List<BarberShop>> = repository.getShopsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    val filterCriteria = MutableStateFlow(FilterCriteria())
    val showFilterSheet = MutableStateFlow(false)

    // Filtered shops based on search query, category, and criteria
    val filteredShops: StateFlow<List<BarberShop>> = combine(
        allShops,
        searchQuery,
        selectedCategory,
        filterCriteria
    ) { shops, query, cat, filter ->
        var list = shops

        // Search query filter
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { shop ->
                shop.name.lowercase().contains(q) ||
                shop.city.lowercase().contains(q) ||
                shop.services.any { it.name.lowercase().contains(q) || it.category.lowercase().contains(q) } ||
                shop.barbers.any { it.name.lowercase().contains(q) }
            }
        }

        // Category filter
        if (cat != null) {
            list = list.filter { shop ->
                shop.services.any { it.category.equals(cat, ignoreCase = true) }
            }
        }

        // Filter criteria
        if (filter.openNowOnly) {
            list = list.filter { it.isOpen }
        }
        if (filter.minRating > 0) {
            list = list.filter { it.rating >= filter.minRating }
        }
        if (filter.maxDistanceKm < 10.0) {
            list = list.filter { it.distanceKm <= filter.maxDistanceKm }
        }
        list = list.filter { it.startingPrice <= filter.maxPrice }

        if (filter.selectedCategories.isNotEmpty()) {
            list = list.filter { shop ->
                shop.services.any { filter.selectedCategories.contains(it.category) }
            }
        }

        // Sorting
        when (filter.sortBy) {
            SortOption.RECOMMENDED -> list.sortedByDescending { it.rating * (10.0 - it.distanceKm) }
            SortOption.NEAREST -> list.sortedBy { it.distanceKm }
            SortOption.HIGHEST_RATED -> list.sortedByDescending { it.rating }
            SortOption.LOWEST_PRICE -> list.sortedBy { it.startingPrice }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Bookings & History
    val allBookings: StateFlow<List<Booking>> = repository.getBookingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingBookings = allBookings.map { list ->
        list.filter { it.status == BookingStatus.UPCOMING }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User's active position in the queue for their primary upcoming booking
    val primaryQueuePosition: StateFlow<UserQueuePosition?> = upcomingBookings.flatMapLatest { list ->
        val firstUpcoming = list.firstOrNull { it.status == BookingStatus.UPCOMING }
        if (firstUpcoming != null) {
            repository.getUserQueuePositionFlow(
                bookingId = firstUpcoming.id,
                barberName = firstUpcoming.barberName,
                scheduledTime = firstUpcoming.timeSlot
            )
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val completedBookings = allBookings.map { list ->
        list.filter { it.status == BookingStatus.COMPLETED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cancelledBookings = allBookings.map { list ->
        list.filter { it.status == BookingStatus.CANCELLED || it.status == BookingStatus.NO_SHOW }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications
    val notifications: StateFlow<List<NotificationItem>> = repository.getNotificationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =========================================================================
    // SMART RECOVERY & CANCELLED SLOTS ENGINE STATE
    // =========================================================================
    val activeOffers: StateFlow<List<SlotOffer>> = repository.getActiveOffersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOffers: StateFlow<List<SlotOffer>> = repository.getOffersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedOffer = MutableStateFlow<SlotOffer?>(null)
    val showOfferDetailsModal = MutableStateFlow(false)

    val selectedReminderBooking = MutableStateFlow<Booking?>(null)
    val showReminderDetailsModal = MutableStateFlow(false)

    val showSmartRecoveryConsole = MutableStateFlow(false)
    val targetCandidates = MutableStateFlow<List<TargetCustomer>>(emptyList())

    val isSimulatingReminder = MutableStateFlow(false)
    val isSimulatingRecovery = MutableStateFlow(false)
    val isBookingOffer = MutableStateFlow(false)
    val offerBookingError = MutableStateFlow<String?>(null)
    val offerBookingSuccess = MutableStateFlow<Booking?>(null)
    val simulateOfferConflict = MutableStateFlow(false) // HTTP 409 Concurrency test

    val noShowModalBooking = MutableStateFlow<Booking?>(null)

    // Booking Flow State
    val selectedShop = MutableStateFlow<BarberShop?>(null)
    val selectedServices = MutableStateFlow<List<BarberService>>(emptyList())
    val selectedBarber = MutableStateFlow<Barber?>(null) // null = "Any Available Barber"
    val selectedDate = MutableStateFlow("25 Sep 2026")
    val selectedSlot = MutableStateFlow<String?>("5:30 PM")
    val bookingStep = MutableStateFlow(1) // 1: Service, 2: Barber, 3: Time, 4: Review, 5: Payment, 6: Confirmed
    val availableSlotsMap = MutableStateFlow<Map<String, List<Pair<String, Boolean>>>>(emptyMap())
    val isLoadingSlots = MutableStateFlow(false)

    // Slot Hold & Countdown
    val slotReservation = MutableStateFlow<SlotReservation?>(null)
    val holdCountdownSeconds = MutableStateFlow(600) // 10 minutes
    private var countdownJob: Job? = null
    val isHoldingSlot = MutableStateFlow(false)
    val bookingConflictError = MutableStateFlow<String?>(null)
    val simulateConflictMode = MutableStateFlow(false) // Toggle to test 409 conflict

    // Payment State
    val selectedPaymentMethod = MutableStateFlow("UPI (Google Pay)")
    val isProcessingPayment = MutableStateFlow(false)
    val lastConfirmedBooking = MutableStateFlow<Booking?>(null)

    // Actions & Modals
    val cancelModalBooking = MutableStateFlow<Booking?>(null)
    val rescheduleModalBooking = MutableStateFlow<Booking?>(null)
    val rescheduleNewDate = MutableStateFlow("26 Sep 2026")
    val rescheduleNewTime = MutableStateFlow("06:00 PM")
    val reviewModalBooking = MutableStateFlow<Booking?>(null)
    val showDirectionsShop = MutableStateFlow<BarberShop?>(null)

    // Functions
    fun openShopDetail(shop: BarberShop) {
        selectedShop.value = shop
        selectedServices.value = listOf(shop.services.first())
        selectedBarber.value = null
        currentScreen.value = "SHOP_DETAIL"
    }

    fun startBookingFlow() {
        val shop = selectedShop.value ?: return
        if (selectedServices.value.isEmpty()) {
            selectedServices.value = listOf(shop.services.first())
        }
        bookingStep.value = 1
        currentScreen.value = "BOOKING_FLOW"
        fetchAvailability()
    }

    fun toggleServiceSelection(service: BarberService) {
        val current = selectedServices.value.toMutableList()
        if (current.any { it.id == service.id }) {
            if (current.size > 1) {
                current.removeAll { it.id == service.id }
            }
        } else {
            current.add(service)
        }
        selectedServices.value = current
    }

    fun selectBarber(barber: Barber?) {
        selectedBarber.value = barber
        fetchAvailability()
    }

    fun selectDate(date: String) {
        selectedDate.value = date
        fetchAvailability()
    }

    fun selectSlot(slot: String) {
        selectedSlot.value = slot
        bookingConflictError.value = null
    }

    fun fetchAvailability() {
        val shop = selectedShop.value ?: return
        val barberId = selectedBarber.value?.id ?: "any"
        viewModelScope.launch {
            isLoadingSlots.value = true
            availableSlotsMap.value = repository.getAvailabilitySlots(shop.id, barberId, selectedDate.value)
            isLoadingSlots.value = false
        }
    }

    fun proceedToReview() {
        val shop = selectedShop.value ?: return
        val slot = selectedSlot.value ?: return

        viewModelScope.launch {
            isHoldingSlot.value = true
            bookingConflictError.value = null
            val result = repository.holdSlot(
                shopId = shop.id,
                slotTime = slot,
                slotDate = selectedDate.value,
                simulateConflict = simulateConflictMode.value
            )
            isHoldingSlot.value = false

            result.onSuccess { reservation ->
                slotReservation.value = reservation
                startCountdown(10 * 60)
                bookingStep.value = 4 // Review step
            }.onFailure { err ->
                bookingConflictError.value = err.message
            }
        }
    }

    private fun startCountdown(totalSeconds: Int) {
        countdownJob?.cancel()
        holdCountdownSeconds.value = totalSeconds
        countdownJob = viewModelScope.launch {
            while (holdCountdownSeconds.value > 0) {
                delay(1000)
                holdCountdownSeconds.value -= 1
            }
            bookingConflictError.value = "Reservation hold expired. Please select a time slot again."
            slotReservation.value = null
        }
    }

    fun proceedToPayment() {
        bookingStep.value = 5 // Payment Step
    }

    fun completePaymentAndConfirm() {
        val shop = selectedShop.value ?: return
        val services = selectedServices.value
        val slot = selectedSlot.value ?: return
        val reservation = slotReservation.value ?: return

        viewModelScope.launch {
            isProcessingPayment.value = true
            val price = services.sumOf { it.price }
            val tax = 18
            val total = price + tax

            val payResult = repository.processPayment(reservation.reservationId, total, selectedPaymentMethod.value)
            isProcessingPayment.value = false

            if (payResult.isSuccess) {
                countdownJob?.cancel()
                val barberName = selectedBarber.value?.name ?: "Assigned Master Barber"
                val newBooking = Booking(
                    id = "BK" + (10000 + (1..89999).random()),
                    shopId = shop.id,
                    shopName = shop.name,
                    shopAddress = shop.address,
                    serviceNames = services.map { it.name },
                    barberName = barberName,
                    dateStr = selectedDate.value,
                    timeSlot = slot,
                    price = price,
                    tax = tax,
                    total = total,
                    status = BookingStatus.UPCOMING,
                    paymentStatus = PaymentStatus.PAID,
                    paymentMethod = selectedPaymentMethod.value
                )

                repository.saveBooking(newBooking)
                lastConfirmedBooking.value = newBooking
                bookingStep.value = 6 // Confirmed
            }
        }
    }

    fun cancelActiveBooking(bookingId: String) {
        viewModelScope.launch {
            repository.cancelBooking(bookingId)
            cancelModalBooking.value = null
            refreshCandidates()
        }
    }

    fun rescheduleActiveBooking(bookingId: String) {
        viewModelScope.launch {
            repository.rescheduleBooking(bookingId, rescheduleNewDate.value, rescheduleNewTime.value)
            rescheduleModalBooking.value = null
        }
    }

    fun toggleFavorite(shopId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(shopId)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun detectLocationSimulated() {
        viewModelScope.launch {
            isDetectingLocation.value = true
            delay(800)
            selectedCity.value = "Noida, Sector 18, UP"
            isDetectingLocation.value = false
            showLocationPicker.value = false
        }
    }

    fun updateProfile(name: String, email: String, phone: String) {
        viewModelScope.launch {
            repository.updateUserProfile(name, email, phone)
        }
    }

    fun closeSubScreens() {
        currentScreen.value = null
        countdownJob?.cancel()
        bookingConflictError.value = null
    }

    // =========================================================================
    // SMART RECOVERY & ARRIVAL REMINDER ACTIONS
    // =========================================================================

    fun trigger15MinuteReminder(bookingId: String? = null) {
        viewModelScope.launch {
            isSimulatingReminder.value = true
            delay(500)
            val reminded = repository.checkAndSendAppointmentReminders(bookingId)
            isSimulatingReminder.value = false
            if (reminded.isNotEmpty()) {
                selectedReminderBooking.value = reminded.first()
                showReminderDetailsModal.value = true
            }
        }
    }

    fun triggerCancellationRecovery(bookingId: String) {
        viewModelScope.launch {
            isSimulatingRecovery.value = true
            delay(600)
            val res = repository.triggerSlotRecovery(bookingId, OfferSourceType.CUSTOMER_CANCELLATION)
            isSimulatingRecovery.value = false
            res.onSuccess { offer ->
                selectedOffer.value = offer
                showOfferDetailsModal.value = true
            }
            refreshCandidates()
        }
    }

    fun markCustomerNoShow(bookingId: String) {
        viewModelScope.launch {
            isSimulatingRecovery.value = true
            repository.markBookingNoShow(bookingId)
            isSimulatingRecovery.value = false
            noShowModalBooking.value = null
            refreshCandidates()
        }
    }

    fun bookRecoveredOffer(offer: SlotOffer) {
        viewModelScope.launch {
            isBookingOffer.value = true
            offerBookingError.value = null
            offerBookingSuccess.value = null

            val res = repository.claimSlotOffer(
                offerId = offer.id,
                userId = "usr_101",
                simulateConflict = simulateOfferConflict.value
            )
            isBookingOffer.value = false

            res.onSuccess { newBooking ->
                offerBookingSuccess.value = newBooking
                lastConfirmedBooking.value = newBooking
            }.onFailure { err ->
                offerBookingError.value = err.message
            }
        }
    }

    fun refreshCandidates() {
        val list = repository.evaluateTargetCustomers()
        // If current device location permission is toggled off, update current user candidate
        targetCandidates.value = list.map { candidate ->
            if (candidate.id == "usr_101") {
                val perm = locationPermissionGranted.value
                candidate.copy(
                    locationPermissionGranted = perm,
                    isEligible = perm && candidate.distanceKm <= 2.0 && candidate.lastActiveHoursAgo <= 48.0,
                    ineligibilityReason = if (!perm) "Location permission disabled by user" else candidate.ineligibilityReason
                )
            } else {
                candidate
            }
        }
    }

    fun toggleLocationPermission() {
        locationPermissionGranted.value = !locationPermissionGranted.value
        refreshCandidates()
    }

    fun openReminderDetails(booking: Booking) {
        selectedReminderBooking.value = booking
        showReminderDetailsModal.value = true
    }

    fun openReminderDetailsById(bookingId: String) {
        viewModelScope.launch {
            val booking = allBookings.value.find { it.id == bookingId }
            if (booking != null) {
                selectedReminderBooking.value = booking
                showReminderDetailsModal.value = true
            }
        }
    }

    fun openOfferDetails(offer: SlotOffer) {
        selectedOffer.value = offer
        offerBookingError.value = null
        offerBookingSuccess.value = null
        showOfferDetailsModal.value = true
    }

    fun openOfferDetailsById(offerId: String) {
        viewModelScope.launch {
            val offer = allOffers.value.find { it.id == offerId }
            if (offer != null) {
                openOfferDetails(offer)
            }
        }
    }

    // =========================================================================
    // REAL-TIME BARBER QUEUE ACTIONS & SIMULATION
    // =========================================================================

    fun openLiveQueue(booking: Booking) {
        selectedQueueBooking.value = booking
        showLiveQueueModal.value = true
    }

    fun getQueuePositionForBooking(booking: Booking): Flow<UserQueuePosition> {
        return repository.getUserQueuePositionFlow(
            bookingId = booking.id,
            barberName = booking.barberName,
            scheduledTime = booking.timeSlot
        )
    }

    fun getBarberQueueFlow(barberName: String): Flow<List<BarberQueueItem>> {
        return repository.getBarberQueueFlow(barberName)
    }

    fun startBarberService(queueItemId: String) {
        viewModelScope.launch {
            repository.startBarberService(queueItemId)
        }
    }

    fun completeBarberService(queueItemId: String) {
        viewModelScope.launch {
            repository.completeBarberService(queueItemId)
        }
    }

    fun checkInQueueCustomer(queueItemId: String) {
        viewModelScope.launch {
            repository.customerCheckInQueueItem(queueItemId)
        }
    }

    fun cancelQueueCustomer(queueItemId: String) {
        viewModelScope.launch {
            repository.customerCancelQueueItem(queueItemId)
        }
    }

    fun markQueueCustomerNoShow(queueItemId: String) {
        viewModelScope.launch {
            repository.markQueueItemNoShow(queueItemId)
        }
    }

    fun broadcastBarberDelay(barberName: String, delayMins: Int = 10) {
        viewModelScope.launch {
            repository.broadcastBarberDelay(barberName, delayMins)
        }
    }

    fun addWalkInAhead(barberName: String, customerName: String, service: String, durationMins: Int, timeSlot: String) {
        viewModelScope.launch {
            repository.addWalkInQueueCustomer(barberName, customerName, service, durationMins, timeSlot)
        }
    }

    fun toggleTransportMode() {
        val current = transportMode.value
        val next = when (current) {
            RealtimeTransportMode.WEBSOCKET -> RealtimeTransportMode.SSE
            RealtimeTransportMode.SSE -> RealtimeTransportMode.POLLING
            RealtimeTransportMode.POLLING -> RealtimeTransportMode.WEBSOCKET
        }
        repository.setTransportMode(next)
    }

    fun resetQueueBenchmark() {
        viewModelScope.launch {
            repository.resetQueueToDefault()
        }
    }
}
