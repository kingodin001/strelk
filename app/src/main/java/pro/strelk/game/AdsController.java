package pro.strelk.game;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.YandexAds;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoader;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;

/** Advertising is genuinely opt-in. Never grant rewards unless the SDK calls onRewarded(). */
public final class AdsController {
    private final Activity activity;
    private final boolean rewardedEnabled, interstitialEnabled;
    private RewardedAdLoader rewardedLoader;
    private RewardedAd rewarded;
    private InterstitialAdLoader interstitialLoader;
    private InterstitialAd interstitial;
    private boolean rewardedInFlight;

    public AdsController(Activity activity) {
        this.activity = activity;
        rewardedEnabled = validId(BuildConfig.REWARDED_ID);
        interstitialEnabled = validId(BuildConfig.INTERSTITIAL_ID);
        if (rewardedEnabled || interstitialEnabled) {
            // Automatic SDK initialization is disabled in Manifest until real IDs exist.
            YandexAds.initialize(activity, () -> {
                if (rewardedEnabled) {
                    rewardedLoader = new RewardedAdLoader(activity);
                    loadRewarded();
                }
                if (interstitialEnabled) {
                    interstitialLoader = new InterstitialAdLoader(activity);
                    loadInterstitial();
                }
            });
        }
    }

    private boolean validId(String id) {
        return id != null && (id.matches("R-M-[0-9]+-[0-9]+") ||
                (BuildConfig.DEBUG && id.startsWith("demo-")));
    }
    public boolean readyForReward() { return rewarded != null && !rewardedInFlight; }
    public boolean hasConfiguredReward() { return rewardedEnabled; }

    private void loadRewarded() {
        if (rewardedLoader == null || !rewardedEnabled) return;
        rewardedLoader.loadAd(new AdRequest.Builder(BuildConfig.REWARDED_ID).build(), new RewardedAdLoadListener() {
            @Override public void onAdLoaded(@NonNull RewardedAd ad) { rewarded = ad; }
            @Override public void onAdFailedToLoad(@NonNull AdRequestError error) {
                rewarded = null; // No immediate retries on failure: SDK recommendation.
            }
        });
    }

    public boolean showReward(final Runnable grant) {
        if (!readyForReward()) return false;
        rewardedInFlight = true;
        final RewardedAd ad = rewarded;
        rewarded = null;
        ad.setAdEventListener(new RewardedAdEventListener() {
            private boolean delivered = false;
            private boolean closed = false;
            private void done() {
                if (closed) return;
                closed = true;
                ad.setAdEventListener(null);
                rewardedInFlight = false;
                loadRewarded();
            }
            @Override public void onAdShown() {}
            @Override public void onAdFailedToShow(@NonNull AdError error) { done(); }
            @Override public void onAdDismissed() { done(); }
            @Override public void onAdClicked() {}
            @Override public void onAdImpression(@Nullable ImpressionData data) {}
            @Override public void onRewarded(@NonNull Reward reward) {
                if (!delivered) { delivered = true; grant.run(); }
            }
        });
        ad.show(activity);
        return true;
    }

    private void loadInterstitial() {
        if (interstitialLoader == null || !interstitialEnabled) return;
        interstitialLoader.loadAd(new AdRequest.Builder(BuildConfig.INTERSTITIAL_ID).build(), new InterstitialAdLoadListener() {
            @Override public void onAdLoaded(@NonNull InterstitialAd ad) { interstitial = ad; }
            @Override public void onAdFailedToLoad(@NonNull AdRequestError error) { interstitial = null; }
        });
    }

    public boolean showInterstitialAfterLevel(GameStorage storage) {
        // Only after level 3, every third win, with 180 seconds minimum between fullscreen ads.
        long now = System.currentTimeMillis();
        if (interstitial == null || storage.wins() < 3 || storage.wins() % 3 != 0 ||
                now - storage.lastInterstitialAt() < 180_000L) return false;
        final InterstitialAd ad = interstitial;
        interstitial = null;
        ad.setAdEventListener(new InterstitialAdEventListener() {
            private void done() { ad.setAdEventListener(null); loadInterstitial(); }
            @Override public void onAdShown() {}
            @Override public void onAdFailedToShow(@NonNull AdError error) { done(); }
            @Override public void onAdDismissed() { done(); }
            @Override public void onAdClicked() {}
            @Override public void onAdImpression(@Nullable ImpressionData data) {}
        });
        storage.recordInterstitial(now);
        ad.show(activity);
        return true;
    }

    public void destroy() {
        if (rewarded != null) rewarded.setAdEventListener(null);
        if (interstitial != null) interstitial.setAdEventListener(null);
        rewarded = null;
        interstitial = null;
        rewardedLoader = null;
        interstitialLoader = null;
    }
}
