package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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

	private final ExecutorService executorService = Executors.newFixedThreadPool(1000);


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
	
//	public void calculateReward(User user) {
//		List<VisitedLocation> userLocations = user.getVisitedLocations().stream().collect(Collectors.toList());
//		List<Attraction> attractions = gpsUtil.getAttractions();
//
//		for(VisitedLocation visitedLocation : userLocations) {
//			for(Attraction attraction : attractions) {
//				if(user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
//					if(nearAttraction(visitedLocation, attraction)) {
//						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//					}
//				}
//			}
//		}
//	}

	public CompletableFuture<Void> calculateRewards(User user) {
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (VisitedLocation visitedLocation : userLocations) {
			for (Attraction attraction : attractions) {
				if (user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
					if (nearAttraction(visitedLocation, attraction)) {
						CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
							UserReward userReward = new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user));
							user.addUserReward(userReward);
						}, executorService);
						futures.add(future);
					}
				}
			}
		}

		CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		return allFutures;
	}




//	public void calculateRewardsss(List<User> userList) {
//
//		List<Attraction> attractions = gpsUtil.getAttractions();
//		List<Future<?>> listFuture = new ArrayList<>();
//
//		for(User user : userList) {
//			Future<?> future = executorService.submit( () -> {
//
//				for(Attraction attraction : attractions) {
//					//this condition is needed to avoid useless call to reward calculation that won't be stored...
//					if(user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
//						//rewards are calculated on the last Location only:
//						VisitedLocation lastVisitedLocation = user.getVisitedLocations().get(user.getVisitedLocations().size()-1);
//						if(nearAttraction(lastVisitedLocation, attraction)) {
//							user.addUserReward(new UserReward(lastVisitedLocation, attraction, getRewardPoints(attraction, user)));
//						}
//					}
//				}
//			});
//			listFuture.add(future);
//		}
//
//		listFuture.stream().forEach(f->{
//			try {
//				f.get();
//			} catch (InterruptedException | ExecutionException e) {
//
//				e.printStackTrace();
//			}
//		});
//
//	}


	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	private int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
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

	public void setRewardsPoint(User user, VisitedLocation visitedLocation, Attraction attraction){
		Double distance = getDistance(attraction, visitedLocation.location);
		UserReward userReward = new UserReward(visitedLocation, attraction, distance.intValue());
		CompletableFuture.supplyAsync(()->{
			return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());

		}, executorService).thenAccept(point->{
			userReward.setRewardPoints(point);
			user.addUserReward(userReward);
		});
	}

	public Executor getExecutor(){
		return this.executorService;
	}

}
