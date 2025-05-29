package com.example.NAGOYAMESHI.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.NAGOYAMESHI.form.SubscriptionRegisterForm;
import com.example.NAGOYAMESHI.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer; // Customer オブジェクト用
import com.stripe.model.PaymentMethod; // PaymentMethod オブジェクト用
import com.stripe.model.SetupIntent; // SetupIntent オブジェクト用
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerUpdateParams; // Customer更新用
import com.stripe.param.PaymentMethodAttachParams; // PaymentMethod紐付け用
import com.stripe.param.SetupIntentCreateParams; // SetupIntent作成用
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class StripeService {
	private static final Logger logger = LoggerFactory.getLogger(StripeService.class);
	private final UserRepository userRepository;

	@Value("${stripe.secret-key}")
	private String stripeApiKey;

	@Value("${stripe.price-id}")
	private String stripePriceId;

	@Value("${app.url}")
	private String appBaseUrl;

	public StripeService(UserRepository userRepository) {
		this.userRepository= userRepository;
	}
	
	
	// (既存の createStripeSubscriptionSession メソッドは省略)

	public String createStripeSubscriptionSession(SubscriptionRegisterForm subscriptionRegisterForm, HttpServletRequest httpServletRequest) {
        Stripe.apiKey = stripeApiKey;

        logger.info("createStripeSubscriptionSession called. Using Price ID: {}", stripePriceId);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", subscriptionRegisterForm.getUserId().toString());
        metadata.put("planName", "有料会員の登録（サブスクリプション）");
        metadata.put("amount", "300");

        SessionCreateParams params =
            SessionCreateParams.builder()
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(stripePriceId)
                        .setQuantity(1L)
                        .build())
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(appBaseUrl + "/user/subscription/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(appBaseUrl + "/user/subscription/cancel")
                .putAllMetadata(metadata)

                .build();
        try {
            Session session = Session.create(params);
            logger.info("Stripe Checkout Session created successfully with ID: {}", session.getId());
            return session.getId();
        } catch (StripeException e) {
            logger.error("Failed to create Stripe Checkout Session.", e);
            e.printStackTrace();
            return "";
        }
    }

	// (既存の createCustomerPortalSession メソッドは省略)

	public String createCustomerPortalSession(String stripeCustomerId, HttpServletRequest httpServletRequest) {
		Stripe.apiKey = stripeApiKey;

		logger.info("createCustomerPortalSession called. Customer ID: {}", stripeCustomerId);

		try {
			com.stripe.param.billingportal.SessionCreateParams params = com.stripe.param.billingportal.SessionCreateParams
					.builder()
					.setCustomer(stripeCustomerId)
					.setReturnUrl(appBaseUrl + "/user")
					.build();

			com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session
					.create(params);
			logger.info("Stripe Customer Portal Session created successfully with URL: {}", portalSession.getUrl());
			return portalSession.getUrl();

		} catch (StripeException e) {
			logger.error("Failed to create Stripe Customer Portal Session for customer ID: {}", stripeCustomerId, e);
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * 新しい支払い方法のためのSetup Intentを作成し、クライアントシークレットを返します。
	 * これにより、フロントエンドは安全にカード情報を収集し、PaymentMethodを作成できます。
	 * @param stripeCustomerId Stripe顧客ID
	 * @return Setup Intentのクライアントシークレット、またはエラー時にnull
	 */
	public String createSetupIntent(String stripeCustomerId) {
		Stripe.apiKey = stripeApiKey;

		try {
			SetupIntentCreateParams params = SetupIntentCreateParams.builder()
					.setCustomer(stripeCustomerId) // 顧客IDを関連付ける
					.build();
			SetupIntent setupIntent = SetupIntent.create(params);
			logger.info("Setup Intent created successfully for customer {}: {}", stripeCustomerId, setupIntent.getId());
			return setupIntent.getClientSecret();
		} catch (StripeException e) {
			logger.error("Failed to create Setup Intent for customer {}.", stripeCustomerId, e);
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 指定されたPaymentMethodを顧客にアタッチし、必要であればデフォルトの支払い方法に設定します。
	 * @param stripeCustomerId Stripe顧客ID
	 * @param paymentMethodId アタッチするPaymentMethodのID
	 * @param setDefault デフォルトの支払い方法として設定するかどうか
	 * @return 更新されたCustomerオブジェクト、またはエラー時にnull
	 */
	public Customer updateCustomerPaymentMethod(String stripeCustomerId, String paymentMethodId, boolean setDefault) {
		Stripe.apiKey = stripeApiKey;

		try {
			// PaymentMethodを顧客にアタッチ
			PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
			PaymentMethodAttachParams attachParams = PaymentMethodAttachParams.builder()
					.setCustomer(stripeCustomerId)
					.build();
			paymentMethod.attach(attachParams);
			logger.info("PaymentMethod {} attached to customer {}.", paymentMethodId, stripeCustomerId);

			Customer customer = Customer.retrieve(stripeCustomerId);

			if (setDefault) {
				// デフォルトの支払い方法として設定
				CustomerUpdateParams updateParams = CustomerUpdateParams.builder()
						.setInvoiceSettings(CustomerUpdateParams.InvoiceSettings.builder()
								.setDefaultPaymentMethod(paymentMethod.getId())
								.build())
						.build();
				customer = customer.update(updateParams);
				logger.info("Default PaymentMethod for customer {} updated to {}.", stripeCustomerId, paymentMethodId);
			}
			return customer;
		} catch (StripeException e) {
			logger.error("Failed to update payment method for customer {}.", stripeCustomerId, e);
			e.printStackTrace();
			return null;
		}
	}
}