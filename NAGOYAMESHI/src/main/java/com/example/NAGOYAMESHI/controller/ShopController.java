package com.example.NAGOYAMESHI.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.NAGOYAMESHI.entity.Category;
import com.example.NAGOYAMESHI.entity.Favorite;
import com.example.NAGOYAMESHI.entity.Review;
import com.example.NAGOYAMESHI.entity.Shop;
import com.example.NAGOYAMESHI.entity.User;
import com.example.NAGOYAMESHI.form.ReservationInputForm;
import com.example.NAGOYAMESHI.repository.FavoriteRepository;
import com.example.NAGOYAMESHI.repository.ReviewRepository;
import com.example.NAGOYAMESHI.repository.ShopRepository;
import com.example.NAGOYAMESHI.security.UserDetailsImpl;
import com.example.NAGOYAMESHI.service.FavoriteService;
import com.example.NAGOYAMESHI.service.ShopService;

@Controller
@RequestMapping("/shops")
public class ShopController {
	private final ShopRepository shopRepository;
	private final ReviewRepository reviewRepository;
	private final FavoriteRepository favoriteRepository;
	private final FavoriteService favoriteService;
    private final ShopService shopService; // ★ ShopService のフィールドを追加 ★


	public ShopController(ShopRepository shopRepository, ReviewRepository reviewRepository,
			FavoriteRepository favoriteRepository, FavoriteService favoriteService, ShopService shopService) {
		this.shopRepository = shopRepository;
		this.reviewRepository = reviewRepository;
		this.favoriteRepository = favoriteRepository;
		this.favoriteService = favoriteService;
        this.shopService = shopService; // ★ ShopService のインスタンスを代入 ★

	}

    @GetMapping
    public String index(@RequestParam(name = "keyword", required = false) String keyword,
                        @RequestParam(name = "area", required = false) String area,
                        @RequestParam(name = "price", required = false) Integer price,
                        @RequestParam(name = "order", required = false) String order,
                        @RequestParam(name = "category", required = false) String category,
                        @PageableDefault(page = 0, size = 10, sort = "id", direction = Direction.ASC) Pageable pageable,
                        Model model) {

        Page<Shop> shopPage;

        if (category != null && !category.isEmpty()) {
            shopPage = shopRepository.findByCategories_Name(category, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            if (order != null && order.equals("priceAsc")) {
                shopPage = shopRepository.findByNameLikeOrAddressLikeOrderByPriceAsc("%" + keyword + "%",
                        "%" + keyword + "%", pageable);
            } else {
                shopPage = shopRepository.findByNameLikeOrAddressLikeOrderByCreatedAtDesc("%" + keyword + "%",
                        "%" + keyword + "%", pageable);
            }
        } else if (area != null && !area.isEmpty()) {
            if (order != null && order.equals("priceAsc")) {
                shopPage = shopRepository.findByAddressLikeOrderByPriceAsc("%" + area + "%", pageable);
            } else {
                shopPage = shopRepository.findByAddressLikeOrderByCreatedAtDesc("%" + area + "%", pageable);
            }
        } else if (price != null) {
            if (order != null && order.equals("priceAsc")) {
                shopPage = shopRepository.findByPriceLessThanEqualOrderByPriceAsc(price, pageable);
            } else {
                shopPage = shopRepository.findByPriceLessThanEqualOrderByCreatedAtDesc(price, pageable);
            }
        } else {
            if (order != null && order.equals("priceAsc")) {
                shopPage = shopRepository.findAllByOrderByPriceAsc(pageable);
            } else {
                shopPage = shopRepository.findAllByOrderByCreatedAtDesc(pageable);
            }
        }

        model.addAttribute("shopPage", shopPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("area", area);
        model.addAttribute("price", price);
        model.addAttribute("order", order);
        model.addAttribute("category", category);

        return "shops/index";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable(name = "id") Integer id, Model model,
                       @PageableDefault(page = 0, size = 6, sort = "id") Pageable pageable,
                       @ModelAttribute("reservationInputForm") ReservationInputForm reservationInputForm,
                       @AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {

        Shop shop = shopRepository.getReferenceById(id);
        Favorite favorite = null;
        boolean hasUserAlreadyReviewed = false;
        boolean isFavorite = false;

        if (userDetailsImpl != null) {
            User user = userDetailsImpl.getUser();
            isFavorite = favoriteService.isFavorite(shop, user);
            if (isFavorite) {
                favorite = favoriteRepository.findByShopAndUser(shop, user);
            }
        }

        Page<Review> reviewPage = reviewRepository.findByShopOrderByCreatedAtDesc(shop, pageable);

        List<String> condensedDaysWithHours = shopService.getCondensedOpeningHours(shop); // ★ ShopService のメソッドを呼び出す ★
        List<Category> shopCategories = new ArrayList<>(shop.getCategories());

        model.addAttribute("shop", shop);
        model.addAttribute("shopCategories", shopCategories);
        model.addAttribute("reservationInputForm", new ReservationInputForm());
        model.addAttribute("hasUserAlreadyReviewed", hasUserAlreadyReviewed);
        model.addAttribute("reviewPage", reviewPage);
        model.addAttribute("favorite", favorite);
        model.addAttribute("isFavorite", isFavorite);
        model.addAttribute("condensedDaysWithHours", condensedDaysWithHours); // ★ 結果をモデルに追加 ★

        return "shops/show";
    }

	

	

}
