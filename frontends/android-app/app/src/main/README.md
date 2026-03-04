# Zero Meeting System - Android Frontend

Ứng dụng Android cho hệ thống **Zero Meeting System**.
Module này chịu trách nhiệm:

* Hiển thị giao diện người dùng
* Điều hướng giữa các màn hình
* Gọi API backend
* Quản lý trạng thái cuộc họp
* Tương tác giữa người dùng trong phòng họp

Ứng dụng được xây dựng theo mô hình **MVVM + Dependency Injection** để đảm bảo code rõ ràng, dễ mở rộng và dễ bảo trì.

---

# Kiến trúc tổng thể

Ứng dụng sử dụng mô hình **MVVM (Model - View - ViewModel)**.

Luồng dữ liệu:

```
Activity / Fragment (UI)
        ↓
ViewModel
        ↓
Repository
        ↓
Network (Retrofit API)
        ↓
Backend Server
```

Giải thích:

**View (Activity / Fragment)**
Hiển thị giao diện và nhận sự kiện từ người dùng.

**ViewModel**
Xử lý logic của UI và quản lý trạng thái màn hình.

**Repository**
Lớp trung gian giữa ViewModel và API.

**Network Layer**
Định nghĩa API và cấu hình Retrofit.

---

# Cấu trúc thư mục

```
com.example.zeromeeting

core
 ├─ di
 │    ├─ RetrofitModule
 │    └─ RepositoryModule
 │
 ├─ network
 │    └─ ApiService
 │
 └─ utils

data
 ├─ model
 │    ├─ User
 │    ├─ Meeting
 │    └─ Message
 │
 └─ repository
      ├─ AuthRepository
      └─ MeetingRepository

view
 ├─ splash
 │    └─ SplashActivity
 │    └─ SplashViewModel
 ├─ auth
 │    ├─ LoginActivity
 │    └─ RegisterActivity
 │
 ├─ dashboard
 │    └─ DashboardActivity
 │
 ├─ meetingcreate
 │    └─ CreateMeetingActivity
 │
 ├─ meetingroom
 │    └─ MeetingRoomActivity
 │
 ├─ adapters
 ├─ components
 └─ common


(Phần ViewModel chúng ta sẽ gộp vào chỗ view của từng màn hình để dễ fix bug và theo dõi)
ZeroMeetingApp
```

---

# Dependency Injection (Hilt)

Project sử dụng **Hilt** để quản lý dependency.

## Application

Hilt được khởi tạo trong `ZeroMeetingApp`.

```
@HiltAndroidApp
public class ZeroMeetingApp extends Application {
}
```

Class này giúp Hilt tạo container dependency cho toàn bộ ứng dụng.

---

## Inject vào Activity

Các Activity cần sử dụng dependency phải thêm annotation:

```
@AndroidEntryPoint
```

Ví dụ:

```
@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {
}
```

---

## Inject vào ViewModel

ViewModel sử dụng annotation:

```
@HiltViewModel
```

Ví dụ:

```
@HiltViewModel
public class AuthViewModel extends ViewModel {

    private AuthRepository repository;

    @Inject
    public AuthViewModel(AuthRepository repository) {
        this.repository = repository;
    }
}
```

Hilt sẽ tự động inject `AuthRepository`.

---

# Hilt Modules

Các module dependency nằm trong:

```
core/di
```

Ví dụ:

## RetrofitModule

Chịu trách nhiệm cung cấp Retrofit instance.

```
@Module
@InstallIn(SingletonComponent.class)
public class RetrofitModule {

    @Provides
    @Singleton
    public Retrofit provideRetrofit() {
        return new Retrofit.Builder()
                .baseUrl("BASE_URL")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    public ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }
}
```

---

## RepositoryModule

Cung cấp repository cho ViewModel.

```
@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    @Provides
    public AuthRepository provideAuthRepository(ApiService apiService) {
        return new AuthRepository(apiService);
    }

}
```

---

# Giao tiếp API

Ứng dụng sử dụng:

* Retrofit
* OkHttp
* Gson

Luồng gọi API:

```
UI
 ↓
ViewModel
 ↓
Repository
 ↓
ApiService
 ↓
Server
```

---

# Định nghĩa API

Tất cả endpoint được khai báo trong:

```
core/network/ApiService.java
```

Ví dụ:

```
public interface ApiService {

    @POST("/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

}
```

---

# Repository

Repository chịu trách nhiệm gọi API.

Ví dụ:

```
public class AuthRepository {

    private ApiService apiService;

    public AuthRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public Call<LoginResponse> login(LoginRequest request) {
        return apiService.login(request);
    }
}
```

---

# ViewModel

ViewModel gọi repository và trả dữ liệu về UI.

Ví dụ:

```
public class AuthViewModel extends ViewModel {

    private AuthRepository repository;

    @Inject
    public AuthViewModel(AuthRepository repository) {
        this.repository = repository;
    }

    public void login(LoginRequest request) {
        repository.login(request);
    }
}
```

---

# Luồng tạo một màn hình mới

Khi tạo feature mới cần thực hiện theo thứ tự sau:

1. Tạo Model
2. Khai báo API trong ApiService
3. Tạo Repository
4. Tạo ViewModel
5. Tạo Activity
6. Tạo XML layout
7. Kết nối Activity với ViewModel
8. Thêm navigation

---

# Quy tắc quan trọng

* Không viết logic trong Activity
* Activity chỉ xử lý UI
* Logic nằm trong ViewModel
* Gọi API thông qua Repository
* Dependency phải được inject bằng Hilt

---
