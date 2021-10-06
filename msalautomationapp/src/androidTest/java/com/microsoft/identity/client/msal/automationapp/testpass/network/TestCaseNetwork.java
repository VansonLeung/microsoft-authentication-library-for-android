package com.microsoft.identity.client.msal.automationapp.testpass.network;

import android.content.SharedPreferences;
import android.util.Log;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.NetworkStatesFile;
import com.microsoft.identity.client.ui.automation.annotations.NetworkTestTimeout;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.network.runners.NetworkTestRunner;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.common.java.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;

@RunWith(NetworkTestRunner.class)
public class TestCaseNetwork extends BaseMsalUiNetworkTest {

    @Override
    public void setup() {
        Log.d("NetworkTestRule", "Running test setup");
        super.setup();
    }

    @Override
    public void cleanup() {
        Log.d("NetworkTestRule", "Running test clean up.");
        super.cleanup();

        boolean mainActivityFocused = mActivity.hasWindowFocus();

        if (!mainActivityFocused) {
            UiAutomatorUtils.pressBack();
            Log.d("NetworkTestRule", "Pressing the back button...");
        }

        CommonUtils.deleteDirectory(mContext.getCacheDir());
        CommonUtils.deleteDirectory(mContext.getDir("webview", 0));
        mContext.deleteSharedPreferences(SharedPreferencesAccountCredentialCache.DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES);
    }

    @NetworkStatesFile("input_acquireTokenWithoutBroker.csv")
    @NetworkTestTimeout(seconds = 120)
    @Test
    public void test_acquireTokenWithoutBroker() {
        Log.d("NetworkTestRule", "Running test....");
        TokenRequestLatch tokenRequestLatch = new TokenRequestLatch(1);
        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mLoginHint)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(tokenRequestLatch))
                .withPrompt(Prompt.SELECT_ACCOUNT)
                .build();


        final InteractiveRequest interactiveRequest = new InteractiveRequest(
                mApplication,
                parameters,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        final String username = mLoginHint;
                        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(mLoginHint)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequest.execute();

        tokenRequestLatch.await(TokenRequestTimeout.SHORT);

        mBrowser.clear();
        mAccount = null;
    }

    @NetworkStatesFile("input_acquireTokenSilentWithoutBroker.csv")
    @NetworkTestTimeout(seconds = 120)
    @Test
    public void test_acquireTokenSilentWithoutBroker() {
        final TokenRequestLatch latch = new TokenRequestLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mLoginHint)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(latch))
                .withPrompt(Prompt.SELECT_ACCOUNT)
                .build();


        final InteractiveRequest interactiveRequest = new InteractiveRequest(
                mApplication,
                parameters,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        final String username = mLoginHint;
                        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(mLoginHint)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequest.execute();
        latch.await(TokenRequestTimeout.SHORT);

        IAccount account = getAccount();

        final int numberOfTests = 10;
        for (int i = 0; i < numberOfTests; i++) {
            TokenRequestLatch tokenRequestLatch = new TokenRequestLatch(1);

            final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                    .forAccount(account)
                    .fromAuthority(account.getAuthority())
                    .forceRefresh(true)
                    .withScopes(Arrays.asList(mScopes))
                    .withCallback(successfulSilentCallback(tokenRequestLatch))
                    .build();

            mApplication.acquireTokenSilentAsync(silentParameters);

            tokenRequestLatch.await(TokenRequestTimeout.SILENT);
        }
    }
}
