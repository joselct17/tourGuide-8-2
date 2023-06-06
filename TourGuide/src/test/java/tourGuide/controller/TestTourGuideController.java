package tourGuide.controller;


import com.fasterxml.jackson.databind.ObjectMapper;

import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tourGuide.model.user.User;
import tourGuide.service.TourGuideService;

import java.util.Date;
import java.util.UUID;


import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestTourGuideController {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TourGuideService tourGuideService;

    @Autowired
    private ObjectMapper objectMapper;

    User user1;
    Location location1;
    VisitedLocation visitedLocation1;

    Location location2;

    VisitedLocation visitedLocation2;

    @BeforeEach
    public void setup() {

        user1 = new User(UUID.randomUUID(), "John", "165223", "jhon@tourguide.com");
        location1= new Location(45.5455,45.545);
        visitedLocation1 = new VisitedLocation(UUID.randomUUID(), location1, new Date());
        location2 = new Location(58.54, 45.25);
        visitedLocation2 = new VisitedLocation(UUID.randomUUID(), location2, new Date());

    }

    @Test
    public void GET_index_shouldSucceed() throws Exception {

        //ACT + ASSERT
        mockMvc
                .perform(get("/"))
                .andExpect(status().isOk());

    }

    @Test
    public void GET_getLocation_shouldSucceed() throws Exception {


        //ARRANGE
        when(tourGuideService.getUser("john")).thenReturn(user1);
        when(tourGuideService.getUserLocation(user1)).thenReturn(visitedLocation1);

        //ACT
        MvcResult result = mockMvc
                .perform(get("/getLocation?userName=john"))
                .andExpect(status().isOk())
                .andReturn();


        String resultLocationAsString = result.getResponse().getContentAsString();
        assertNotNull(resultLocationAsString);
        assertEquals(objectMapper.writeValueAsString(location1), resultLocationAsString);

    }

}
