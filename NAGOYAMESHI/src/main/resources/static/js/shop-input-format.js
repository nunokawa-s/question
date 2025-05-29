/**
 * 郵便番号の結合（編集フォーム用）
 */
function updatePostalCodeEdit() {
    const first = document.getElementById('postalCodeFirstEdit').value;
    const second = document.getElementById('postalCodeSecondEdit').value;
    document.getElementById('postalCodeEdit').value = first + second;
}

/**
 * 電話番号の結合（編集フォーム用）
 */
function updatePhoneNumberEdit() {
    const first = document.getElementById('phoneFirstEdit').value;
    const second = document.getElementById('phoneSecondEdit').value;
    const third = document.getElementById('phoneThirdEdit').value;
    document.getElementById('phoneNumberEdit').value = first + second + third;
}

/**
 * 開店時間の結合
 * @param {string} day 曜日 (例: 'monday')
 */
function updateOpeningHours(day) {
    const first = document.getElementById(day + 'OpeningHoursFirst').value;
    const second = document.getElementById(day + 'OpeningHoursSecond').value;
    document.getElementById(day + 'OpeningHours').value = first + second;
}

/**
 * 閉店時間の結合
 * @param {string} day 曜日 (例: 'monday')
 */
function updateClosingHours(day) {
    const first = document.getElementById(day + 'ClosingHoursFirst').value;
    const second = document.getElementById(day + 'ClosingHoursSecond').value;
    document.getElementById(day + 'ClosingHours').value = first + second;
}

/**
 * 時間入力フィールドのバリデーション
 * 時間と分の両方が入力されているかチェックし、そうでない場合はエラーメッセージを表示します。
 * @param {string} day 曜日 (例: 'monday')
 * @param {string} type 'opening' または 'closing'
 */
function validateTimeInput(day, type) {
    const firstInputId = day + (type === 'opening' ? 'OpeningHoursFirst' : 'ClosingHoursFirst');
    const secondInputId = day + (type === 'opening' ? 'OpeningHoursSecond' : 'ClosingHoursSecond');
    const errorDivId = day + (type === 'opening' ? 'OpeningHoursError' : 'ClosingHoursError');
    const firstValue = document.getElementById(firstInputId).value;
    const secondValue = document.getElementById(secondInputId).value;
    const errorDiv = document.getElementById(errorDivId);

    if ((firstValue && !secondValue) || (!firstValue && secondValue)) {
        errorDiv.textContent = '時間と分の両方を入力してください。';
    } else {
        errorDiv.textContent = '';
    }
}

/**
 * 休業日のチェックボックスの状態変更時の処理
 * チェックされた場合、対応する時間入力フィールドをクリアします。
 * @param {string} day 曜日 (例: 'monday')
 */
function setupClosedDayHandlers(day) {
    const closedCheckbox = document.getElementById(day + 'Closed');
    const openingFirst = document.getElementById(day + 'OpeningHoursFirst');
    const openingSecond = document.getElementById(day + 'OpeningHoursSecond');
    const closingFirst = document.getElementById(day + 'ClosingHoursFirst');
    const closingSecond = document.getElementById(day + 'ClosingHoursSecond');
    const openingHidden = document.getElementById(day + 'OpeningHours');
    const closingHidden = document.getElementById(day + 'ClosingHours');
    const openingError = document.getElementById(day + 'OpeningHoursError');
    const closingError = document.getElementById(day + 'ClosingHoursError');

    if (closedCheckbox) {
        closedCheckbox.addEventListener('change', function() {
            if (this.checked) {
                openingFirst.value = '';
                openingSecond.value = '';
                closingFirst.value = '';
                closingSecond.value = '';
                openingHidden.value = '';
                closingHidden.value = '';
                if (openingError) openingError.textContent = '';
                if (closingError) closingError.textContent = '';
            }
            // チェックが外れた際の処理は必要に応じて追加 (例: 直前の値を復元)
        });
    }
}

/**
 * ページロード時に初期値をセットし、休業日ハンドラーを設定します。
 */
document.addEventListener('DOMContentLoaded', function() {
    const days = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'];
    days.forEach(setInitialTimeValues);
    days.forEach(setupClosedDayHandlers);

    // フォーム送信時のバリデーション
    const shopEditForm = document.getElementById('shopEditForm');
    if (shopEditForm) {
        shopEditForm.addEventListener('submit', function(event) {
            const timeErrorElements = document.querySelectorAll('[id$="OpeningHoursError"], [id$="ClosingHoursError"]');
            let hasTimeErrors = false;
            timeErrorElements.forEach(function(errorElement) {
                if (errorElement.textContent !== '') {
                    hasTimeErrors = true;
                }
            });

            if (hasTimeErrors) {
                event.preventDefault();
                alert('入力時間に誤りがあります。時間と分の両方を入力してください。');
            }
        });
    }
});

/**
 * ページロード時に時間入力フィールドに初期値を設定します。
 * @param {string} day 曜日 (例: 'monday')
 */
function setInitialTimeValues(day) {
    const openingHidden = document.getElementById(day + 'OpeningHours');
    const closingHidden = document.getElementById(day + 'ClosingHours');
    const openingFirst = document.getElementById(day + 'OpeningHoursFirst');
    const openingSecond = document.getElementById(day + 'OpeningHoursSecond');
    const closingFirst = document.getElementById(day + 'ClosingHoursFirst');
    const closingSecond = document.getElementById(day + 'ClosingHoursSecond');

    if (openingHidden && openingHidden.value.length === 4) {
        openingFirst.value = openingHidden.value.substring(0, 2);
        openingSecond.value = openingHidden.value.substring(2, 4);
    }

    if (closingHidden && closingHidden.value.length === 4) {
        closingFirst.value = closingHidden.value.substring(0, 2);
        closingSecond.value = closingHidden.value.substring(2, 4);
    }
}

function updatePostalCode() {
    const first = document.getElementById('postalCodeFirst').value;
    const second = document.getElementById('postalCodeSecond').value;
    const postalCodeField = document.getElementById('postalCode');
    if (first.length === 3 && second.length === 4) {
        postalCodeField.value = first + second;
    } else if (!first && !second) {
        postalCodeField.value = ''; // すべて空の場合
    } else {
        postalCodeField.value = ''; // 不完全な入力の場合はクリア (または適切な処理)
    }
}

function updatePhoneNumber() {
    const first = document.getElementById('phoneFirst').value;
    const second = document.getElementById('phoneSecond').value;
    const third = document.getElementById('phoneThird').value;
    const phoneNumberField = document.getElementById('phoneNumber');
    if (first.length >= 2 && first.length <= 4 &&
        second.length >= 2 && second.length <= 4 &&
        third.length === 4) {
        phoneNumberField.value = first + second + third;
    } else if (!first && !second && !third) {
        phoneNumberField.value = ''; // すべて空の場合
    } else {
        phoneNumberField.value = ''; // 不完全な入力の場合はクリア (または適切な処理)
    }
}
