// public/js/subscribe.js

document.addEventListener('DOMContentLoaded', async function () {
    console.log("subscribe.js loaded.");

    const subscribeButton = document.getElementById('paymentButton');

    if (!subscribeButton) {
        console.warn("Subscription button with ID 'paymentButton' not found.");
        return;
    }

    subscribeButton.addEventListener('click', async function () {
        this.disabled = true;

        const csrfMeta = document.querySelector('meta[name="_csrf"]');
        const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
        const headers = { 'Content-Type': 'application/json' };

        if (csrfMeta && csrfHeaderMeta) {
            headers[csrfHeaderMeta.getAttribute('content')] = csrfMeta.getAttribute('content');
        }

        try {
            const response = await fetch('/user/create-checkout-session', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({})
            });

            console.log("Fetch API response received:", response);
            if (!response.ok) {
                const errorText = await response.text();
                console.error("Server error response:", errorText);
                throw new Error(errorText);
            }

            const sessionId = await response.text();
            console.log("Received sessionId:", sessionId);

            if (sessionId && sessionId !== 'error') {
                setTimeout(() => {
                    // 複数のmetaタグがある場合に備えて、content属性ありのものを取得
                    const stripeMetaTags = document.querySelectorAll('meta[name="stripe-public-key"]');
                    let stripePublicKey = null;

                    stripeMetaTags.forEach(meta => {
                        const content = meta.getAttribute('content');
                        if (content && content.trim() !== '') {
                            stripePublicKey = content;
                        }
                    });

                    if (!stripePublicKey) {
                        console.error("Stripe public key not found in any meta tag.");
                        alert('Stripe設定エラー: 公開キーが見つかりません。');
                        subscribeButton.disabled = false;
                        return;
                    }

                    console.log("Using Stripe public key:", stripePublicKey);

                    /** @type {import('@stripe/stripe-js').Stripe} */
                    const stripe = Stripe(stripePublicKey);

                    stripe.redirectToCheckout({ sessionId: sessionId }).then(function (result) {
                        if (result.error) {
                            console.error("Stripe Checkout エラー:", result.error.message);
                            alert('決済中にエラーが発生しました: ' + result.error.message);
                        }
                    });

                }, 50); // わずかな遅延でDOM反映を待つ
            } else {
                alert('決済セッションの作成に失敗しました。');
            }
        } catch (error) {
            console.error('API呼び出しエラー:', error);
            alert('決済セッションの開始中にエラーが発生しました: ' + error.message);
        } finally {
            this.disabled = false;
        }
    });
});
