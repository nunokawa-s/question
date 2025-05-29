package com.example.NAGOYAMESHI.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.NAGOYAMESHI.entity.Reservation;
import com.example.NAGOYAMESHI.entity.Shop;
import com.example.NAGOYAMESHI.entity.User;
import com.example.NAGOYAMESHI.form.ReservationEditForm;
import com.example.NAGOYAMESHI.form.ReservationRegisterForm;
import com.example.NAGOYAMESHI.repository.ReservationRepository;
import com.example.NAGOYAMESHI.repository.ShopRepository;
import com.example.NAGOYAMESHI.repository.UserRepository;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository, ShopRepository shopRepository,
            UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void create(ReservationRegisterForm reservationRegisterForm) {
        Reservation reservation = new Reservation();
        Shop shop = shopRepository.getReferenceById(reservationRegisterForm.getShopId());
        User user = userRepository.getReferenceById(reservationRegisterForm.getUserId());
        LocalDate reservedDate = LocalDate.parse(reservationRegisterForm.getFromDateToReserve());
        LocalTime reservedTime = LocalTime.parse(reservationRegisterForm.getFromTimeToReserve());

        reservation.setShop(shop);
        reservation.setUser(user);
        reservation.setReservedDate(reservedDate);
        reservation.setReservedTime(reservedTime);
        reservation.setNumberOfPeople(reservationRegisterForm.getNumberOfPeople());

        reservationRepository.save(reservation);
    }
    
    public Map<String, String> validateReservation(ReservationRegisterForm reservationRegisterForm, Shop shop) {
        Map<String, String> errors = new HashMap<>();

        if (!isWithinCapacity(reservationRegisterForm.getNumberOfPeople(), shop.getCapacity())) {
            errors.put("capacity", "予約人数が定員数を超えています。");
        }

        if (!isWithinOperatingHours(reservationRegisterForm.getFromDateToReserve(),
                reservationRegisterForm.getFromTimeToReserve(), shop)) {
            errors.put("operatingHours", "この時間帯は営業時間外です。");
        }

        return errors;
    }

    public boolean isWithinCapacity(Integer numberOfPeople, Integer capacity) {
        return numberOfPeople != null && capacity != null && numberOfPeople <= capacity;
    }

  

    private Optional<LocalTime> getOpeningHours(Shop shop, DayOfWeek dayOfWeek) {
        DateTimeFormatter shopTimeFormatter = DateTimeFormatter.ofPattern("HHmm");
        switch (dayOfWeek) {
            case MONDAY:
                return Optional.ofNullable(shop.getMondayOpeningHours()).map(time -> LocalTime.parse(time, shopTimeFormatter));
            case TUESDAY:
                return Optional.ofNullable(shop.getTuesdayOpeningHours()).map(time -> LocalTime.parse(time, shopTimeFormatter));
            case WEDNESDAY:
                return Optional.ofNullable(shop.getWednesdayOpeningHours()).map(time -> LocalTime.parse(time, shopTimeFormatter));
            case THURSDAY:
                return Optional.ofNullable(shop.getThursdayOpeningHours()).map(time -> LocalTime.parse(time, shopTimeFormatter));
            case FRIDAY:
                return Optional.ofNullable(shop.getFridayOpeningHours()).map(time -> LocalTime.parse(time, shopTimeFormatter));
            case SATURDAY:
                return Optional.ofNullable(shop.getSaturdayOpeningHours()).map(time -> LocalTime.parse(time, shopTimeFormatter));
            case SUNDAY:
                return Optional.ofNullable(shop.getSundayOpeningHours()).map(time -> LocalTime.parse(time, shopTimeFormatter));
            default:
                return Optional.empty();
        }
    }

    private Optional<LocalTime> getClosingHours(Shop shop, DayOfWeek dayOfWeek) {
        DateTimeFormatter shopTimeFormatter = DateTimeFormatter.ofPattern("HHmm");
        switch (dayOfWeek) {
            case MONDAY:
                return Optional.ofNullable(shop.getMondayClosingHours()).map(time -> LocalTime.parse(time, shopTimeFormatter));
            case TUESDAY:
                return Optional.ofNullable(shop.getTuesdayClosingHours()).map(time -> LocalTime.parse(time, shopTimeFormatter));
            case WEDNESDAY:
                return Optional.ofNullable(shop.getWednesdayClosingHours()).map(time -> LocalTime.parse(time, shopTimeFormatter));
            case THURSDAY:
                return Optional.ofNullable(shop.getThursdayClosingHours()).map(time -> LocalTime.parse(time, shopTimeFormatter));
            case FRIDAY:
                return Optional.ofNullable(shop.getFridayClosingHours()).map(time -> LocalTime.parse(time, shopTimeFormatter));
            case SATURDAY:
                return Optional.ofNullable(shop.getSaturdayClosingHours()).map(time -> LocalTime.parse(time, shopTimeFormatter));
            case SUNDAY:
                return Optional.ofNullable(shop.getSundayClosingHours()).map(time -> LocalTime.parse(time, shopTimeFormatter));
            default:
                return Optional.empty();
        }
    }


    @Transactional
    public void update(ReservationEditForm reservationEditForm, Shop shop) {
        LocalDate reservedDate = LocalDate.parse(reservationEditForm.getFromDateToReserve());
        LocalTime reservedTime = LocalTime.parse(reservationEditForm.getFromTimeToReserve());
        Integer numberOfPeople = reservationEditForm.getNumberOfPeople();
        Integer capacity = shop.getCapacity();

        // 営業時間外チェック
        if (!isWithinOperatingHoursOnly(reservedDate, reservedTime, shop)) {
            throw new IllegalArgumentException("選択された日時は営業時間外です。");
        }

        // 休業日チェック
        if (isClosedDay(reservedDate, shop)) {
            throw new IllegalArgumentException("選択された日は休業日です。");
        }

        // 定員超過チェック
        if (numberOfPeople != null && !isWithinCapacity(numberOfPeople, capacity)) {
            throw new IllegalArgumentException("予約人数が定員を超えています。");
        }

        // 予約情報の更新
        Reservation reservation = reservationRepository.getReferenceById(reservationEditForm.getId());
        reservation.setReservedDate(reservedDate);
        reservation.setReservedTime(reservedTime);
        reservation.setNumberOfPeople(reservationEditForm.getNumberOfPeople());
        reservationRepository.save(reservation);  // 変更を保存
    }
    
    public boolean isClosedDay(LocalDate date, Shop shop) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        switch (dayOfWeek) {
            case MONDAY:
                return shop.getMondayOpeningHours() == null || shop.getMondayOpeningHours().isEmpty() ||
                       shop.getMondayClosingHours() == null || shop.getMondayClosingHours().isEmpty();
            case TUESDAY:
                return shop.getTuesdayOpeningHours() == null || shop.getTuesdayOpeningHours().isEmpty() ||
                       shop.getTuesdayClosingHours() == null || shop.getTuesdayClosingHours().isEmpty();
            case WEDNESDAY:
                return shop.getWednesdayOpeningHours() == null || shop.getWednesdayOpeningHours().isEmpty() ||
                       shop.getWednesdayClosingHours() == null || shop.getWednesdayClosingHours().isEmpty();
            case THURSDAY:
                return shop.getThursdayOpeningHours() == null || shop.getThursdayOpeningHours().isEmpty() ||
                       shop.getThursdayClosingHours() == null || shop.getThursdayClosingHours().isEmpty();
            case FRIDAY:
                return shop.getFridayOpeningHours() == null || shop.getFridayOpeningHours().isEmpty() ||
                       shop.getFridayClosingHours() == null || shop.getFridayClosingHours().isEmpty();
            case SATURDAY:
                return shop.getSaturdayOpeningHours() == null || shop.getSaturdayOpeningHours().isEmpty() ||
                       shop.getSaturdayClosingHours() == null || shop.getSaturdayClosingHours().isEmpty();
            case SUNDAY:
                return shop.getSundayOpeningHours() == null || shop.getSundayOpeningHours().isEmpty() ||
                       shop.getSundayClosingHours() == null || shop.getSundayClosingHours().isEmpty();
            default:
                return false; // 想定外の曜日
        }
    }
    
    public boolean isWithinOperatingHoursOnly(LocalDate date, LocalTime time, Shop shop) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        Optional<LocalTime> openingTime = getOpeningHours(shop, dayOfWeek);
        Optional<LocalTime> closingTime = getClosingHours(shop, dayOfWeek);

        if (openingTime.isPresent() && closingTime.isPresent()) {
            LocalTime open = openingTime.get();
            LocalTime close = closingTime.get();

            // 閉店時間より後、または開店時間より前ならNG
            return !time.isBefore(open) && !time.isAfter(close);
        }

        // 営業時間情報がなければ予約不可とする（仕様により調整）
        return false;
    }
    
    public boolean isWithinOperatingHours(String reservedDateStr, String reservedTimeStr, Shop shop) {
        if (reservedTimeStr == null) {
            return true; // 時間情報がない場合はチェックしない
        }
        try {
            LocalDate reservedDate = LocalDate.parse(reservedDateStr);
            LocalTime reservedTime = LocalTime.parse(reservedTimeStr);
            DayOfWeek dayOfWeek = reservedDate.getDayOfWeek();
            Optional<LocalTime> openingTime = getOpeningHours(shop, dayOfWeek);
            Optional<LocalTime> closingTime = getClosingHours(shop, dayOfWeek);

            // 特定の曜日の営業時間が NULL の場合は予約不可とする
            if (dayOfWeek == DayOfWeek.MONDAY && (shop.getMondayOpeningHours() == null || shop.getMondayClosingHours() == null || shop.getMondayOpeningHours().isEmpty() || shop.getMondayClosingHours().isEmpty())) {
                return false;
            }
            // 他の曜日についても同様のチェックを追加する場合はここに記述

            if (openingTime.isPresent() && closingTime.isPresent()) {
                return !reservedTime.isBefore(openingTime.get()) && !reservedTime.isAfter(closingTime.get());
            }
            return true; // 曜日の営業時間情報がない場合はチェックしない (NULL ではない場合のみチェックを通す)
        } catch (DateTimeParseException e) {
            return false; // パースエラーの場合は営業時間外とみなす
        }
    }
    
    public Reservation getReservationByIdWithShop(Integer id) {
        return reservationRepository.findByIdWithShop(id)
                .orElseThrow(() -> new NoSuchElementException("指定された予約は見つかりませんでした。"));
    }
}