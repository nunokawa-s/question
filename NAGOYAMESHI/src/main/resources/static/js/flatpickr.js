/**
 *
 */
let maxDate = new Date();
maxDate = maxDate.setMonth(maxDate.getMonth() + 3);
document.addEventListener("DOMContentLoaded", function() {

	const tomorrow = new Date();
	tomorrow.setDate(tomorrow.getDate() + 1);
	const tomorrowFormatted = tomorrow.toISOString().slice(0, 10);

	//日付選択
	flatpickr('#fromDateToReserve', {
		mode: "single",
		locale: 'ja',
		minDate: tomorrowFormatted, // 明日以降の日付を設定
		dateFormat: "Y-m-d"
	});

	// 時間選択
	flatpickr("#fromTimeToReserve", {
		enableTime: true,
		noCalendar: true,
		dateFormat: "H:i",
		time_24hr: true
	});

});