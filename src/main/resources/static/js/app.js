/**
 * Hospital Doctor Appointment System - Core Client JavaScript
 */

document.addEventListener('DOMContentLoaded', function () {
    // 1. Initialize Bootstrap Tooltips & Popovers
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // 2. Demo Login Fast Autofill
    window.fillCredentials = function (role) {
        const emailInput = document.getElementById('email');
        const passwordInput = document.getElementById('password');

        if (role === 'doctor') {
            if (emailInput) emailInput.value = 'doctor@example.com';
            if (passwordInput) passwordInput.value = 'Doctor@123';
        } else if (role === 'patient') {
            if (emailInput) emailInput.value = 'patient@example.com';
            if (passwordInput) passwordInput.value = 'Patient@123';
        }
    };

    // 3. Dynamic Time Slot Fetcher for Appointment Booking
    const doctorSelect = document.getElementById('doctorSelect');
    const datePicker = document.getElementById('appointmentDatePicker');
    const slotsContainer = document.getElementById('slotsContainer');
    const selectedTimeInput = document.getElementById('selectedTimeInput');
    const slotLoadingSpinner = document.getElementById('slotLoadingSpinner');
    const noSlotsMessage = document.getElementById('noSlotsMessage');

    function loadAvailableSlots() {
        if (!doctorSelect || !datePicker || !slotsContainer) return;

        const doctorId = doctorSelect.value;
        const selectedDate = datePicker.value;

        if (!doctorId || !selectedDate) {
            slotsContainer.innerHTML = '';
            if (noSlotsMessage) noSlotsMessage.classList.remove('d-none');
            return;
        }

        if (slotLoadingSpinner) slotLoadingSpinner.classList.remove('d-none');
        if (noSlotsMessage) noSlotsMessage.classList.add('d-none');
        slotsContainer.innerHTML = '';

        fetch(`/api/doctors/${doctorId}/slots?date=${selectedDate}`)
            .then(response => {
                if (!response.ok) throw new Error('Failed to load slots');
                return response.json();
            })
            .then(slots => {
                if (slotLoadingSpinner) slotLoadingSpinner.classList.add('d-none');
                slotsContainer.innerHTML = '';

                if (!slots || slots.length === 0) {
                    if (noSlotsMessage) {
                        noSlotsMessage.textContent = 'No availability slots scheduled for this doctor on this day.';
                        noSlotsMessage.classList.remove('d-none');
                    }
                    return;
                }

                let availableCount = 0;
                slots.forEach(slot => {
                    const btn = document.createElement('button');
                    btn.type = 'button';
                    btn.className = `btn btn-outline-primary time-slot-btn m-1 ${!slot.available ? 'disabled' : ''}`;
                    btn.textContent = slot.formattedTime;
                    btn.dataset.time = slot.startTime;

                    if (!slot.available) {
                        btn.disabled = true;
                        btn.title = 'Slot Already Booked or Unavailable';
                    } else {
                        availableCount++;
                        btn.addEventListener('click', function () {
                            document.querySelectorAll('.time-slot-btn').forEach(b => b.classList.remove('active', 'selected', 'btn-primary'));
                            btn.classList.add('active', 'selected', 'btn-primary');
                            if (selectedTimeInput) {
                                selectedTimeInput.value = slot.startTime;
                            }
                        });
                    }

                    slotsContainer.appendChild(btn);
                });

                if (availableCount === 0 && noSlotsMessage) {
                    noSlotsMessage.textContent = 'All slots for this day are fully booked or have passed.';
                    noSlotsMessage.classList.remove('d-none');
                }
            })
            .catch(error => {
                console.error('Error fetching slots:', error);
                if (slotLoadingSpinner) slotLoadingSpinner.classList.add('d-none');
                if (noSlotsMessage) {
                    noSlotsMessage.textContent = 'Unable to load slots. Please check your network or try again.';
                    noSlotsMessage.classList.remove('d-none');
                }
            });
    }

    if (doctorSelect && datePicker) {
        doctorSelect.addEventListener('change', loadAvailableSlots);
        datePicker.addEventListener('change', loadAvailableSlots);
        // Initial load if pre-selected
        if (doctorSelect.value && datePicker.value) {
            loadAvailableSlots();
        }
    }

    // 4. Auto Dismiss Alerts after 5 seconds
    const autoAlerts = document.querySelectorAll('.alert-dismissible');
    autoAlerts.forEach(function (alert) {
        setTimeout(function () {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
});
