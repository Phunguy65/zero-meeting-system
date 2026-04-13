package com.example.zeromeeting.core.di;

import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    // Sau này khi bạn tạo AuthRepository hay MeetingRepository,
    // chúng ta sẽ viết code @Provides ở đây để Hilt tự động quản lý.

    /* Ví dụ:
    @Provides
    @Singleton
    public AuthRepository provideAuthRepository(ApiService apiService) {
        return new AuthRepository(apiService);
    }
    */
}
