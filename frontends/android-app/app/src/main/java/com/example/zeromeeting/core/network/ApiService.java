package com.example.zeromeeting.core.network;

import com.example.zeromeeting.core.model.auth.GoogleLoginRequest;
import com.example.zeromeeting.core.model.auth.LoginRequest;
import com.example.zeromeeting.core.model.auth.LoginResponse;
import com.example.zeromeeting.core.model.auth.LogoutRequest;
import com.example.zeromeeting.core.model.auth.RefreshTokenRequest;
import com.example.zeromeeting.core.model.auth.RegisterRequest;
import com.example.zeromeeting.core.model.auth.RegisterResponse;
import com.example.zeromeeting.core.model.chat.ChatMessage;
import com.example.zeromeeting.core.model.chat.ChatRoom;
import com.example.zeromeeting.core.model.chat.SendMessageRequest;
import com.example.zeromeeting.core.model.meeting.ApproveAllResponse;
import com.example.zeromeeting.core.model.meeting.JoinMeetingRequest;
import com.example.zeromeeting.core.model.meeting.JoinRequestItem;
import com.example.zeromeeting.core.model.meeting.Meeting;
import com.example.zeromeeting.core.model.meeting.ParticipantItem;
import com.example.zeromeeting.core.model.meeting.RequestJoinResponse;
import com.example.zeromeeting.core.model.meeting.ScheduleMeetingRequest;
import com.example.zeromeeting.core.model.user.DeleteAccountResponse;
import com.example.zeromeeting.core.model.user.PatchUserRequest;
import com.example.zeromeeting.core.model.user.UserPreferencesResponse;
import com.example.zeromeeting.core.model.user.UserResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

// Import các file Model sau khi chúng ta tạo ở bước tiếp theo
// import com.example.zeromeeting.core.model.auth.*;
// import com.example.zeromeeting.core.model.user.*;

public interface ApiService {

    // ==========================================
    // MODULE: AUTHENTICATION (XÁC THỰC)
    // ==========================================

    @POST("api/v1/auth/login")
    Call<JsendResponse<LoginResponse>> login(@Body LoginRequest request);

    @POST("api/v1/auth/register")
    Call<JsendResponse<RegisterResponse>> register(@Body RegisterRequest request);

    @POST("api/v1/auth/refresh")
    Call<JsendResponse<LoginResponse>> refreshToken(@Body RefreshTokenRequest request);

    @POST("api/v1/auth/logout")
    Call<JsendResponse<Void>> logout(@Body LogoutRequest request);

    @POST("api/v1/auth/google-login")
    Call<JsendResponse<LoginResponse>> googleLogin(@Body GoogleLoginRequest request);

    // ==========================================
    // MODULE: ME (TÀI KHOẢN CÁ NHÂN)
    // ==========================================

    @GET("api/v1/me")
    Call<JsendResponse<UserResponse>> getMe();

    @PATCH("api/v1/me")
    Call<JsendResponse<UserResponse>> updateMe(@Body PatchUserRequest request);

    @DELETE("api/v1/me")
    Call<JsendResponse<DeleteAccountResponse>> deleteMe();

    @GET("api/v1/me/preferences")
    Call<JsendResponse<UserPreferencesResponse>> getMyPreferences();

    @PATCH("api/v1/me/preferences")
    Call<JsendResponse<UserPreferencesResponse>> updateMyPreferences(@Body Object request); // Tạm để Object, cập nhật sau khi có schema

    // ==========================================
    // MODULE: USERS (TÌM KIẾM NGƯỜI DÙNG)
    // ==========================================

    @GET("api/v1/users:search")
    Call<JsendResponse<CursorScrollResponse<UserResponse>>> searchUsers(
        @QueryMap Map<String, Object> options
    );

    // ==========================================
    // MODULE: MEETINGS (PHÒNG HỌP)
    // ==========================================

    @POST("api/v1/meetings:schedule")
    Call<JsendResponse<Meeting>> scheduleMeeting(@Body ScheduleMeetingRequest request);

    @POST("api/v1/meetings:instant")
    Call<JsendResponse<Meeting>> createInstantMeeting(@Body Object request); // CreateInstantMeetingRequest

    @GET("api/v1/meetings/{id}")
    Call<JsendResponse<Meeting>> getMeetingDetail(@retrofit2.http.Path("id") String id);

    @POST("api/v1/meetings/{id}:start")
    Call<JsendResponse<Void>> startMeeting(@retrofit2.http.Path("id") String id);

    @POST("api/v1/meetings/{id}:end")
    Call<JsendResponse<Void>> endMeeting(@retrofit2.http.Path("id") String id);

    // ==========================================
    // MODULE: PARTICIPANTS & JOIN REQUESTS (NGƯỜI THAM GIA)
    // ==========================================

    @GET("api/v1/meetings/{id}/participants")
    Call<JsendResponse<List<ParticipantItem>>> getParticipants(@retrofit2.http.Path("id") String id);

    @POST("api/v1/meetings/{id}/participants:kick")
    Call<JsendResponse<Void>> kickParticipant(
        @retrofit2.http.Path("id") String meetingId,
        @Query("userId") String userId,
        @Query("displayName") String displayName
    );

    @POST("api/v1/meetings/{id}:requestJoin")
    Call<JsendResponse<RequestJoinResponse>> requestJoin(
        @retrofit2.http.Path("id") String meetingId,
        @Body JoinMeetingRequest request
    );

    @GET("api/v1/meetings/{id}/joinRequests")
    Call<JsendResponse<OffsetScrollResponse<JoinRequestItem>>> getJoinRequests(
        @retrofit2.http.Path("id") String id,
        @Query("pageSize") Integer pageSize,
        @Query("offset") Integer offset
    );

    @POST("api/v1/meetings/{id}/joinRequests:approveAll")
    Call<JsendResponse<ApproveAllResponse>> approveAllJoinRequests(@retrofit2.http.Path("id") String id);

    @POST("api/v1/meetings/{id}/joinRequests/{requestId}:approve")
    Call<JsendResponse<String>> approveJoinRequest(
        @retrofit2.http.Path("id") String meetingId,
        @retrofit2.http.Path("requestId") String requestId
    );

    @POST("api/v1/meetings/{id}/joinRequests/{requestId}:deny")
    Call<JsendResponse<Void>> denyJoinRequest(
        @retrofit2.http.Path("id") String meetingId,
        @retrofit2.http.Path("requestId") String requestId
    );

    // ==========================================
    // MODULE: CHAT (TRÒ CHUYỆN TRONG PHÒNG)
    // ==========================================

    @GET("api/v1/chat/rooms/{roomId}")
    Call<JsendResponse<ChatRoom>> getChatRoom(@retrofit2.http.Path("roomId") String roomId);

    @GET("api/v1/chat/rooms/{roomId}/messages")
    Call<JsendResponse<CursorScrollResponse<ChatMessage>>> getChatMessages(
        @retrofit2.http.Path("roomId") String roomId,
        @Query("size") Integer size,
        @Query("beforeSeqNum") Long beforeSeqNum
    );

    @POST("api/v1/chat/rooms/{roomId}/messages")
    Call<JsendResponse<ChatMessage>> sendMessage(
        @retrofit2.http.Path("roomId") String roomId,
        @Body SendMessageRequest request
    );
}
