package com.example.NAGOYAMESHI.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.NAGOYAMESHI.entity.Category;
import com.example.NAGOYAMESHI.form.CategoryForm;
import com.example.NAGOYAMESHI.repository.CategoryRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryRepository categoryRepository;

    public AdminCategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

 

    @GetMapping
    public String index(@RequestParam(name = "keyword", required = false) String keyword,
                        @PageableDefault(page = 0, size = 20) Pageable pageable,
                        Model model) {
        Page<Category> categoryPage;
        if (keyword != null && !keyword.isEmpty()) {
            categoryPage = categoryRepository.findByNameLike("%" + keyword + "%", pageable);
            model.addAttribute("keyword", keyword);
        } else {
            categoryPage = categoryRepository.findAll(pageable);
        }
        model.addAttribute("categories", categoryPage.getContent());
        model.addAttribute("page", categoryPage);
        return "admin/categories/index";
    }

    // カテゴリ登録フォーム表示
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        return "admin/categories/register";
    }

    // カテゴリ登録処理
    @PostMapping("/register")
    public String create(@Valid @ModelAttribute("categoryForm") CategoryForm categoryForm,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/categories/register";
        }
        if (categoryRepository.existsByName(categoryForm.getName())) {
            result.rejectValue("name", "error.categoryForm", "そのカテゴリ名は既に登録されています。");
            return "admin/categories/register";
        }

        Category category = new Category();
        category.setName(categoryForm.getName());
        categoryRepository.save(category);
        redirectAttributes.addFlashAttribute("successMessage", "カテゴリを登録しました。");
        return "redirect:/admin/categories";
    }

    // カテゴリ編集フォーム表示
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable("id") Integer id, Model model) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category Id:" + id));
        CategoryForm categoryForm = new CategoryForm();
        categoryForm.setId(category.getId());
        categoryForm.setName(category.getName());
        model.addAttribute("categoryForm", categoryForm);
        return "admin/categories/edit";
    }

    // カテゴリ編集処理
    @PostMapping("/{id}/edit")
    public String update(@PathVariable("id") Integer id,
                         @Valid @ModelAttribute("categoryForm") CategoryForm categoryForm,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            categoryForm.setId(id); // フォームにIDを保持
            return "admin/categories/edit";
        }
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category Id:" + id));
        if (!existingCategory.getName().equals(categoryForm.getName()) && categoryRepository.existsByName(categoryForm.getName())) {
            result.rejectValue("name", "error.categoryForm", "そのカテゴリ名は既に登録されています。");
            categoryForm.setId(id); // フォームにIDを保持
            return "admin/categories/edit";
        }

        existingCategory.setName(categoryForm.getName());
        categoryRepository.save(existingCategory);
        redirectAttributes.addFlashAttribute("successMessage", "カテゴリを更新しました。");
        return "redirect:/admin/categories";
    }

    // カテゴリ削除処理
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category Id:" + id));
        // 削除前に、このカテゴリを使用している店舗がないか確認するなどの処理を追加することも検討
        categoryRepository.delete(category);
        redirectAttributes.addFlashAttribute("successMessage", "カテゴリを削除しました。");
        return "redirect:/admin/categories";
    }
}