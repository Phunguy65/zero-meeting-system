package io.github.phunguy65.zms.di;

import android.content.Context;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import io.github.phunguy65.zms.data.remote.api.AuthApi;
import io.github.phunguy65.zms.data.remote.api.ChatApi;
import io.github.phunguy65.zms.data.remote.api.InviteManagementApi;
import io.github.phunguy65.zms.data.remote.api.InviteTokensApi;
import io.github.phunguy65.zms.data.remote.api.JoinRequestsApi;
import io.github.phunguy65.zms.data.remote.api.MeApi;
import io.github.phunguy65.zms.data.remote.api.MeetingsApi;
import io.github.phunguy65.zms.data.remote.api.ParticipantsApi;
import io.github.phunguy65.zms.data.remote.api.RecordingsApi;
import io.github.phunguy65.zms.data.remote.api.UserMeetingsApi;
import io.github.phunguy65.zms.data.remote.api.UsersApi;
import io.github.phunguy65.zms.data.remote.interceptor.AndroidErrorTranslator;
import io.github.phunguy65.zms.data.remote.interceptor.AuthInterceptor;
import io.github.phunguy65.zms.data.remote.interceptor.ErrorTranslator;
import io.github.phunguy65.zms.data.remote.interceptor.JsendUnwrapInterceptor;
import io.github.phunguy65.zms.frontends.BuildConfig;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Hilt module providing network dependencies: OkHttpClient, Retrofit, and API interfaces.
 *
 * <p>The OkHttp client is configured with:
 * <ul>
 *   <li>{@link AuthInterceptor} — injects Bearer token for authenticated requests</li>
 *   <li>{@link JsendUnwrapInterceptor} — strips JSend envelopes before Retrofit deserialisation</li>
 *   <li>{@link HttpLoggingInterceptor} — logs HTTP traffic in debug builds</li>
 * </ul>
 *
 * <p>Interceptor ordering: AuthInterceptor is added first to inject the token, then JsendUnwrap
 * processes the response body before the logging interceptor. The logging interceptor therefore
 * logs the <em>unwrapped</em> payload, which keeps log output concise.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class NetworkModule {

    private static final int TIMEOUT_SECONDS = 30;

    @Provides
    @Singleton
    ErrorTranslator provideErrorTranslator(@ApplicationContext Context context) {
        return new AndroidErrorTranslator(context);
    }

    @Provides
    @Singleton
    JsendUnwrapInterceptor provideJsendUnwrapInterceptor(ErrorTranslator translator) {
        return new JsendUnwrapInterceptor(translator);
    }

    @Provides
    @Singleton
    HttpLoggingInterceptor provideLoggingInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(
                BuildConfig.DEBUG
                        ? HttpLoggingInterceptor.Level.BODY
                        : HttpLoggingInterceptor.Level.NONE);
        return interceptor;
    }

    @Provides
    @Singleton
    OkHttpClient provideOkHttpClient(
            AuthInterceptor authInterceptor,
            JsendUnwrapInterceptor jsendInterceptor,
            HttpLoggingInterceptor loggingInterceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(jsendInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    ObjectMapper provideObjectMapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Provides
    @Singleton
    Retrofit provideRetrofit(OkHttpClient client, ObjectMapper objectMapper) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build();
    }

    @Provides
    @Singleton
    AuthApi provideAuthApi(Retrofit retrofit) {
        return retrofit.create(AuthApi.class);
    }

    @Provides
    @Singleton
    MeApi provideMeApi(Retrofit retrofit) {
        return retrofit.create(MeApi.class);
    }

    @Provides
    @Singleton
    UsersApi provideUsersApi(Retrofit retrofit) {
        return retrofit.create(UsersApi.class);
    }

    @Provides
    @Singleton
    MeetingsApi provideMeetingsApi(Retrofit retrofit) {
        return retrofit.create(MeetingsApi.class);
    }

    @Provides
    @Singleton
    UserMeetingsApi provideUserMeetingsApi(Retrofit retrofit) {
        return retrofit.create(UserMeetingsApi.class);
    }

    @Provides
    @Singleton
    JoinRequestsApi provideJoinRequestsApi(Retrofit retrofit) {
        return retrofit.create(JoinRequestsApi.class);
    }

    @Provides
    @Singleton
    ParticipantsApi provideParticipantsApi(Retrofit retrofit) {
        return retrofit.create(ParticipantsApi.class);
    }

    @Provides
    @Singleton
    RecordingsApi provideRecordingsApi(Retrofit retrofit) {
        return retrofit.create(RecordingsApi.class);
    }

    @Provides
    @Singleton
    ChatApi provideChatApi(Retrofit retrofit) {
        return retrofit.create(ChatApi.class);
    }

    @Provides
    @Singleton
    InviteTokensApi provideInviteTokensApi(Retrofit retrofit) {
        return retrofit.create(InviteTokensApi.class);
    }

    @Provides
    @Singleton
    InviteManagementApi provideInviteManagementApi(Retrofit retrofit) {
        return retrofit.create(InviteManagementApi.class);
    }

    /**
     * Provides the LiveKit server URL from BuildConfig.
     */
    @Provides
    @Singleton
    @LiveKitUrl
    String provideLiveKitUrl() {
        return BuildConfig.LIVEKIT_URL;
    }
}
