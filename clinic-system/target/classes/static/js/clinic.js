/**
 * clinic.js — ClinicCare frontend utilities
 *
 * Responsibilities:
 *  1. Auto-dismiss flash alert banners after 5 seconds
 *  2. Confirm dialogs for destructive actions (cancel, suspend, deactivate)
 *  3. Booking form: keep doctor + date in sync when either changes
 *  4. Enforce minimum date (today) on all date pickers
 *  5. Disable submit buttons on form submission to prevent double-posting
 *  6. Highlight the active nav link based on the current URL
 */

document.addEventListener('DOMContentLoaded', function () {

    // ── 1. Auto-dismiss flash alerts after 5 seconds ─────────────────────────
    document.querySelectorAll('.alert.alert-dismissible').forEach(function (alert) {
        setTimeout(function () {
            var bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) bsAlert.close();
        }, 5000);
    });


    // ── 2. Confirm dialogs for destructive POST forms ─────────────────────────
    // Covers: appointment cancel, clinic suspend/deactivate, user disable
    // Forms declare their message via data-confirm="..." attribute,
    // or fall back to the generic message below.
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            var message = form.getAttribute('data-confirm') ||
                'Are you sure you want to perform this action?';
            if (!window.confirm(message)) {
                e.preventDefault();
            }
        });
    });

    // Also catch inline onsubmit="return confirm(...)" patterns that
    // already exist in templates — no extra work needed for those.


    // ── 3. Booking form: sync doctor + date fields ────────────────────────────
    // The book-appointment page submits a GET to reload available slots
    // whenever the doctor OR date changes.  If the user changes the doctor
    // but hasn't touched the date yet, we still want to fire the reload
    // so the available-slots panel updates cleanly.
    var doctorSelect = document.querySelector('select[name="doctorId"]');
    var dateInput    = document.querySelector('input[name="date"]');

    if (doctorSelect && dateInput) {
        // Only trigger form submit when BOTH fields have a value
        function submitIfReady() {
            if (doctorSelect.value && dateInput.value) {
                doctorSelect.closest('form').submit();
            }
        }

        doctorSelect.addEventListener('change', submitIfReady);
        dateInput.addEventListener('change', submitIfReady);
    }


    // ── 4. Enforce minimum date = today on all date pickers ──────────────────
    // Provides a client-side safety net in addition to the server-side
    // @Future validation on AppointmentBookRequest.appointmentDate.
    var today = new Date().toISOString().split('T')[0];
    document.querySelectorAll('input[type="date"]').forEach(function (input) {
        // Only set min if the field doesn't already have a stricter one
        if (!input.min || input.min < today) {
            // Don't override date-of-birth fields (those allow past dates)
            var fieldName = input.name || input.id || '';
            if (!fieldName.toLowerCase().includes('birth') &&
                !fieldName.toLowerCase().includes('dob')) {
                input.min = today;
            }
        }
    });


    // ── 5. Prevent double-submit on all forms ─────────────────────────────────
    // Disables the submit button after the first click so patients can't
    // accidentally book the same appointment twice by clicking quickly.
    document.querySelectorAll('form').forEach(function (form) {
        form.addEventListener('submit', function () {
            var submitBtn = form.querySelector('[type="submit"]');
            if (submitBtn && !submitBtn.disabled) {
                // Small delay so the browser can capture the value before disabling
                setTimeout(function () {
                    submitBtn.disabled = true;
                    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" ' +
                        'role="status" aria-hidden="true"></span>Processing…';
                }, 10);
            }
        });
    });


    // ── 6. Highlight active nav link ──────────────────────────────────────────
    var currentPath = window.location.pathname;
    document.querySelectorAll('.navbar-nav .nav-link').forEach(function (link) {
        var href = link.getAttribute('href');
        if (href && currentPath.startsWith(href) && href !== '/') {
            link.classList.add('active');
        }
    });

});