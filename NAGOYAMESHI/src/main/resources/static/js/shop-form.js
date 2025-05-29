function toggleTimeInputs(day) {
    const isClosed = document.getElementById(day + 'Closed').checked;
    const openingHoursInput = document.querySelector('[name="' + day + 'OpeningHours"]');
    const closingHoursInput = document.querySelector('[name="' + day + 'ClosingHours"]');

    openingHoursInput.disabled = isClosed;
    closingHoursInput.disabled = isClosed;

    if (isClosed) {
        openingHoursInput.value = '';
        closingHoursInput.value = '';
    }
}