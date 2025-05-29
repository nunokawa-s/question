package com.example.NAGOYAMESHI.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.NAGOYAMESHI.entity.Category;
import com.example.NAGOYAMESHI.entity.Shop;
import com.example.NAGOYAMESHI.form.ShopEditForm;
import com.example.NAGOYAMESHI.form.ShopRegisterForm;
import com.example.NAGOYAMESHI.repository.CategoryRepository;
import com.example.NAGOYAMESHI.repository.ShopRepository;

@Service
public class ShopService {
	private final ShopRepository shopRepository;
	private final CategoryRepository categoryRepository;

	public ShopService(ShopRepository shopRepository, CategoryRepository categoryRepository) {
		this.shopRepository = shopRepository;
		this.categoryRepository = categoryRepository;
	}

	private String emptyToNull(String s) {
		return (s == null || s.isEmpty()) ? null : s;
	}
	
	private void validateOpeningClosingTimes(String opening, String closing) {
	    if ((StringUtils.hasText(opening) && !StringUtils.hasText(closing)) ||
	        (!StringUtils.hasText(opening) && StringUtils.hasText(closing))) {
	        throw new IllegalArgumentException("開店時間と閉店時間は両方入力するか、両方とも空にしてください。");
	    }
	    // 両方 null なら OK (休業日)
	    if (opening == null && closing == null) {
	        return;
	    }
	    // 片方が null でもう片方が空文字なら NG
	    if ((opening == null && StringUtils.hasText(closing)) || (StringUtils.hasText(opening) && closing == null)) {
	        throw new IllegalArgumentException("開店時間と閉店時間は両方入力するか、両方とも空にしてください。");
	    }
	}

	    @Transactional
	    public Shop create(ShopRegisterForm shopRegisterForm) {
	        Shop shop = new Shop();
	        MultipartFile imageFile = shopRegisterForm.getImageFile();

	        if (!imageFile.isEmpty()) {
	            String imageName = imageFile.getOriginalFilename();
	            String hashedImageName = generateNewFileName(imageName);
	            Path filePath = Paths.get("src/main/resources/static/storage", hashedImageName);

	            copyImageFile(imageFile, filePath);

	            shop.setImageName(hashedImageName);
	        }

	        validateOpeningClosingTimes(shopRegisterForm.getMondayOpeningHours(), shopRegisterForm.getMondayClosingHours());
	        validateOpeningClosingTimes(shopRegisterForm.getTuesdayOpeningHours(), shopRegisterForm.getTuesdayClosingHours());
	        validateOpeningClosingTimes(shopRegisterForm.getWednesdayOpeningHours(), shopRegisterForm.getWednesdayClosingHours());
	        validateOpeningClosingTimes(shopRegisterForm.getThursdayOpeningHours(), shopRegisterForm.getThursdayClosingHours());
	        validateOpeningClosingTimes(shopRegisterForm.getFridayOpeningHours(), shopRegisterForm.getFridayClosingHours());
	        validateOpeningClosingTimes(shopRegisterForm.getSaturdayOpeningHours(), shopRegisterForm.getSaturdayClosingHours());
	        validateOpeningClosingTimes(shopRegisterForm.getSundayOpeningHours(), shopRegisterForm.getSundayClosingHours());

	        setCommonShopProperties(shop, shopRegisterForm);
	        shopRepository.save(shop); // ここで shop に ID がセットされる

	        // フォームから選択されたカテゴリ ID を取得
	        List<Integer> categoryIds = shopRegisterForm.getCategoryIds();
	        if (categoryIds != null && !categoryIds.isEmpty()) {
	            Set<Category> categories = categoryIds.stream()
	                    .map(categoryRepository::findById)
	                    .filter(java.util.Optional::isPresent)
	                    .map(java.util.Optional::get)
	                    .collect(Collectors.toSet());
	            shop.setCategories(categories);
	            shopRepository.save(shop); // 関連付けを保存
	        }

	        return shop; // 作成した Shop オブジェクトを返す
	    }


	    @Transactional
	    public void update(ShopEditForm shopEditForm) {
	        Shop shop = shopRepository.findById(shopEditForm.getId())
	                .orElseThrow(() -> new IllegalArgumentException("Invalid shop Id:" + shopEditForm.getId()));
	        shop.setName(shopEditForm.getName());
	        shop.setDescription(shopEditForm.getDescription());
	        shop.setPrice(shopEditForm.getPrice());
	        shop.setCapacity(shopEditForm.getCapacity());
	        shop.setPostalCode(shopEditForm.getPostalCode());
	        shop.setAddress(shopEditForm.getAddress());
	        shop.setPhoneNumber(shopEditForm.getPhoneNumber());

	        validateOpeningClosingTimes(shopEditForm.getMondayOpeningHours(), shopEditForm.getMondayClosingHours());
	        validateOpeningClosingTimes(shopEditForm.getTuesdayOpeningHours(), shopEditForm.getTuesdayClosingHours());
	        validateOpeningClosingTimes(shopEditForm.getWednesdayOpeningHours(), shopEditForm.getWednesdayClosingHours());
	        validateOpeningClosingTimes(shopEditForm.getThursdayOpeningHours(), shopEditForm.getThursdayClosingHours());
	        validateOpeningClosingTimes(shopEditForm.getFridayOpeningHours(), shopEditForm.getFridayClosingHours());
	        validateOpeningClosingTimes(shopEditForm.getSaturdayOpeningHours(), shopEditForm.getSaturdayClosingHours());
	        validateOpeningClosingTimes(shopEditForm.getSundayOpeningHours(), shopEditForm.getSundayClosingHours());

	        shop.setMondayOpeningHours(emptyToNull(shopEditForm.getMondayOpeningHours()));
	        shop.setMondayClosingHours(emptyToNull(shopEditForm.getMondayClosingHours()));

	        shop.setTuesdayOpeningHours(emptyToNull(shopEditForm.getTuesdayOpeningHours()));
	        shop.setTuesdayClosingHours(emptyToNull(shopEditForm.getTuesdayClosingHours()));

	        shop.setWednesdayOpeningHours(emptyToNull(shopEditForm.getWednesdayOpeningHours()));
	        shop.setWednesdayClosingHours(emptyToNull(shopEditForm.getWednesdayClosingHours()));

	        shop.setThursdayOpeningHours(emptyToNull(shopEditForm.getThursdayOpeningHours()));
	        shop.setThursdayClosingHours(emptyToNull(shopEditForm.getThursdayClosingHours()));

	        shop.setFridayOpeningHours(emptyToNull(shopEditForm.getFridayOpeningHours()));
	        shop.setFridayClosingHours(emptyToNull(shopEditForm.getFridayClosingHours()));

	        shop.setSaturdayOpeningHours(emptyToNull(shopEditForm.getSaturdayOpeningHours()));
	        shop.setSaturdayClosingHours(emptyToNull(shopEditForm.getSaturdayClosingHours()));

	        shop.setSundayOpeningHours(emptyToNull(shopEditForm.getSundayOpeningHours()));
	        shop.setSundayClosingHours(emptyToNull(shopEditForm.getSundayClosingHours()));

	        Set<Category> categories = new HashSet<>();
	        for (Integer categoryId : shopEditForm.getCategoryIds()) {
	            categoryRepository.findById(categoryId).ifPresent(categories::add);
	        }
	        shop.setCategories(categories);

	        MultipartFile imageFile = shopEditForm.getImageFile();
	        if (!imageFile.isEmpty()) {
	            String imageName = imageFile.getOriginalFilename();
	            String hashedImageName = generateNewFileName(imageName);
	            Path filePath = Paths.get("src/main/resources/static/storage", hashedImageName);
	            copyImageFile(imageFile, filePath);
	            shop.setImageName(hashedImageName);
	        }

	        shopRepository.save(shop);
	    }
	
	

	public String generateNewFileName(String originalFileName) {
		String extension = "";

		int dotIndex = originalFileName.lastIndexOf('.');
		if (dotIndex >= 0) {
			extension = originalFileName.substring(dotIndex);
		}
		String uuid = UUID.randomUUID().toString();
		return uuid + extension;
	}

	// 画像ファイルを指定したファイルにコピーする
	public void copyImageFile(MultipartFile imageFile, Path filePath) {
		try {
			Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setCommonShopProperties(Shop shop, ShopRegisterForm form) {
		shop.setName(form.getName());
		shop.setDescription(form.getDescription());
		shop.setPrice(form.getPrice());
		shop.setCapacity(form.getCapacity());
		shop.setPostalCode(form.getPostalCode());
		shop.setAddress(form.getAddress());
		shop.setPhoneNumber(form.getPhoneNumber());

		shop.setMondayOpeningHours(form.getMondayClosed() ? null : emptyToNull(form.getMondayOpeningHours()));
		shop.setMondayClosingHours(form.getMondayClosed() ? null : emptyToNull(form.getMondayClosingHours()));

		shop.setTuesdayOpeningHours(form.getTuesdayClosed() ? null : emptyToNull(form.getTuesdayOpeningHours()));
		shop.setTuesdayClosingHours(form.getTuesdayClosed() ? null : emptyToNull(form.getTuesdayClosingHours()));

		shop.setWednesdayOpeningHours(form.getWednesdayClosed() ? null : emptyToNull(form.getWednesdayOpeningHours()));
		shop.setWednesdayClosingHours(form.getWednesdayClosed() ? null : emptyToNull(form.getWednesdayClosingHours()));

		shop.setThursdayOpeningHours(form.getThursdayClosed() ? null : emptyToNull(form.getThursdayOpeningHours()));
		shop.setThursdayClosingHours(form.getThursdayClosed() ? null : emptyToNull(form.getThursdayClosingHours()));

		shop.setFridayOpeningHours(form.getFridayClosed() ? null : emptyToNull(form.getFridayOpeningHours()));
		shop.setFridayClosingHours(form.getFridayClosed() ? null : emptyToNull(form.getFridayClosingHours()));

		shop.setSaturdayOpeningHours(form.getSaturdayClosed() ? null : emptyToNull(form.getSaturdayOpeningHours()));
		shop.setSaturdayClosingHours(form.getSaturdayClosed() ? null : emptyToNull(form.getSaturdayClosingHours()));

		shop.setSundayOpeningHours(form.getSundayClosed() ? null : emptyToNull(form.getSundayOpeningHours()));
		shop.setSundayClosingHours(form.getSundayClosed() ? null : emptyToNull(form.getSundayClosingHours()));

		Set<Category> categories = new HashSet<>();
		for (Integer categoryId : form.getCategoryIds()) {
			categoryRepository.findById(categoryId).ifPresent(categories::add);
		}
		shop.setCategories(categories);
	}
	
	 public String formatTime(String time) {
	        if (time != null && time.length() == 4) {
	            return time.substring(0, 2) + ":" + time.substring(2, 4);
	        }
	        return time;
	    }
	 
	 public List<String> getCondensedOpeningHours(Shop shop) {
		    List<String> condensedDaysWithHours = new ArrayList<>();

		    String mondayHours = formatTime(shop.getMondayOpeningHours()) + "~" + formatTime(shop.getMondayClosingHours());
		    String tuesdayHours = formatTime(shop.getTuesdayOpeningHours()) + "~" + formatTime(shop.getTuesdayClosingHours());
		    String wednesdayHours = formatTime(shop.getWednesdayOpeningHours()) + "~" + formatTime(shop.getWednesdayClosingHours());
		    String thursdayHours = formatTime(shop.getThursdayOpeningHours()) + "~" + formatTime(shop.getThursdayClosingHours());
		    String fridayHours = formatTime(shop.getFridayOpeningHours()) + "~" + formatTime(shop.getFridayClosingHours());
		    String saturdayHours = formatTime(shop.getSaturdayOpeningHours()) + "~" + formatTime(shop.getSaturdayClosingHours());
		    String sundayHours = formatTime(shop.getSundayOpeningHours()) + "~" + formatTime(shop.getSundayClosingHours());

		    String[] days = {"月", "火", "水", "木", "金", "土", "日"};
		    String[] hours = {mondayHours, tuesdayHours, wednesdayHours, thursdayHours, fridayHours, saturdayHours, sundayHours};

		    List<String> currentDays = new ArrayList<>();
		    String previousHour = null;

		    for (int i = 0; i < days.length; i++) {
		        String day = days[i];
		        String hour = hours[i];

		        if (hour.equals(previousHour)) {
		            currentDays.add(day);
		        } else {
		            if (!currentDays.isEmpty()) {
		                String dayRange = formatDayRange(currentDays);
		                condensedDaysWithHours.add(dayRange + ":" + (previousHour.equals("null~null") ? "休業" : previousHour));
		                currentDays = new ArrayList<>();
		            }
		            currentDays.add(day);
		            previousHour = hour;
		        }
		    }

		    // 最後の曜日範囲を追加
		    if (!currentDays.isEmpty()) {
		        String dayRange = formatDayRange(currentDays);
		        condensedDaysWithHours.add(dayRange + ":" + (previousHour.equals("null~null") ? "休業" : previousHour));
		    }

		    return condensedDaysWithHours;
		}

	    private String formatDayRange(List<String> days) {
	        if (days.isEmpty()) {
	            return "";
	        }
	        if (days.size() == 1) {
	            return days.get(0);
	        }
	        // 連続する曜日をまとめるロジック（例：月,火,水 → 月～水）
	        List<String> formattedRanges = new ArrayList<>();
	        List<String> currentSequence = new ArrayList<>();
	        for (int i = 0; i < days.size(); i++) {
	            currentSequence.add(days.get(i));
	            if (i == days.size() - 1 || !isConsecutiveDay(days.get(i), days.get(i + 1))) {
	                if (currentSequence.size() == 1) {
	                    formattedRanges.add(currentSequence.get(0));
	                } else if (currentSequence.size() > 1) {
	                    formattedRanges.add(currentSequence.get(0) + "～" + currentSequence.get(currentSequence.size() - 1));
	                }
	                currentSequence = new ArrayList<>();
	            }
	        }
	        return String.join(",", formattedRanges);
	    }

	    private boolean isConsecutiveDay(String day1, String day2) {
	        String[] daysOrder = {"月", "火", "水", "木", "金", "土", "日"};
	        int index1 = -1;
	        int index2 = -1;
	        for (int i = 0; i < daysOrder.length; i++) {
	            if (daysOrder[i].equals(day1)) {
	                index1 = i;
	            }
	            if (daysOrder[i].equals(day2)) {
	                index2 = i;
	            }
	        }
	        return index1 != -1 && index2 != -1 && index2 == (index1 + 1) % 7;
	    }
}
