package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.model.user.User;
import tourGuide.model.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	private Object userLocationsLock = new Object();

	private final ExecutorService executorService = Executors.newFixedThreadPool(50);


	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}


//	public CompletableFuture<Void> calculateRewards(User user) {
//		List<VisitedLocation> userLocations;
//		List<Attraction> attractions;
//
//		synchronized (userLocationsLock) {
//			userLocations = new ArrayList<>(user.getVisitedLocations());
//			attractions = new ArrayList<>(gpsUtil.getAttractions());
//		}
//
//		List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
//
//		userLocations.parallelStream().forEach(visitedLocation -> {
//			attractions.parallelStream().forEach(attraction -> {
//				synchronized (user.getUserRewards()) {
//					if (user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
//						if (nearAttraction(visitedLocation, attraction)) {
//							CompletableFuture<Void> completableFuture = CompletableFuture.supplyAsync(() -> {
//								UserReward userReward = new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user));
//								userReward.setRewardPoints(getRewardPoints(attraction, user));
//								user.addUserReward(userReward);
//								return null;
//							}, executorService);
//							completableFutures.add(completableFuture);
//						}
//					}
//				}
//			});
//		});
//
//		CompletableFuture<Void> allCompletableFutures = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]));
//		return allCompletableFutures;
//	}
//
//
//	public CompletableFuture<Void> calculateAllRewards(List<User> users) {
//		int batchSize = 100;
//		List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
//
//		for (int i = 0; i < users.size(); i += batchSize) {
//			List<User> batchUsers = users.subList(i, Math.min(i + batchSize, users.size()));
//
//			for (User user : batchUsers) {
//				CompletableFuture<Void> completableFuture = calculateRewards(user);
//				completableFutures.add(completableFuture);
//			}
//
//			CompletableFuture<Void> batchFuture = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]));
//			batchFuture.join();
//			completableFutures.clear();
//		}
//
//		return CompletableFuture.completedFuture(null);
//	}
public void calculateRewards(User user) {

	this.executorService.execute(() -> {
		Iterable<VisitedLocation> userLocations = new ArrayList<>(user.getVisitedLocations());
		List<Attraction>          attractions   = this.gpsUtil.getAttractions();

		for (VisitedLocation visitedLocation : userLocations) {

			for (Attraction attraction : attractions) {

				if (user.getUserRewards()
						.stream()
						.noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {

					if (this.nearAttraction(visitedLocation, attraction)) {

						user.addUserReward(new UserReward(visitedLocation, attraction,
								this.getRewardPoints(attraction, user)
						));
					}
				}
			}
		}
	});
}


	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}

	private int getRewardPoints(Attraction attraction, User user) {
		CompletableFuture<Integer> rewardPointsFuture = CompletableFuture.supplyAsync(() ->
						rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId()),
				executorService
		);
		return rewardPointsFuture.join();
	}


	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}


	public Executor getExecutor(){
		return this.executorService;
	}

}
