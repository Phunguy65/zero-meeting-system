package org.openapitools.jackson.nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.cfg.MapperBuilder;

class JsonNullJacksonServiceLoadingTest {

    @Test
    void testJackson2JsonNullableModuleServiceLoading() {
        String foundModuleName = ObjectMapper.findModules().get(0).getModuleName();
        assertEquals(new JsonNullableModule().getModuleName(), foundModuleName);
    }

    @Test
    void testJackson3JsonNullableModuleServiceLoading() {
        String foundModuleName = MapperBuilder.findModules().get(0).getModuleName();
        assertEquals(new JsonNullableJackson3Module().getModuleName(), foundModuleName);
    }
}
