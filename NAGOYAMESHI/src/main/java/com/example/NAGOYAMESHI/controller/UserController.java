package com.example.NAGOYAMESHI.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // このimportがあることを確認
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.NAGOYAMESHI.entity.User;
import com.example.NAGOYAMESHI.form.SubscriptionRegisterForm; // もし使わないなら削除可能
import com.example.NAGOYAMESHI.form.UserEditForm;
import com.example.NAGOYAMESHI.repository.UserRepository;
import com.example.NAGOYAMESHI.security.UserDetailsImpl;
import com.example.NAGOYAMESHI.service.StripeService;
import com.example.NAGOYAMESHI.service.UserService;
import com.stripe.model.Customer;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final StripeService stripeService;

    // ★修正点★ application.propertiesからStripe公開可能キーを読み込むフィールド
    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    public UserController(UserRepository userRepository, UserService userService, StripeService stripeService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.stripeService = stripeService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, Model model) {
        User user = userRepository.getReferenceById(userDetailsImpl.getUser().getId());
        model.addAttribute("user", user);
        return "users/index";
    }

    @GetMapping("/edit")
    public String edit(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, Model model) {
        User user = userRepository.getReferenceById(userDetailsImpl.getUser().getId());
        UserEditForm userEditForm = new UserEditForm(user.getId(), user.getName(), user.getFurigana(),
                user.getPostalCode(), user.getAddress(), user.getPhoneNumber(), user.getEmail());
        model.addAttribute("userEditForm", userEditForm);
        return "users/edit";
    }

    @PostMapping("/update")
    public String update(@ModelAttribute @Validated UserEditForm userEditForm, BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (userService.isEmailChanged(userEditForm) && userService.isEmailRegistered(userEditForm.getEmail())) {
            FieldError fieldError = new FieldError(bindingResult.getObjectName(), "email", "すでに登録済みのメールアドレスです。");
            bindingResult.addError(fieldError);
        }
        if (userService.containsFullwidth(userEditForm.getPostalCode()) ||
                userService.containsFullwidth(userEditForm.getPhoneNumber())) {
            redirectAttributes.addFlashAttribute("errorMessage", "全角文字は使用できません。半角で入力してください。");
            return "users/edit";
        }

        if (bindingResult.hasErrors()) {
            return "user/edit";
        }

        userService.update(userEditForm);
        redirectAttributes.addFlashAttribute("successMessage", "会員情報を編集しました。");

        return "redirect:/user";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm() {
        return "users/password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String email,
            @RequestParam String new_password,
            @RequestParam String confirm_password,
            RedirectAttributes redirectAttributes) {

        if (!userService.isSamePassword(new_password, confirm_password)) {
            redirectAttributes.addFlashAttribute("errorMessage", "パスワードが一致しません。");
            return "redirect:/user/reset-password";
        }

        boolean result = userService.resetPassword(email, new_password);

        if (!result) {
            redirectAttributes.addFlashAttribute("errorMessage", "登録されていないメールアドレスです。");
            return "redirect:/user/reset-password";
        }

        redirectAttributes.addFlashAttribute("successMessage", "パスワードが正常に再設定されました。");
        return "redirect:/login";
    }

    @GetMapping("/subscribe")
    public String showSubscriptionForm(Model model) {
        // もしSubscriptionRegisterFormが不要であれば、この行は削除可能です。
        model.addAttribute("subscriptionRegisterForm", new SubscriptionRegisterForm());
        // ★修正点★ ハードコードされたキーではなく、クラスフィールドのstripePublishableKeyを使用
        model.addAttribute("stripePublishableKey", stripePublishableKey);
        return "users/subscribe";
    }

    @PostMapping("/create-checkout-session")
    @ResponseBody
    public String createCheckoutSession(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
            HttpServletRequest httpServletRequest) {
        User user = userDetailsImpl.getUser();

        SubscriptionRegisterForm subscriptionRegisterForm = new SubscriptionRegisterForm();
        subscriptionRegisterForm.setUserId(user.getId());

        try {
            String sessionId = stripeService.createStripeSubscriptionSession(subscriptionRegisterForm,
                    httpServletRequest);
            return sessionId;
        } catch (Exception e) {
            logger.error("Error creating checkout session:", e); // ロギングを強化
            return "error";
        }
    }

    @GetMapping("/subscription/success")
    public String subscriptionSuccess(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, String sessionId,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("successMessage", "有料会員登録が完了しました。");
        return "redirect:/user";
    }

    @GetMapping("/subscription/cancel")
    public String subscriptionCancel(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "有料会員登録がキャンセルされました。");
        return "redirect:/user/subscribe";
    }

    @GetMapping("/unsubscribe")
    public String showUnsubscribeForm(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, Model model) {
        User user = userRepository.getReferenceById(userDetailsImpl.getUser().getId());
        model.addAttribute("user", user);
        return "users/unsubscribe";
    }

    @PostMapping("/create-customer-portal-session")
    @ResponseBody
    public String createCustomerPortalSession(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
            HttpServletRequest request) {
        Integer userId = userDetailsImpl.getUser().getId();
        User user = userRepository.getReferenceById(userId);

        String portalSessionUrl = stripeService.createCustomerPortalSession(user.getStripeCustomerId(), request);

        if (portalSessionUrl != null && !portalSessionUrl.isEmpty()) {
            return portalSessionUrl;
        } else {
            logger.error("Failed to create customer portal session for user ID: {}", userId); // ロギングを強化
            return "";
        }
    }

    // --- ここから支払い方法更新のための追加 ---

    /**
     * 支払い方法更新フォームを表示するエンドポイント
     * @param userDetailsImpl 認証済みユーザー情報
     * @param model モデル
     * @return 支払い方法更新フォームのテンプレート名
     */
    @GetMapping("/payment/update")
    public String showUpdatePaymentMethodForm(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, Model model) {
        User user = userRepository.getReferenceById(userDetailsImpl.getUser().getId());

        if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isEmpty()) {
            model.addAttribute("errorMessage", "お支払い方法を更新するには、まず有料会員登録が必要です。");
            return "redirect:/user";
        }

        String clientSecret = stripeService.createSetupIntent(user.getStripeCustomerId());
        if (clientSecret == null) {
            model.addAttribute("errorMessage", "お支払い方法の更新準備中にエラーが発生しました。");
            return "redirect:/user";
        }
        model.addAttribute("clientSecret", clientSecret);
        // ★修正点★ ハードコードされたキーではなく、クラスフィールドのstripePublishableKeyを使用
        model.addAttribute("stripePublishableKey", stripePublishableKey);
        return "users/payment/update";
    }

    /**
     * フロントエンドからPaymentMethod IDを受け取り、支払い方法を更新するAPIエンドポイント
     * @param paymentMethodId フロントエンドから送信されたPaymentMethod ID
     * @param userDetailsImpl 認証済みユーザー情報
     * @return 成功/失敗メッセージ
     */
    @PostMapping("/payment/update-method")
    @ResponseBody
    public ResponseEntity<String> updatePaymentMethod(@RequestParam("paymentMethodId") String paymentMethodId,
            @AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
        Integer userId = userDetailsImpl.getUser().getId();
        User user = userRepository.getReferenceById(userId);

        if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isEmpty()) {
            logger.error("User {} does not have a Stripe Customer ID. Cannot update payment method.", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Stripe顧客IDが見つかりません。");
        }

        Customer updatedCustomer = stripeService.updateCustomerPaymentMethod(user.getStripeCustomerId(),
                paymentMethodId, true);

        if (updatedCustomer != null) {
            logger.info("Payment method updated successfully for user {}.", userId);
            return ResponseEntity.ok("支払い方法が正常に更新されました。");
        } else {
            logger.error("Failed to update payment method for user {}.", userId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("支払い方法の更新に失敗しました。");
        }
    }
}