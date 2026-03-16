package io.github.phunguy65.zms.usermanagement.infrastructure.grpc;

import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import io.github.phunguy65.zms.proto.user.v1.BatchGetUserRequest;
import io.github.phunguy65.zms.proto.user.v1.BatchGetUserResponse;
import io.github.phunguy65.zms.proto.user.v1.UserServiceGrpc;
import io.github.phunguy65.zms.proto.user.v1.UserSnapshot;
import io.github.phunguy65.zms.usermanagement.infrastructure.persistence.UserJpaEntity;
import io.github.phunguy65.zms.usermanagement.infrastructure.persistence.UserJpaRepository;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * gRPC server implementation for the UserService.
 *
 * <p>Resolves users by email or username and returns full {@link UserSnapshot} state.
 * Deleted users are excluded. Partial results are returned — missing identifiers are absent
 * from the response map.
 */
@Service
public class UserServiceGrpcImpl extends UserServiceGrpc.UserServiceImplBase {

    private final UserJpaRepository userJpaRepository;

    public UserServiceGrpcImpl(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void batchGetUser(
            BatchGetUserRequest request, StreamObserver<BatchGetUserResponse> responseObserver) {
        var responseBuilder = BatchGetUserResponse.newBuilder();

        for (String email : request.getEmailsList()) {
            userJpaRepository
                    .findByEmailAndDeletedAtIsNull(email)
                    .ifPresent(user -> responseBuilder.putUsers(email, toSnapshot(user)));
        }

        for (String username : request.getUsernamesList()) {
            userJpaRepository
                    .findByUsernameAndDeletedAtIsNull(username)
                    .ifPresent(user -> responseBuilder.putUsers(username, toSnapshot(user)));
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private UserSnapshot toSnapshot(UserJpaEntity user) {
        var builder = UserSnapshot.newBuilder()
                .setId(user.getId().toString())
                .setEmail(user.getEmail())
                .setFullName(user.getFullName())
                .setAuthProvider(user.getAuthProvider())
                .setCreatedAt(toTimestamp(user.getCreatedAt()))
                .setUpdatedAt(toTimestamp(user.getUpdatedAt()));

        if (user.getUsername() != null) {
            builder.setUsername(StringValue.of(user.getUsername()));
        }
        if (user.getAvatarUrl() != null) {
            builder.setAvatarUrl(StringValue.of(user.getAvatarUrl()));
        }
        if (user.getPreferences() != null) {
            parsePreferencesToStruct(user.getPreferences()).ifPresent(builder::setPreferences);
        }

        return builder.build();
    }

    private Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    /**
     * Parses a raw JSON string into a protobuf {@link Struct}.
     * Returns empty if the JSON is invalid or not an object.
     */
    private java.util.Optional<Struct> parsePreferencesToStruct(String json) {
        try {
            var structBuilder = Struct.newBuilder();
            com.google.protobuf.util.JsonFormat.parser()
                    .ignoringUnknownFields()
                    .merge(json, structBuilder);
            return java.util.Optional.of(structBuilder.build());
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }
}
