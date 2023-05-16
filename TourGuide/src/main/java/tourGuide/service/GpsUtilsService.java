package tourGuide.service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.springframework.stereotype.Service;
import tourGuide.model.user.User;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class GpsUtilsService {

    GpsUtil gpsUtil;

    private final ExecutorService executorService = Executors.newFixedThreadPool(1000);

    public GpsUtilsService() {
        gpsUtil = new GpsUtil();
    }

    public VisitedLocation getUserLocation(UUID userId) {
        return gpsUtil.getUserLocation(userId);
    }

    public void getUserLocation(User user, TourGuideService tourGuideService) {
        CompletableFuture.supplyAsync(() -> {
            return gpsUtil.getUserLocation(user.getUserId());
        },
                executorService).thenAccept(visitedLocation -> {
                    tourGuideService.trackUserLocation(user);
        });
    }

    public List<Attraction> getListOfAttractions() {
        return gpsUtil.getAttractions();
    }
}
