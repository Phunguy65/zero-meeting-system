package io.github.phunguy65.zms.di;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import io.github.phunguy65.zms.data.repository.AuthRepositoryImpl;
import io.github.phunguy65.zms.data.repository.CalendarRepositoryImpl;
import io.github.phunguy65.zms.data.repository.ChatRepositoryImpl;
import io.github.phunguy65.zms.data.repository.JoinRoomRepositoryImpl;
import io.github.phunguy65.zms.data.repository.LiveKitRepositoryImpl;
import io.github.phunguy65.zms.data.repository.MeRepositoryImpl;
import io.github.phunguy65.zms.data.repository.MeetingHistoryRepositoryImpl;
import io.github.phunguy65.zms.data.repository.MeetingRepositoryImpl;
import io.github.phunguy65.zms.data.repository.ParticipantRepositoryImpl;
import io.github.phunguy65.zms.data.repository.ProfileRepositoryImpl;
import io.github.phunguy65.zms.data.repository.RecordingRepositoryImpl;
import io.github.phunguy65.zms.data.repository.ScheduleRepositoryImpl;
import io.github.phunguy65.zms.data.repository.SessionRepositoryImpl;
import io.github.phunguy65.zms.data.repository.WaitingRoomRepositoryImpl;
import io.github.phunguy65.zms.domain.repository.AuthRepository;
import io.github.phunguy65.zms.domain.repository.CalendarRepository;
import io.github.phunguy65.zms.domain.repository.ChatRepository;
import io.github.phunguy65.zms.domain.repository.JoinRoomRepository;
import io.github.phunguy65.zms.domain.repository.LiveKitRepository;
import io.github.phunguy65.zms.domain.repository.MeRepository;
import io.github.phunguy65.zms.domain.repository.MeetingHistoryRepository;
import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import io.github.phunguy65.zms.domain.repository.ParticipantRepository;
import io.github.phunguy65.zms.domain.repository.ProfileRepository;
import io.github.phunguy65.zms.domain.repository.RecordingRepository;
import io.github.phunguy65.zms.domain.repository.ScheduleRepository;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.repository.WaitingRoomRepository;

/** Hilt module binding repository interfaces to their implementations. */
@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    @Binds
    abstract AuthRepository bindAuthRepository(AuthRepositoryImpl impl);

    @Binds
    abstract MeRepository bindMeRepository(MeRepositoryImpl impl);

    @Binds
    abstract SessionRepository bindSessionRepository(SessionRepositoryImpl impl);

    @Binds
    abstract MeetingRepository bindMeetingRepository(MeetingRepositoryImpl impl);

    @Binds
    abstract MeetingHistoryRepository bindMeetingHistoryRepository(
            MeetingHistoryRepositoryImpl impl);

    @Binds
    abstract ChatRepository bindChatRepository(ChatRepositoryImpl impl);

    @Binds
    abstract CalendarRepository bindCalendarRepository(CalendarRepositoryImpl impl);

    @Binds
    abstract ProfileRepository bindProfileRepository(ProfileRepositoryImpl impl);

    @Binds
    abstract ScheduleRepository bindScheduleRepository(ScheduleRepositoryImpl impl);

    @Binds
    abstract LiveKitRepository bindLiveKitRepository(LiveKitRepositoryImpl impl);

    @Binds
    abstract JoinRoomRepository bindJoinRoomRepository(JoinRoomRepositoryImpl impl);

    @Binds
    abstract WaitingRoomRepository bindWaitingRoomRepository(WaitingRoomRepositoryImpl impl);

    @Binds
    abstract ParticipantRepository bindParticipantRepository(ParticipantRepositoryImpl impl);

    @Binds
    abstract RecordingRepository bindRecordingRepository(RecordingRepositoryImpl impl);
}
