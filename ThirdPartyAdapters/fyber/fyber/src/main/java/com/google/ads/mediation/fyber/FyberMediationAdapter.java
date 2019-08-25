package com.google.ads.mediation.fyber;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.fyber.inneractive.sdk.config.IAConfigManager;
import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListenerAdapter;
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListenerAdapter;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.google.android.gms.ads.AdFormat;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.mediation.VersionInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Fyber's official AdMob 3rd party adapter class
 * Implements Banners and interstitials by implementing the {@link MediationBannerAdapter} and {@link MediationInterstitialAdapter} interfaces
 * Implements initialization and Rewarded video ads, by extending the {@link Adapter} class
 */
public class FyberMediationAdapter extends Adapter
        implements MediationBannerAdapter, MediationInterstitialAdapter {
    // Definitions
    /**
     * Adapter class name for logging.
     */
    static final String TAG = FyberMediationAdapter.class.getSimpleName();

    /**
     * Fyber requires to know the host mediation platform
     */
    private final static InneractiveMediationName MEDIATOR_NAME = InneractiveMediationName.ADMOB;

    /**
     * Key to obtain App id, required for initializing Fyber's SDK.
     */
    static final String KEY_APP_ID = "applicationId";
    /**
     * Key to obtain a placement name or spot id. Required for creating a Fyber ad request
     */
    static final String KEY_SPOT_ID = "spotId";

    // Members
    /** Call initialize only once. Sometimes Adapter.initialize is not called. So also try to initialize the Fyber SDK from the requests */
    private boolean mInitializedCalled = false;

    // Banner related members

    /**
     * Admob's external banner listener
     */
    MediationBannerListener mMediationBannerListener;
    /**
     * Fyber's Spot object for the banner
     */
    InneractiveAdSpot mBannerSpot;

    /** Holds the banner view which is created by Fyber, in order to return when AdMob calls getView */
    private ViewGroup mBannerWrapperView;

    // Interstitial related members
    /**
     * Admob's external interstitial listener
     */
    MediationInterstitialListener mMediationInterstitialListener;
    /**
     * The context which was passed by AdMob to {@link #requestInterstitialAd}
    */
    WeakReference<Context> mInterstitialContext;
    /**
     * Fyber's spot object for interstitial
     */
    InneractiveAdSpot mInterstitialSpot;

    public FyberMediationAdapter() {
        Log.d(TAG, "FyberMediationAdapter ctor");
    }

    /**
     * Not supported. Use {@link MediationBannerAdapter#requestBannerAd} instead
     * @param configuration
     * @param callback
     */
    public void loadBannerAd(MediationBannerAdConfiguration configuration, MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> callback) {
        callback.onFailure(String.valueOf(this.getClass().getSimpleName()).concat(" does not support MediationBannerAd"));
    }

    /**
     * Not supported. Use {@link MediationInterstitialAdapter#requestInterstitialAd} instead
     * @param configuration
     * @param callback
     */
    public void loadInterstitialAd(MediationInterstitialAdConfiguration configuration, MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
        callback.onFailure(String.valueOf(this.getClass().getSimpleName()).concat(" does not support MediationInterstitialAd"));
    }

    /**
     * Only rewarded ads are implemented using the new Adapter interface
     * @param configuration
     * @param callback
     */
    public void loadRewardedAd(MediationRewardedAdConfiguration configuration, MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
        // Sometimes loadRewardedAd is called before initialize is called
        initializeFromBundle(configuration.getContext(), configuration.getServerParameters());

        FyberRewardedVideoRenderer rewardedVideoRenderer = new FyberRewardedVideoRenderer(configuration, callback);
        rewardedVideoRenderer.render();
    }

    /**
     * Native ads mediaqtion is not supported by the FyberMediationAdapter
     * @param var1
     * @param callback
     */
    public void loadNativeAd(MediationNativeAdConfiguration var1, MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
        Log.d(TAG, "loadNativeAd called");
        callback.onFailure(String.valueOf(this.getClass().getSimpleName()).concat(" does not support native ads."));
    }

    @Override
    public void initialize(Context context, final InitializationCompleteCallback completionCallback, List<MediationConfiguration> mediationConfigurations) {

        Log.d(TAG, "FyberMediationAdapter initialize called with: " + mediationConfigurations);

        String appId = null;

        // Get AppId from configuration
        for (MediationConfiguration configuration : mediationConfigurations) {
            Bundle serverParameters = configuration.getServerParameters();
            appId = serverParameters.getString(KEY_APP_ID);

            // Found an app id in server params
            if (!TextUtils.isEmpty(appId)) {
                break;
            }
        }

        if (TextUtils.isEmpty(appId)) {
            if (completionCallback != null) {
                completionCallback.onInitializationFailed("Fyber SDK requires an appId to be configured on the AdMob console");
            }

            Log.w(TAG, "No appId received from AdMob. Cannot initialize Fyber marketplace");
        } else {
            InneractiveAdManager.initialize(context, appId);

            IAConfigManager.addListener(new IAConfigManager.OnConfigurationReadyAndValidListener() {
                @Override
                public void onConfigurationReadyAndValid(IAConfigManager iaConfigManager, boolean success, Exception e) {
                    // Can be called more than once
                    if (completionCallback != null) {
                        if (success) {
                            completionCallback.onInitializationSucceeded();
                        } else {
                            completionCallback.onInitializationFailed("Fyber SDK initialization failed");
                            Log.d(TAG, "reporting initialization failed");
                        }
                    }

                    IAConfigManager.removeListener(this);
                }
            });
        }

    }

    public VersionInfo getVersionInfo() {
        String versionString = BuildConfig.VERSION_NAME;
        String splits[] = versionString.split("\\.");
        int major = Integer.parseInt(splits[0]);
        int minor = Integer.parseInt(splits[1]);
        int micro = Integer.parseInt(splits[2]) * 100 + Integer.parseInt(splits[3]);
        return new VersionInfo(major, minor, micro);
    }

    public VersionInfo getSDKVersionInfo() {
        String sdkVersion = InneractiveAdManager.getVersion();
        String splits[] = sdkVersion.split("\\.");
        int major = 0;
        int minor = 0;
        int micro = 0;
        if (splits.length > 2) {
            major = Integer.parseInt(splits[0]);
            minor = Integer.parseInt(splits[1]);
            micro = Integer.parseInt(splits[2]);
        } else if (splits.length == 2) {
            major = Integer.parseInt(splits[0]);
            minor = Integer.parseInt(splits[1]);
        } else if (splits.length == 1) {
            major = Integer.parseInt(splits[0]);
        }
        return new VersionInfo(major, minor, micro);
    }

    /*****************************************************
    /** MediationBannerAdapter implementation starts here
    ******************************************************/

    @Override
    public void requestBannerAd(final Context context, final MediationBannerListener mediationBannerListener, Bundle bundle, AdSize adSize,
                                MediationAdRequest mediationAdRequest, Bundle mediationExtras) {
        Log.d(TAG, "requestBannerAd called with bundle: " + bundle);
        initializeFromBundle(context, bundle);

        // Check that we got a valid spot id from the server
        String spotId = bundle.getString(FyberMediationAdapter.KEY_SPOT_ID);
        if (TextUtils.isEmpty(spotId)) {
            mediationBannerListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            Log.w(TAG, "Cannot render banner ad. Please define a valid spot id on the AdMob console");
            return;
        }

        mMediationBannerListener = mediationBannerListener;

        mBannerSpot = InneractiveAdSpotManager.get().createSpot();
        mBannerSpot.setMediationName(MEDIATOR_NAME);

        InneractiveAdViewUnitController controller = new InneractiveAdViewUnitController();
        mBannerSpot.addUnitController(controller);

        InneractiveAdRequest request = new InneractiveAdRequest(spotId);
        // Prepare wrapper view before making request
        mBannerWrapperView = new RelativeLayout(context);

        createFyberBannerAdListener();
        mBannerSpot.requestAd(request);
    }

    @Override
    public View getBannerView() {
        return mBannerWrapperView;
    }

    @Override
    public void onDestroy() {
        if (mBannerSpot != null) {
            mBannerSpot.destroy();
            mBannerSpot = null;
        }
    }

    @Override
    public void onPause() {
        // No relevant action. Refresh is disabled for banners
    }

    @Override
    public void onResume() {
        // No relevant action. Refresh is disabled for banners
    }

    /**
     * Creates Fyber's banner ad request listener
     */
    private void createFyberBannerAdListener() {
        mBannerSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {
            @Override
            public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot inneractiveAdSpot) {
                if(createFyberAdViewListener()) {
                    mMediationBannerListener.onAdLoaded(FyberMediationAdapter.this);
                } else {
                    mMediationBannerListener.onAdFailedToLoad(FyberMediationAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }

            @Override
            public void onInneractiveFailedAdRequest(InneractiveAdSpot inneractiveAdSpot, InneractiveErrorCode inneractiveErrorCode) {
                // Convert Fyber's Marketplace error code into AdMob's AdRequest error code
                int adMobErrorCode = AdRequest.ERROR_CODE_INTERNAL_ERROR;
                if (inneractiveErrorCode == InneractiveErrorCode.CONNECTION_ERROR
                        || inneractiveErrorCode == InneractiveErrorCode.CONNECTION_TIMEOUT) {
                    adMobErrorCode = AdRequest.ERROR_CODE_NETWORK_ERROR;
                } else if (inneractiveErrorCode == InneractiveErrorCode.NO_FILL) {
                    adMobErrorCode = AdRequest.ERROR_CODE_NO_FILL;
                }

                mMediationBannerListener.onAdFailedToLoad(FyberMediationAdapter.this, adMobErrorCode);
            }
        });
    }

    /**
     * When an ad is fetched successfully, creates a listener for Fyber's AdView events
     * @return true if created succesfully, false otherwise
     */
    private boolean createFyberAdViewListener() {
        // Just a double check that we have the right time of selected controller
        if (mBannerSpot == null || false == (mBannerSpot.getSelectedUnitController() instanceof InneractiveAdViewUnitController)) {
            return false;
        }

        InneractiveAdViewUnitController controller = (InneractiveAdViewUnitController)mBannerSpot.getSelectedUnitController();

        // Bind the wrapper view
        controller.bindView(mBannerWrapperView);

        InneractiveAdViewEventsListener adViewListener = new InneractiveAdViewEventsListenerAdapter() {
            @Override
            public void onAdImpression(InneractiveAdSpot adSpot) {
                mMediationBannerListener.onAdOpened(FyberMediationAdapter.this);
            }

            @Override
            public void onAdClicked(InneractiveAdSpot adSpot) {
                mMediationBannerListener.onAdClicked(FyberMediationAdapter.this);
            }

            @Override
            public void onAdWillCloseInternalBrowser(InneractiveAdSpot adSpot) {
                mMediationBannerListener.onAdClosed(FyberMediationAdapter.this);
            }

            @Override
            public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
                mMediationBannerListener.onAdLeftApplication(FyberMediationAdapter.this);
            }
        };

        controller.setEventsListener(adViewListener);

        return true;
    }

    /** MediationInterstitialAdapter implementation */

    @Override
    public void requestInterstitialAd(Context context, final MediationInterstitialListener mediationInterstitialListener, Bundle bundle, MediationAdRequest mediationAdRequest, Bundle mediationExtras) {
        Log.d(TAG, "requestInterstitialAd called with bundle: " + bundle);

        initializeFromBundle(context, bundle);

        /** Cache the context for showInterstitial */
        mInterstitialContext = new WeakReference<>(context);

        // Check that we got a valid spot id from the server
        String spotId = bundle.getString(FyberMediationAdapter.KEY_SPOT_ID);
        if (TextUtils.isEmpty(spotId)) {
            mediationInterstitialListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            Log.w(TAG, "Cannot render banner ad. Please define a valid spot id on the AdMob console");
            return;
        }

        mMediationInterstitialListener = mediationInterstitialListener;

        mInterstitialSpot = InneractiveAdSpotManager.get().createSpot();
        mInterstitialSpot.setMediationName(MEDIATOR_NAME);

        InneractiveFullscreenUnitController controller = new InneractiveFullscreenUnitController();
        mInterstitialSpot.addUnitController(controller);

        InneractiveAdRequest request = new InneractiveAdRequest(spotId);

        createFyberInterstitialAdListener();
        mInterstitialSpot.requestAd(request);
    }

    @Override
    public void showInterstitial() {
        if (mInterstitialSpot != null && mInterstitialSpot.getSelectedUnitController() instanceof InneractiveFullscreenUnitController) {
            Context context = mInterstitialContext != null ? mInterstitialContext.get() : null;
            if (context != null) {
                ((InneractiveFullscreenUnitController) mInterstitialSpot.getSelectedUnitController()).show(context);
            } else {
                Log.w(TAG, "showInterstitial called, but context reference was lost");
            }
        } else {
            Log.w(TAG, "showInterstitial called, but spot is not ready for show? Should never happen");
        }
    }

    private void createFyberInterstitialAdListener() {
        mInterstitialSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {
            @Override
            public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
                if(createFyberInterstitialListener()) {
                    mMediationInterstitialListener.onAdLoaded(FyberMediationAdapter.this);
                } else {
                    mMediationInterstitialListener.onAdFailedToLoad(FyberMediationAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }

            @Override
            public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
                                                     InneractiveErrorCode inneractiveErrorCode) {
                // Convert Fyber's Marketplace error code into AdMob's AdRequest error code
                int adMobErrorCode = AdRequest.ERROR_CODE_INTERNAL_ERROR;
                if (inneractiveErrorCode == InneractiveErrorCode.CONNECTION_ERROR
                        || inneractiveErrorCode == InneractiveErrorCode.CONNECTION_TIMEOUT) {
                    adMobErrorCode = AdRequest.ERROR_CODE_NETWORK_ERROR;
                } else if (inneractiveErrorCode == InneractiveErrorCode.NO_FILL) {
                    adMobErrorCode = AdRequest.ERROR_CODE_NO_FILL;
                }

                mMediationInterstitialListener.onAdFailedToLoad(FyberMediationAdapter.this, adMobErrorCode);
            }
        });
    }

    /**
     * When an ad is fetched successfully, creates a listener for Fyber's Interstitial events
     * @return true if created succesfully, false otherwise
     */
    private boolean createFyberInterstitialListener() {
        // Just a double check that we have the right time of selected controller
        if (mInterstitialSpot == null || false == (mInterstitialSpot.getSelectedUnitController() instanceof InneractiveFullscreenUnitController)) {
            return false;
        }

        InneractiveFullscreenUnitController controller = (InneractiveFullscreenUnitController)mInterstitialSpot.getSelectedUnitController();

        InneractiveFullscreenAdEventsListener interstitalListener = new InneractiveFullscreenAdEventsListenerAdapter() {
            @Override
            public void onAdImpression(InneractiveAdSpot adSpot) {
                mMediationInterstitialListener.onAdOpened(FyberMediationAdapter.this);
            }

            @Override
            public void onAdClicked(InneractiveAdSpot adSpot) {
                mMediationInterstitialListener.onAdClicked(FyberMediationAdapter.this);
            }

            @Override
            public void onAdDismissed(InneractiveAdSpot adSpot) {
                mMediationInterstitialListener.onAdClosed(FyberMediationAdapter.this);
            }

            @Override
            public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
                mMediationInterstitialListener.onAdLeftApplication(FyberMediationAdapter.this);
            }
        };

        controller.setEventsListener(interstitalListener);

        return true;
    }

    /**
     *  Helper method for calling the initialization method, if it wasn't called by Admob
     * @param context
     * @param bundle
     */
    private void initializeFromBundle(Context context, Bundle bundle) {
        if (mInitializedCalled == false) {
            List<MediationConfiguration> configs = new ArrayList<>();
            configs.add(new MediationConfiguration(AdFormat.BANNER, bundle));

            initialize(context, null, configs);
        }
    }

}
