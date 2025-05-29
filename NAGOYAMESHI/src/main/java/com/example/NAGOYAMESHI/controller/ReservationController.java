package com.example.NAGOYAMESHI.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.NAGOYAMESHI.entity.Reservation;
import com.example.NAGOYAMESHI.entity.Shop;
import com.example.NAGOYAMESHI.entity.User;
import com.example.NAGOYAMESHI.form.ReservationEditForm;
import com.example.NAGOYAMESHI.form.ReservationInputForm;
import com.example.NAGOYAMESHI.form.ReservationRegisterForm;
import com.example.NAGOYAMESHI.repository.ReservationRepository;
import com.example.NAGOYAMESHI.repository.ShopRepository;
import com.example.NAGOYAMESHI.repository.UserRepository;
import com.example.NAGOYAMESHI.security.UserDetailsImpl;
import com.example.NAGOYAMESHI.service.ReservationService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ReservationController {
	private final ReservationRepository reservationRepository;
	private final ShopRepository shopRepository;
	private final ReservationService reservationService;
	private final UserRepository userRepository;

	public ReservationController(ReservationRepository reservationRepository, ShopRepository shopRepository,
			ReservationService reservationService, UserRepository userRepository) {
		this.reservationRepository = reservationRepository;
		this.shopRepository = shopRepository;
		this.reservationService = reservationService;
		this.userRepository = userRepository;
	}

	@GetMapping("/reservations")
	public String index(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
			@PageableDefault(page = 0, size = 20, sort = "id", direction = Direction.ASC) Pageable pageable,
			Model model) {
		User user = userDetailsImpl.getUser();
		Page<Reservation> reservationPage = reservationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
		model.addAttribute("reservationPage", reservationPage);
		return "reservations/index";
	}

	@GetMapping("/shops/{id}/reservations/input")
	public String input(@PathVariable(name = "id") Integer id,
	                    @ModelAttribute @Validated ReservationInputForm reservationInputForm,
	                    BindingResult bindingResult,
	                    RedirectAttributes redirectAttributes,
	                    Model model) {
	    Shop shop = shopRepository.getReferenceById(id);
	    List<String> condensedDaysWithHours = new ArrayList<>();
	    addDayWithHours(condensedDaysWithHours, "月", shop.getMondayOpeningHours(), shop.getMondayClosingHours());
	    addDayWithHours(condensedDaysWithHours, "火", shop.getTuesdayOpeningHours(), shop.getTuesdayClosingHours());
	    addDayWithHours(condensedDaysWithHours, "水", shop.getWednesdayOpeningHours(), shop.getWednesdayClosingHours());
	    addDayWithHours(condensedDaysWithHours, "木", shop.getThursdayOpeningHours(), shop.getThursdayClosingHours());
	    addDayWithHours(condensedDaysWithHours, "金", shop.getFridayOpeningHours(), shop.getFridayClosingHours());
	    addDayWithHours(condensedDaysWithHours, "土", shop.getSaturdayOpeningHours(), shop.getSaturdayClosingHours());
	    addDayWithHours(condensedDaysWithHours, "日", shop.getSundayOpeningHours(), shop.getSundayClosingHours());

	    Integer numberOfPeople = reservationInputForm.getNumberOfPeople();
	    Integer capacity = shop.getCapacity();
	    LocalDate reservedDate = null;
	    LocalTime reservedTime = null;

	    if (!(reservationInputForm.getFromDateToReserve() == null
	            || reservationInputForm.getFromDateToReserve().isEmpty())) {
	        try {
	            reservedDate = reservationInputForm.getReservedDate();
	            // 予約日が設定されていれば、休業日かどうかをチェック
	            if (reservedDate != null && reservationService.isClosedDay(reservedDate, shop)) {
	                bindingResult.addError(
	                        new FieldError(bindingResult.getObjectName(), "fromDateToReserve", "選択された日は休業日です。"));
	            }
	        } catch (DateTimeParseException e) {
	            bindingResult.addError(
	                    new FieldError(bindingResult.getObjectName(), "fromDateToReserve", "予約日の形式が正しくありません。"));
	        }
	    }
	    if (!(reservationInputForm.getFromTimeToReserve() == null
	            || reservationInputForm.getFromTimeToReserve().isEmpty())) {
	        try {
	            reservedTime = reservationInputForm.getReservedTime();
	        } catch (DateTimeParseException e) {
	            bindingResult.addError(
	                    new FieldError(bindingResult.getObjectName(), "fromTimeToReserve", "予約時間の形式が正しくありません。"));
	        }
	    }
	    if (reservedDate != null && reservedTime != null) {
	        if (!reservationService.isWithinOperatingHours(reservationInputForm.getFromDateToReserve(),
	                reservationInputForm.getFromTimeToReserve(), shop)) {
	            bindingResult.addError(
	                    new FieldError(bindingResult.getObjectName(), "fromTimeToReserve", "選択された日時は営業時間外です。"));
	        }
	    }
	    if (numberOfPeople != null && !reservationService.isWithinCapacity(numberOfPeople, capacity)) {
	        bindingResult.addError(new FieldError(bindingResult.getObjectName(), "numberOfPeople", "人数が定員を超えています。"));
	    }

	    if (bindingResult.hasErrors()) {
	        model.addAttribute("shop", shop);
	        model.addAttribute("errorMessage", "予約内容に不備があります。");
	        model.addAttribute("condensedDaysWithHours", condensedDaysWithHours);
	        return "shops/show";
	    }

	    redirectAttributes.addFlashAttribute("reservationInputForm", reservationInputForm);
	    return "redirect:/shops/{id}/reservations/confirm";
	}

	@GetMapping("/shops/{id}/reservations/confirm")
	public String confirm(@PathVariable(name = "id") Integer id,
			@ModelAttribute ReservationInputForm reservationInputForm,
			@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
			HttpServletRequest httpServletRequest,
			Model model) {
		Shop shop = shopRepository.getReferenceById(id);
		User user = userDetailsImpl.getUser();
		String reservedDate = reservationInputForm.getFromDateToReserve();
		String reservedTime = reservationInputForm.getFromTimeToReserve();

		ReservationRegisterForm reservationRegisterForm = new ReservationRegisterForm(shop.getId(), user.getId(),
				reservedDate.toString(), reservedTime.toString(), reservationInputForm.getNumberOfPeople());

		model.addAttribute("shop", shop);
		model.addAttribute("reservationRegisterForm", reservationRegisterForm);
		return "reservations/confirm";
	}

	@PostMapping("/shops/{shopId}/reservations/create")
    public String create(@PathVariable("shopId") Integer shopId,
                         @Validated @ModelAttribute ReservationRegisterForm reservationRegisterForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        Shop shop = shopRepository.getReferenceById(shopId);

        if (bindingResult.hasErrors()) {
            model.addAttribute("shop", shop);
            model.addAttribute("reservationRegisterForm", reservationRegisterForm);
            return "reservations/confirm"; // 確認画面に戻す
        }

        Map<String, String> errors = reservationService.validateReservation(reservationRegisterForm, shop);

        if (!errors.isEmpty()) {
            model.addAttribute("shop", shop);
            model.addAttribute("reservationRegisterForm", reservationRegisterForm);
            model.addAttribute("errorMap", errors); // エラーメッセージをまとめてModelに渡す
            return "reservations/confirm"; // 確認画面に戻す
        }

        try {
            reservationService.create(reservationRegisterForm);
            redirectAttributes.addFlashAttribute("successMessage", "予約が完了しました。");
            return "redirect:/shops/" + shopId; // 予約完了後に店舗詳細画面へリダイレクト
        } catch (Exception e) {
            model.addAttribute("shop", shop);
            model.addAttribute("errorMessage", "予約処理中にエラーが発生しました。");
            return "reservations/confirm"; // 確認画面に戻す
        }
    }

	// 予約完了を表示するメソッド
	@GetMapping("/shops/{id}/reservations/index")
	public String complete(@PathVariable("id") Integer id, Model model) {
		Shop shop = shopRepository.getReferenceById(id);
		model.addAttribute("shop", shop);
		return "reservations/index";
	}

	@GetMapping("/shops/{shopId}/edit/{userId}/{reservationId}")
	public String edit(@PathVariable(name = "shopId") Integer shopId,
	                   @PathVariable(name = "userId") Integer userId,
	                   @PathVariable(name = "reservationId") Integer reservationId,
	                   Model model) {
	    Shop shop = shopRepository.getReferenceById(shopId);
	    User user = userRepository.getReferenceById(userId);
	    Reservation reservation = reservationService.getReservationByIdWithShop(reservationId); // ★ これが重要

	    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

	    ReservationEditForm reservationEditForm = new ReservationEditForm(
	            reservation.getShop().getId(),
	            reservation.getUser().getId(),
	            reservation.getReservedDate().format(dateFormatter),
	            reservation.getReservedTime().format(timeFormatter),
	            reservation.getNumberOfPeople());

	    reservationEditForm.setId(reservation.getId());

	    model.addAttribute("shop", shop);
	    model.addAttribute("user", user);
	    model.addAttribute("reservation", reservation);
	    model.addAttribute("reservationEditForm", reservationEditForm);

	    return "reservations/edit";
	}

	@PostMapping("/shops/{shopId}/update/{userId}/{reservationId}")
	public String update(@PathVariable(name = "shopId") Integer shopId,
	                     @PathVariable(name = "userId") Integer userId,
	                     @PathVariable(name = "reservationId") Integer reservationId,
	                     @ModelAttribute @Validated ReservationEditForm reservationEditForm,
	                     BindingResult bindingResult,
	                     Model model,
	                     RedirectAttributes redirectAttributes) {

	    // 店舗情報とユーザー情報の取得
	    Shop shop = shopRepository.getReferenceById(shopId);
	    User user = userRepository.getReferenceById(userId);

	    // 予約情報の取得
	    Reservation reservation = reservationRepository.findByIdWithShop(reservationId)
	        .orElseThrow(() -> new IllegalArgumentException("予約が見つかりません"));

	    // バリデーションエラーがあれば、エラーメッセージを表示して再度フォームを返す
	    if (bindingResult.hasErrors()) {
	        model.addAttribute("shop", shop);
	        model.addAttribute("user", user);
	        model.addAttribute("reservation", reservation);
	        model.addAttribute("reservationEditForm", reservationEditForm);
	        model.addAttribute("errorMessage", "入力内容に誤りがあります。");
	        return "reservations/edit";
	    }

	    // 予約内容の更新をサービスクラスに委任
	    try {
	        reservationService.update(reservationEditForm, shop); // 予約の更新をサービスに委託

	        // 更新後の成功メッセージ
	        redirectAttributes.addFlashAttribute("successMessage", "予約内容を更新しました。");

	        // 予約一覧画面へリダイレクト
	        return "redirect:/reservations";
	    } catch (IllegalArgumentException e) {
	        // エラー発生時の処理
	        String msg = e.getMessage();

	        // エラーメッセージを追加
	        if ("選択された日時は営業時間外です。".equals(msg)) {
	            bindingResult.addError(new FieldError(bindingResult.getObjectName(), "fromDateToReserve", msg));
	        } else if ("選択された日は休業日です。".equals(msg)) {
	            bindingResult.addError(new FieldError(bindingResult.getObjectName(), "fromDateToReserve", msg));
	        } else if ("予約人数が定員を超えています。".equals(msg)) {
	            bindingResult.addError(new FieldError(bindingResult.getObjectName(), "numberOfPeople", msg));
	        } else {
	            model.addAttribute("errorMessage", "予約内容の変更中にエラーが発生しました。");
	        }

	        // エラーがあった場合は再度編集画面に戻す
	        model.addAttribute("shop", shop);
	        model.addAttribute("user", user);
	        model.addAttribute("reservation", reservation);
	        model.addAttribute("reservationEditForm", reservationEditForm);

	        return "reservations/edit";
	    }
	}

	@PostMapping("/reservations/{reservationId}/delete")
	public String delete(@PathVariable(name = "reservationId") Integer reservationId,
			RedirectAttributes redirectAttributes) {

		reservationRepository.deleteById(reservationId);

		redirectAttributes.addFlashAttribute("successMessage", "予約を削除しました。");

		return "redirect:/reservations";
	}

	private void addDayWithHours(List<String> list, String day, String openingHours, String closingHours) {
		if (openingHours != null && !openingHours.isEmpty() && closingHours != null && !closingHours.isEmpty()) {
			// 時間のフォーマットを修正
			String formattedOpeningHours = formatTime(openingHours);
			String formattedClosingHours = formatTime(closingHours);
			list.add(day + ":" + formattedOpeningHours + "〜" + formattedClosingHours);
		}
		// 開店時間または閉店時間のどちらか一方でも欠けている場合はリストに追加しない
	}

	private String formatTime(String time) {
		if (time != null && time.length() == 4) {
			return time.substring(0, 2) + ":" + time.substring(2, 4);
		}
		return time; // その他の形式の場合はそのまま返す
	}
}
