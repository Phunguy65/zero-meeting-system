package io.github.phunguy65.zms.usermanagement.infrastructure.grpc;

import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import io.github.phunguy65.zms.proto.user.v1.BatchGetUserRequest;
import io.github.phunguy65.zms.proto.user.v1.BatchGetUserResponse;
import io.github.phunguy65.zms.proto.user.v1.UserServiceGrpc;
import io.github.phunguy65.zms.proto.user.v1.UserSnapshot;
import io.github.phunguy65.zms.usermanagement.application.usecase.internal.BatchGetUserUseCase;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * gRPC server implementation for the UserService.
 *
 * <p>Delegates to {@link BatchGetUserUseCase} for user resolution.
 * Deleted users are excluded. Partial results are returned — missing emails are absent
 * from the response map.
 */
@Component
public class UserServiceGrpcImpl extends UserServiceGrpc.UserServiceImplBase {

    private final BatchGetUserUseCase batchGetUserUseCase;

    public UserServiceGrpcImpl(BatchGetUserUseCase batchGetUserUseCase) {
        this.batchGetUserUseCase = batchGetUserUseCase;
    }

    @Override
    public void batchGetUser(
            BatchGetUserRequest request, StreamObserver<BatchGetUserResponse> responseObserver) {
        var users = batchGetUserUseCase.execute(request.getEmailsList());

        var responseBuilder = BatchGetUserResponse.newBuilder();
        users.forEach((email, user) -> responseBuilder.putUsers(email, toSnapshot(user)));

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private UserSnapshot toSnapshot(User user) {
        var builder = UserSnapshot.newBuilder()
                .setId(user.getId().toString())
                .setEmail(user.getEmail().value())
                .setFullName(user.getFullName().value())
                .setAuthProvider(user.getAuthProvider())
                .setCreatedAt(toTimestamp(user.getCreatedAt()))
                .setUpdatedAt(toTimestamp(user.getUpdatedAt()));

        user.getUsername().ifPresent(u -> builder.setUsername(StringValue.of(u.value())));
        user.getAvatarUrl().ifPresent(a -> builder.setAvatarUrl(StringValue.of(a)));
        user.getPreferences()
                .flatMap(this::parsePreferencesToStruct)
                .ifPresent(builder::setPreferences);

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
    private Optional<Struct> parsePreferencesToStruct(String json) {
        try {
            var structBuilder = Struct.newBuilder();
            com.google.protobuf.util.JsonFormat.parser()
                    .ignoringUnknownFields()
                    .merge(json, structBuilder);
            return Optional.of(structBuilder.build());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
