package io.github.phunguy65.zms.usermanagement;

import io.github.phunguy65.zms.usermanagement.infrastructure.security.FirebaseTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class UserManagementApplicationTests {

    @MockitoBean
    FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void contextLoads() {}
}
