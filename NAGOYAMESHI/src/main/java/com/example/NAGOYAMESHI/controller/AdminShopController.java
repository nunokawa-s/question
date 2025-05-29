package com.example.NAGOYAMESHI.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.NAGOYAMESHI.entity.Category;
import com.example.NAGOYAMESHI.entity.Shop;
import com.example.NAGOYAMESHI.form.ShopEditForm;
import com.example.NAGOYAMESHI.form.ShopRegisterForm;
import com.example.NAGOYAMESHI.repository.CategoryRepository;
import com.example.NAGOYAMESHI.repository.ShopRepository;
import com.example.NAGOYAMESHI.service.ShopService;

@Controller
@RequestMapping("/admin/shops")
public class AdminShopController {
	private final ShopRepository shopRepository;
	private final CategoryRepository categoryRepository;
	private final ShopService shopService;// ★ CategoryRepository のフィールドを追加 ★

	public AdminShopController(ShopRepository shopRepository, CategoryRepository categoryRepository,
			ShopService shopService) {
		this.shopRepository = shopRepository;
		this.categoryRepository = categoryRepository;
		this.shopService = shopService;
	}

	@GetMapping
	public String index(Model model,
			@PageableDefault(page = 0, size = 10, sort = "id", direction = Direction.ASC) Pageable pageable,
			@RequestParam(name = "keyword", required = false) String keyword) {
		Page<Shop> shopPage;

		if (keyword != null && !keyword.isEmpty()) {
			shopPage = shopRepository.findByNameLike("%" + keyword + "%", pageable);
		} else {
			shopPage = shopRepository.findAll(pageable);
		}

		model.addAttribute("shopPage", shopPage);
		model.addAttribute("keyword", keyword);

		return "admin/shops/index";
	}

	@GetMapping("/{id}")
	public String show(@PathVariable(name = "id") Integer id, Model model) {
	    Shop shop = shopRepository.findById(id)
	            .orElseThrow(() -> new IllegalArgumentException("Invalid shop Id:" + id));

	    List<String> condensedDaysWithHours = shopService.getCondensedOpeningHours(shop); // ★ ここで呼び出す ★
	    List<Category> shopCategories = new ArrayList<>(shop.getCategories());

	    model.addAttribute("shop", shop);
	    model.addAttribute("condensedDaysWithHours", condensedDaysWithHours);
	    model.addAttribute("shopCategories", shopCategories);

	    return "admin/shops/show";
	}

	



	@GetMapping("/register")
	public String register(Model model) {
		model.addAttribute("shopRegisterForm", new ShopRegisterForm());

		List<Category> categories = categoryRepository.findAll();
		model.addAttribute("categories", categories);

		return "admin/shops/register";
	}

	@PostMapping("/create")
	public String create(@Validated @ModelAttribute ShopRegisterForm shopRegisterForm, BindingResult result,
			Model model, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			model.addAttribute("categories", categoryRepository.findAll());
			return "admin/shops/register";
		}

		try {
			shopService.create(shopRegisterForm);
			redirectAttributes.addFlashAttribute("successMessage", "店舗を登録しました。");
			return "redirect:/admin/shops";
		} catch (IllegalArgumentException e) {
			model.addAttribute("categories", categoryRepository.findAll());
			model.addAttribute("errorMessage", e.getMessage());
			return "admin/shops/register";
		} catch (DataIntegrityViolationException e) {
			model.addAttribute("categories", categoryRepository.findAll());
			model.addAttribute("errorMessage", "登録に失敗しました。入力内容を確認してください。");
			return "admin/shops/register";
		}
	}

	@GetMapping("/{id}/edit")
	public String edit(@PathVariable(name = "id") Integer id, Model model) {
		Shop shop = shopRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid shop Id:" + id));
		String imageName = shop.getImageName();

		List<Integer> categoryIds = shop.getCategories().stream()
				.map(Category::getId)
				.toList();

		ShopEditForm shopEditForm = new ShopEditForm();
		shopEditForm.setId(shop.getId());
		shopEditForm.setName(shop.getName());
		shopEditForm.setDescription(shop.getDescription());
		shopEditForm.setPrice(shop.getPrice());
		shopEditForm.setCapacity(shop.getCapacity());
		shopEditForm.setPostalCode(shop.getPostalCode());
		shopEditForm.setAddress(shop.getAddress());
		shopEditForm.setPhoneNumber(shop.getPhoneNumber());
		shopEditForm.setMondayOpeningHours(shop.getMondayOpeningHours());
		shopEditForm.setMondayClosingHours(shop.getMondayClosingHours());
		shopEditForm.setTuesdayOpeningHours(shop.getTuesdayOpeningHours());
		shopEditForm.setTuesdayClosingHours(shop.getTuesdayClosingHours());
		shopEditForm.setWednesdayOpeningHours(shop.getWednesdayOpeningHours());
		shopEditForm.setWednesdayClosingHours(shop.getWednesdayClosingHours());
		shopEditForm.setThursdayOpeningHours(shop.getThursdayOpeningHours());
		shopEditForm.setThursdayClosingHours(shop.getThursdayClosingHours());
		shopEditForm.setFridayOpeningHours(shop.getFridayOpeningHours());
		shopEditForm.setFridayClosingHours(shop.getFridayClosingHours());
		shopEditForm.setSaturdayOpeningHours(shop.getSaturdayOpeningHours());
		shopEditForm.setSaturdayClosingHours(shop.getSaturdayClosingHours());
		shopEditForm.setSundayOpeningHours(shop.getSundayOpeningHours());
		shopEditForm.setSundayClosingHours(shop.getSundayClosingHours());
		shopEditForm.setCategoryIds(categoryIds);

		List<Category> categories = categoryRepository.findAll();
		model.addAttribute("imageName", imageName);
		model.addAttribute("shopEditForm", shopEditForm);
		model.addAttribute("categories", categories);

		return "admin/shops/edit";
	}

	@PostMapping("/{id}/update")
	public String update(@PathVariable(name = "id") Integer id,
			@Validated @ModelAttribute ShopEditForm shopEditForm,
			BindingResult result,
			Model model, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			model.addAttribute("imageName", shopRepository.findById(id).orElseThrow().getImageName());
			model.addAttribute("categories", categoryRepository.findAll());
			return "admin/shops/edit";
		}

		try {
			shopService.update(shopEditForm);
			redirectAttributes.addFlashAttribute("successMessage", "店舗情報を更新しました。");
			return "redirect:/admin/shops";
		} catch (IllegalArgumentException e) {
			model.addAttribute("imageName", shopRepository.findById(id).orElseThrow().getImageName());
			model.addAttribute("categories", categoryRepository.findAll());
			model.addAttribute("errorMessage", e.getMessage());
			return "admin/shops/edit";
		} catch (DataIntegrityViolationException e) {
			model.addAttribute("imageName", shopRepository.findById(id).orElseThrow().getImageName());
			model.addAttribute("categories", categoryRepository.findAll());
			model.addAttribute("errorMessage", "更新に失敗しました。入力内容を確認してください。");
			return "admin/shops/edit";
		}
	}
	@PostMapping("/{id}/delete")
	public String delete(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) {
		shopRepository.deleteById(id); 
		redirectAttributes.addFlashAttribute("successMessage", "店舗を削除しました。");
		return "redirect:/admin/shops";
	}
}