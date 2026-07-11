package com.rdp.courts.courtservice.court;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.UUID;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(CourtController.class)
class CourtControllerTest {

    @Autowired
    MockMvcTester mockMvc;

    @MockitoBean
    CourtService courtService;

    @Test
    void shouldReturnActiveCourts() throws Exception {
        final var id = UUID.randomUUID();
        given(courtService.getActiveCourts()).willReturn(List.of(new Court(id, "Court 1", true)));

        assertThat(mockMvc.get().uri("/courts")).hasStatusOk().bodyJson().extractingPath("$")
                .convertTo(InstanceOfAssertFactories.list(Court.class)).hasSize(1).satisfies(courts -> {
                    assertThat(courts.get(0).getName()).isEqualTo("Court 1");
                    assertThat(courts.get(0).isActive()).isEqualTo(true);
                });
    }
}
