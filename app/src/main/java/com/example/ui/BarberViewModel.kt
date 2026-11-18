package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.network.ApiHealthStatus
import com.example.data.network.QuickQueuePreviewDto
import com.example.data.network.QuickQueueConfirmDto
import com.example.data.repository.BarberRepository
import com.example.data.repository.ProposalChangedException
import com.example.data.repository.NoAvailabilityException
import com.example.data.util.BookingDateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

sealed class QuickQueueUiState {
    object Idle : QuickQueueUiState()
    object LoadingPreview : QuickQueueUiState()
    data class Proposed(val proposal: QuickQueuePreviewDto) : QuickQueueUiState()
    data class ProposalChanged(val updatedProposal: QuickQueuePreviewDto, val message: String) : QuickQueueUiState()
    data class NoAvailability(val message: String) : QuickQueueUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : QuickQueueUiState()
    object Confirming : QuickQueueUiState()
    data class Confirmed(val confirmation: QuickQueueConfirmDto) : QuickQueueUiState()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BarberViewModel(private val repository: BarberRepository) : ViewModel() {

    // Production Cloud API Sync & Status State
    val apiHealthStatus: StateFlow<ApiHealthStatus> = repository.apiHealthStatus
    val isSyncing = MutableStateFlow(false)
    val showApiConfigModal = MutableStateFlow(false)

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
    val isLoggedIn = MutableStateFlow(com.example.data.local.SessionManager.isLoggedIn())
    val authLoading = MutableStateFlow(false)
    val authError = MutableStateFlow<String?>(null)
    val authSuccess = MutableStateFlow<String?>(null)

    val userProfile: StateFlow<User> = repository.getUserProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.local.SessionManager.getCurrentUser())

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
        list.filter { it.status == BookingStatus.UPCOMING && BookingDateUtils.isSlotUpcoming(it.dateStr, it.timeSlot) }
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

    // Dynamic Real API Integration for My Appointments Screen
    val selectedAppointmentTab = MutableStateFlow(AppointmentTab.UPCOMING)
    val isLoadingAppointments = MutableStateFlow(false)
    val appointmentError = MutableStateFlow<String?>(null)
    val apiAppointments = MutableStateFlow<List<Booking>>(emptyList())

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

    // Booking Flow State (Dynamic 3-Day Window in Asia/Kolkata)
    val availableBookingDates = MutableStateFlow(BookingDateUtils.getAvailableBookingDates())
    val selectedShop = MutableStateFlow<BarberShop?>(null)
    val selectedServices = MutableStateFlow<List<BarberService>>(emptyList())
    val selectedBarber = MutableStateFlow<Barber?>(null) // null = "Any Available Barber"
    val selectedDate = MutableStateFlow(BookingDateUtils.getTodayFullDateStr())
    val selectedSlot = MutableStateFlow<String?>("5:30 PM")
    val bookingStep = MutableStateFlow(1) // 1: Service, 2: Barber, 3: Time, 4: Review, 5: Payment, 6: Confirmed
    val availableSlotsMap = MutableStateFlow<Map<String, List<Pair<String, Boolean>>>>(emptyMap())
    val isLoadingSlots = MutableStateFlow(false)
    val slotIntervalMinutes = MutableStateFlow(30)

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
    val rescheduleNewDate = MutableStateFlow(BookingDateUtils.getTodayFullDateStr())
    val rescheduleNewTime = MutableStateFlow("06:00 PM")
    val rescheduleNewBarber = MutableStateFlow<String?>(null)
    val reviewModalBooking = MutableStateFlow<Booking?>(null)
    val showDirectionsShop = MutableStateFlow<BarberShop?>(null)

    // Booking Mode & Quick Queue State
    val selectedBookingMode = MutableStateFlow<BookingMode>(BookingMode.QUICK_QUEUE)
    val quickQueueUiState = MutableStateFlow<QuickQueueUiState>(QuickQueueUiState.Idle)
    val activeQuickQueueProposal = MutableStateFlow<QuickQueuePreviewDto?>(null)
    val quickQueueReservationId = MutableStateFlow<String?>(null)
    val isRequestingQuickQueue = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            refreshCandidates()
            // Direct fetch of live shops from Cloud REST API
            repository.fetchShopsFromRemote()
            // Automatic initial sync with public Cloud API
            repository.syncWithCloudApi()
        }

        // Handle midnight transition for dynamic booking date calculation
        viewModelScope.launch {
            while (isActive) {
                delay(30_000) // check every 30 seconds
                val freshDates = BookingDateUtils.getAvailableBookingDates()
                val currentFirstDate = availableBookingDates.value.firstOrNull()?.fullDateStr
                if (currentFirstDate != freshDates.first().fullDateStr) {
                    availableBookingDates.value = freshDates
                    if (!BookingDateUtils.isDateInAllowedWindow(selectedDate.value)) {
                        selectedDate.value = freshDates.first().fullDateStr
                    }
                    if (!BookingDateUtils.isDateInAllowedWindow(rescheduleNewDate.value)) {
                        rescheduleNewDate.value = freshDates.first().fullDateStr
                    }
                }
            }
        }

        // Real-time API appointments fetching when user changes or tab changes
        viewModelScope.launch {
            combine(selectedAppointmentTab, userProfile) { tab, user ->
                Pair(tab, user)
            }.collectLatest { (tab, _) ->
                fetchAppointmentsForCurrentTab()
            }
        }
    }

    fun refreshAvailableBookingDates() {
        val freshDates = BookingDateUtils.getAvailableBookingDates()
        availableBookingDates.value = freshDates
        if (!BookingDateUtils.isDateInAllowedWindow(selectedDate.value)) {
            selectedDate.value = freshDates.first().fullDateStr
        }
        if (!BookingDateUtils.isDateInAllowedWindow(rescheduleNewDate.value)) {
            rescheduleNewDate.value = freshDates.first().fullDateStr
        }
    }

    fun refreshShops() {
        viewModelScope.launch {
            repository.fetchShopsFromRemote()
            repository.syncWithCloudApi()
        }
    }

    // Functions
    fun openShopDetail(shop: BarberShop) {
        selectedShop.value = shop
        selectedServices.value = listOf(shop.services.first())
        selectedBarber.value = shop.barbers.firstOrNull()
        currentScreen.value = "SHOP_DETAIL"
    }

    fun startBookingFlow() {
        refreshAvailableBookingDates()
        val shop = selectedShop.value ?: return
        if (selectedServices.value.isEmpty()) {
            selectedServices.value = listOf(shop.services.first())
        }
        if (selectedBarber.value == null) {
            selectedBarber.value = shop.barbers.firstOrNull()
        }
        bookingStep.value = 1
        currentScreen.value = "BOOKING_FLOW"
        // Reset Quick Queue State
        quickQueueUiState.value = QuickQueueUiState.Idle
        activeQuickQueueProposal.value = null
        quickQueueReservationId.value = null
        selectedBookingMode.value = BookingMode.QUICK_QUEUE
        bookingConflictError.value = null
        fetchAvailability()
    }

    fun selectBookingMode(mode: BookingMode) {
        selectedBookingMode.value = mode
        bookingConflictError.value = null
        if (mode == BookingMode.QUICK_QUEUE) {
            quickQueueUiState.value = QuickQueueUiState.Idle
            activeQuickQueueProposal.value = null
        } else {
            fetchAvailability()
        }
    }

    fun requestQuickQueuePreview() {
        val shop = selectedShop.value ?: return
        val services = selectedServices.value
        if (services.isEmpty()) return
        val barber = selectedBarber.value
        val effectiveBarberId = barber?.id?.takeIf { it.isNotBlank() } 
            ?: shop.barbers.firstOrNull()?.id?.takeIf { it.isNotBlank() } 
            ?: "b1"

        viewModelScope.launch {
            isRequestingQuickQueue.value = true
            quickQueueUiState.value = QuickQueueUiState.LoadingPreview
            bookingConflictError.value = null

            val result = repository.previewQuickQueue(
                shopId = shop.id,
                barberId = effectiveBarberId,
                serviceIds = services.map { it.id }
            )

            isRequestingQuickQueue.value = false
            result.onSuccess { proposal ->
                activeQuickQueueProposal.value = proposal
                proposal.dateStr?.let { selectedDate.value = it }
                if (proposal.startTime.isNotBlank()) {
                    selectedSlot.value = proposal.startTime
                }
                quickQueueUiState.value = QuickQueueUiState.Proposed(proposal)
            }.onFailure { err ->
                when (err) {
                    is ProposalChangedException -> {
                        activeQuickQueueProposal.value = err.updatedProposal
                        err.updatedProposal.dateStr?.let { selectedDate.value = it }
                        if (err.updatedProposal.startTime.isNotBlank()) {
                            selectedSlot.value = err.updatedProposal.startTime
                        }
                        quickQueueUiState.value = QuickQueueUiState.ProposalChanged(
                            updatedProposal = err.updatedProposal,
                            message = err.message ?: "The server updated the earliest appointment slot."
                        )
                    }
                    is NoAvailabilityException -> {
                        quickQueueUiState.value = QuickQueueUiState.NoAvailability(
                            message = err.message ?: "No suitable appointment time is available for your selected services. Please try another barber, service, or date."
                        )
                    }
                    else -> {
                        quickQueueUiState.value = QuickQueueUiState.Error(
                            message = err.message ?: "Failed to find earliest time. Please try again."
                        )
                    }
                }
            }
        }
    }

    fun confirmQuickQueueProposal() {
        val proposal = activeQuickQueueProposal.value ?: return
        val token = proposal.confirmationToken
        if (token.isBlank()) return

        viewModelScope.launch {
            isRequestingQuickQueue.value = true
            quickQueueUiState.value = QuickQueueUiState.Confirming

            val result = repository.confirmQuickQueue(token)
            isRequestingQuickQueue.value = false

            result.onSuccess { confirmDto ->
                quickQueueReservationId.value = confirmDto.reservationId
                quickQueueUiState.value = QuickQueueUiState.Confirmed(confirmDto)

                confirmDto.dateStr?.let { selectedDate.value = it }
                confirmDto.startTime?.let { selectedSlot.value = it }

                skipPaymentAndConfirmBooking()
            }.onFailure { err ->
                when (err) {
                    is ProposalChangedException -> {
                        activeQuickQueueProposal.value = err.updatedProposal
                        quickQueueUiState.value = QuickQueueUiState.ProposalChanged(
                            updatedProposal = err.updatedProposal,
                            message = "Proposal changed before confirmation. Please review the updated time."
                        )
                    }
                    is NoAvailabilityException -> {
                        quickQueueUiState.value = QuickQueueUiState.NoAvailability(
                            message = err.message ?: "The proposed slot is no longer available."
                        )
                    }
                    else -> {
                        quickQueueUiState.value = QuickQueueUiState.Error(
                            message = err.message ?: "Failed to confirm Quick Queue time. Please retry."
                        )
                    }
                }
            }
        }
    }

    fun cancelQuickQueue() {
        quickQueueUiState.value = QuickQueueUiState.Idle
        activeQuickQueueProposal.value = null
        quickQueueReservationId.value = null
        selectedBookingMode.value = BookingMode.SCHEDULED
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
        if (BookingDateUtils.isDateInAllowedWindow(date)) {
            selectedDate.value = date
        } else {
            selectedDate.value = availableBookingDates.value.first().fullDateStr
        }
        fetchAvailability()
    }

    fun selectSlot(slot: String) {
        selectedSlot.value = slot
        bookingConflictError.value = null
    }

    fun setSlotIntervalMinutes(minutes: Int) {
        slotIntervalMinutes.value = minutes
        fetchAvailability()
    }

    suspend fun getRescheduleAvailabilitySlots(
        shopId: String,
        barberId: String,
        date: String,
        serviceIds: List<String> = emptyList(),
        slotIntervalMinutes: Int = this.slotIntervalMinutes.value
    ): Map<String, List<Pair<String, Boolean>>> {
        return repository.getAvailabilitySlots(
            shopId = shopId,
            barberId = barberId,
            date = date,
            serviceIds = serviceIds,
            slotIntervalMinutes = slotIntervalMinutes
        )
    }

    fun fetchAvailability() {
        val shop = selectedShop.value ?: return
        val barberId = selectedBarber.value?.id ?: "any"
        val serviceIds = selectedServices.value.map { it.id }
        viewModelScope.launch {
            isLoadingSlots.value = true
            availableSlotsMap.value = repository.getAvailabilitySlots(
                shopId = shop.id,
                barberId = barberId,
                date = selectedDate.value,
                serviceIds = serviceIds,
                slotIntervalMinutes = slotIntervalMinutes.value
            )
            isLoadingSlots.value = false
        }
    }

    fun proceedToReview() {
        val shop = selectedShop.value ?: return
        val slot = selectedSlot.value ?: return
        val barber = selectedBarber.value
        val barberId = barber?.id ?: shop.barbers.firstOrNull()?.id ?: "b1"
        val barberName = barber?.name ?: shop.barbers.firstOrNull()?.name ?: "Rahul Sharma"

        val services = selectedServices.value
        val serviceIds = services.map { it.id }.ifEmpty { listOf(shop.services.firstOrNull()?.id ?: "s1") }
        val serviceNames = services.map { it.name }.ifEmpty { listOf(shop.services.firstOrNull()?.name ?: "Signature Royal Haircut") }
        val durationMinutes = services.sumOf { it.durationMin }.takeIf { it > 0 } ?: 30
        val startTime = BookingDateUtils.formatUserFriendlyTime(slot.substringBefore(" – ").substringBefore(" - ").trim()).ifBlank { "10:00 AM" }
        val bookingDate = BookingDateUtils.toIsoDateString(selectedDate.value)
        val slotDate = BookingDateUtils.toIsoDateString(selectedDate.value)
        val slotTime = startTime

        viewModelScope.launch {
            isHoldingSlot.value = true
            bookingConflictError.value = null
            val result = repository.holdSlot(
                shopId = shop.id,
                barberId = barberId,
                barberName = barberName,
                slotTime = slotTime,
                slotDate = slotDate,
                startTime = startTime,
                bookingDate = bookingDate,
                durationMinutes = durationMinutes,
                serviceIds = serviceIds,
                serviceNames = serviceNames,
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

    fun skipPaymentAndConfirmBooking() {
        val shop = selectedShop.value ?: return
        val services = selectedServices.value
        val mode = selectedBookingMode.value
        val qqProposal = activeQuickQueueProposal.value

        val date = if (mode == BookingMode.QUICK_QUEUE && !qqProposal?.dateStr.isNullOrBlank()) {
            qqProposal?.dateStr!!
        } else {
            selectedDate.value
        }

        val slot = if (mode == BookingMode.QUICK_QUEUE && !qqProposal?.startTime.isNullOrBlank()) {
            qqProposal?.startTime!!
        } else {
            selectedSlot.value ?: return
        }

        val barber = selectedBarber.value
        val effectiveBarberId = if (mode == BookingMode.QUICK_QUEUE && !qqProposal?.barberId.isNullOrBlank()) {
            qqProposal?.barberId!!
        } else {
            barber?.id ?: shop.barbers.firstOrNull()?.id ?: "b1"
        }

        val barberName = if (mode == BookingMode.QUICK_QUEUE && !qqProposal?.barberName.isNullOrBlank()) {
            qqProposal?.barberName!!
        } else {
            barber?.name ?: shop.barbers.firstOrNull()?.name ?: "Rahul Sharma"
        }

        val reservationId = if (mode == BookingMode.QUICK_QUEUE) {
            quickQueueReservationId.value
        } else {
            slotReservation.value?.reservationId
        }

        viewModelScope.launch {
            isProcessingPayment.value = true
            countdownJob?.cancel()

            val effectiveServices = if (services.isNotEmpty()) {
                services
            } else {
                listOf(shop.services.firstOrNull() ?: BarberService("s1", "Signature Royal Haircut", "Haircut", 300, 30))
            }

            val price = if (mode == BookingMode.QUICK_QUEUE && qqProposal != null && qqProposal.price > 0) {
                qqProposal.price
            } else {
                effectiveServices.sumOf { it.price }
            }
            val tax = 18
            val total = price + tax

            val serviceIds = effectiveServices.map { it.id }
            val serviceNames = effectiveServices.map { it.name }
            val bookingDate = BookingDateUtils.toIsoDateString(date)
            val startTime = slot.substringBefore(" – ").substringBefore(" - ").trim()
            val durationMinutes = effectiveServices.sumOf { it.durationMin }.takeIf { it > 0 } ?: 30

            val newBooking = Booking(
                id = "BK" + (10000 + (1..89999).random()),
                shopId = shop.id,
                shopName = shop.name,
                shopAddress = shop.address,
                serviceNames = serviceNames,
                serviceIds = serviceIds,
                barberId = effectiveBarberId,
                barberName = barberName,
                dateStr = date,
                bookingDate = bookingDate,
                timeSlot = slot,
                startTime = startTime,
                durationMinutes = durationMinutes,
                price = price,
                tax = tax,
                total = total,
                status = BookingStatus.UPCOMING,
                paymentStatus = PaymentStatus.PENDING,
                paymentMethod = "Pay at Salon (Post-service)",
                bookingMode = if (mode == BookingMode.QUICK_QUEUE) "QUICK_QUEUE" else "SCHEDULED"
            )

            // Directly call API and save booking using reservationId
            val savedBooking = repository.saveBooking(newBooking, reservationId = reservationId)
            lastConfirmedBooking.value = savedBooking
            isProcessingPayment.value = false
            bookingStep.value = 6 // Confirmed Screen
        }
    }

    fun completePaymentAndConfirm() {
        val shop = selectedShop.value ?: return
        val services = selectedServices.value
        val slot = selectedSlot.value ?: return
        val reservation = slotReservation.value ?: return

        viewModelScope.launch {
            isProcessingPayment.value = true
            val effectiveServices = if (services.isNotEmpty()) {
                services
            } else {
                listOf(shop.services.firstOrNull() ?: BarberService("s1", "Signature Royal Haircut", "Haircut", 300, 30))
            }
            val price = effectiveServices.sumOf { it.price }
            val tax = 18
            val total = price + tax

            val payResult = repository.processPayment(reservation.reservationId, total, selectedPaymentMethod.value)
            isProcessingPayment.value = false

            if (payResult.isSuccess) {
                countdownJob?.cancel()
                val barber = selectedBarber.value
                val effectiveBarberId = barber?.id ?: reservation.barberId ?: shop.barbers.firstOrNull()?.id ?: "b1"
                val barberName = barber?.name ?: reservation.barberName ?: shop.barbers.firstOrNull()?.name ?: "Rahul Sharma"
                val serviceIds = effectiveServices.map { it.id }
                val serviceNames = effectiveServices.map { it.name }
                val bookingDate = BookingDateUtils.toIsoDateString(selectedDate.value)
                val startTime = slot.substringBefore(" – ").substringBefore(" - ").trim()
                val durationMinutes = effectiveServices.sumOf { it.durationMin }.takeIf { it > 0 } ?: 30

                val newBooking = Booking(
                    id = "BK" + (10000 + (1..89999).random()),
                    shopId = shop.id,
                    shopName = shop.name,
                    shopAddress = shop.address,
                    serviceNames = serviceNames,
                    serviceIds = serviceIds,
                    barberId = effectiveBarberId,
                    barberName = barberName,
                    dateStr = selectedDate.value,
                    bookingDate = bookingDate,
                    timeSlot = slot,
                    startTime = startTime,
                    durationMinutes = durationMinutes,
                    price = price,
                    tax = tax,
                    total = total,
                    status = BookingStatus.UPCOMING,
                    paymentStatus = PaymentStatus.PAID,
                    paymentMethod = selectedPaymentMethod.value,
                    bookingMode = "SCHEDULED"
                )

                val savedBooking = repository.saveBooking(newBooking, reservationId = reservation.reservationId)
                lastConfirmedBooking.value = savedBooking
                bookingStep.value = 6 // Confirmed
                fetchAppointmentsForCurrentTab()
            }
        }
    }

    fun cancelActiveBooking(bookingId: String) {
        viewModelScope.launch {
            repository.cancelBooking(bookingId)
            cancelModalBooking.value = null
            refreshCandidates()
            fetchAppointmentsForCurrentTab()
        }
    }

    fun rescheduleActiveBooking(
        bookingId: String,
        newDateStr: String? = null,
        newTimeSlot: String? = null,
        newBarberName: String? = null
    ) {
        val rawDate = newDateStr ?: rescheduleNewDate.value
        val targetDate = if (BookingDateUtils.isDateInAllowedWindow(rawDate)) {
            rawDate
        } else {
            availableBookingDates.value.first().fullDateStr
        }
        val targetTime = newTimeSlot ?: rescheduleNewTime.value
        val targetBarber = newBarberName ?: rescheduleNewBarber.value
        val isoTargetDate = BookingDateUtils.toIsoDateString(targetDate)
        viewModelScope.launch {
            repository.rescheduleBooking(
                bookingId = bookingId,
                newDateStr = isoTargetDate,
                newTimeSlot = targetTime,
                newBarberName = targetBarber
            )
            rescheduleModalBooking.value = null
            fetchAppointmentsForCurrentTab()
        }
    }

    fun selectAppointmentTab(tab: AppointmentTab) {
        selectedAppointmentTab.value = tab
    }

    fun fetchAppointmentsForCurrentTab() {
        val currentUser = userProfile.value
        val targetUserId = currentUser.id.ifBlank { currentUser.email.ifBlank { "guest" } }
        val currentTab = selectedAppointmentTab.value
        viewModelScope.launch {
            isLoadingAppointments.value = true
            appointmentError.value = null
            val result = repository.fetchAppointmentsFromApi(
                status = currentTab.statusKey,
                userId = targetUserId
            )
            isLoadingAppointments.value = false
            result.onSuccess { list ->
                apiAppointments.value = list
            }.onFailure { err ->
                appointmentError.value = err.message
            }
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

    // =========================================================================
    // AUTHENTICATION ACTIONS & API INTEGRATION
    // =========================================================================

    fun login(emailOrPhone: String, password: String, onSuccess: () -> Unit = {}) {
        if (emailOrPhone.isBlank()) {
            authError.value = "Please enter your email or mobile number"
            return
        }
        if (password.isBlank()) {
            authError.value = "Please enter your password"
            return
        }

        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            authSuccess.value = null

            val result = repository.login(emailOrPhone.trim(), password)
            authLoading.value = false

            result.onSuccess { user ->
                isLoggedIn.value = true
                authSuccess.value = "Welcome back, ${user.name}!"
                // Directly refresh live shop catalog from API
                viewModelScope.launch {
                    repository.fetchShopsFromRemote()
                    repository.syncWithCloudApi()
                }
                fetchAppointmentsForCurrentTab()
                onSuccess()
            }.onFailure { error ->
                authError.value = error.message ?: "Authentication failed. Please check credentials."
            }
        }
    }

    fun register(name: String, email: String, phone: String, password: String, onSuccess: () -> Unit = {}) {
        if (name.isBlank()) {
            authError.value = "Please enter your full name"
            return
        }
        if (email.isBlank() || !email.contains("@")) {
            authError.value = "Please enter a valid email address"
            return
        }
        if (phone.isBlank() || phone.length < 8) {
            authError.value = "Please enter a valid phone number"
            return
        }
        if (password.length < 6) {
            authError.value = "Password must be at least 6 characters long"
            return
        }

        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            authSuccess.value = null

            val result = repository.register(name.trim(), email.trim(), phone.trim(), password)
            authLoading.value = false

            result.onSuccess { user ->
                isLoggedIn.value = true
                authSuccess.value = "Account created successfully! Welcome, ${user.name}."
                // Directly refresh live shop catalog from API
                viewModelScope.launch {
                    repository.fetchShopsFromRemote()
                    repository.syncWithCloudApi()
                }
                fetchAppointmentsForCurrentTab()
                onSuccess()
            }.onFailure { error ->
                authError.value = error.message ?: "Registration failed. Please try again."
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            isLoggedIn.value = false
            currentTab.value = 0
            currentScreen.value = null
            authError.value = null
            authSuccess.value = null
        }
    }

    fun clearAuthMessages() {
        authError.value = null
        authSuccess.value = null
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

    fun refreshCloudSync() {
        viewModelScope.launch {
            isSyncing.value = true
            repository.syncWithCloudApi()
            isSyncing.value = false
        }
    }

    fun updateApiEndpoint(newUrl: String) {
        viewModelScope.launch {
            isSyncing.value = true
            repository.updateApiBaseUrl(newUrl)
            repository.syncWithCloudApi()
            isSyncing.value = false
        }
    }
}
