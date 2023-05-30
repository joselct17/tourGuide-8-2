package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
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

	private final ExecutorService executorService = Executors.newFixedThreadPool(20);


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
	
	public void calculateReward(User user) {
		List<VisitedLocation> userLocations = user.getVisitedLocations().stream().collect(Collectors.toList());
		List<Attraction> attractions = gpsUtil.getAttractions();

		for(VisitedLocation visitedLocation : userLocations) {
			for(Attraction attraction : attractions) {
				if(user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
					if(nearAttraction(visitedLocation, attraction)) {
						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
					}
				}
			}
		}
	}

	public CompletableFuture<Void> calculateRewards(User user) {
		// Récupération des localisations visitées par l'utilisateur
		List<VisitedLocation> userLocations = user.getVisitedLocations().stream().collect(Collectors.toList());

		// Récupération de la liste des attractions depuis gpsUtil
		List<Attraction> attractions = gpsUtil.getAttractions();

		// Liste des CompletableFuture pour les tâches asynchrones
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		// Parcours des localisations visitées par l'utilisateur
		for (VisitedLocation visitedLocation : userLocations) {

			// Parcours des attractions
			for (Attraction attraction : attractions) {
				// Vérification si l'utilisateur n'a pas déjà une récompense pour cette attraction
				if (user.getUserRewards().stream().filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
					// Vérification si la localisation visitée est proche de l'attraction
					if (nearAttraction(visitedLocation, attraction)) {
						// Création d'un CompletableFuture pour ajouter la récompense à l'utilisateur
						CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
							user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
						}, executorService);

						// Ajout du CompletableFuture à la liste des futures
						futures.add(future);
						break;
					}
				}
			}
		}

		// Retourne un CompletableFuture qui sera complété lorsque tous les CompletableFuture dans la liste auront terminé
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
	}



//	public void calculateRewards(User user) {
//		List<VisitedLocation> userLocations = user.getVisitedLocations();
//		List<Attraction> attractions = gpsUtil.getAttractions();
//		List<Attraction> unvisitedAttractions = attractions.stream()
//				.filter(attraction -> user.getUserRewards().stream()
//						.noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName)))
//				.collect(Collectors.toList());
//
//		ExecutorService executor = Executors.newFixedThreadPool(1000); // Definnir le nombre optimal de threads
//
//		userLocations.parallelStream().forEach(visitedLocation -> {
//			unvisitedAttractions.parallelStream().forEach(attraction -> {
//				if (nearAttraction(visitedLocation, attraction)) {
//					executor.submit(() -> {
//						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//					});
//				}
//			});
//		});
//
//		executor.shutdown();
//		try {
//			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//		} catch (InterruptedException e) {
//			// Gérer l'interruption
//		}
//	}

//	public void calculateRewards(User user) {
//		List<VisitedLocation> userLocations = user.getVisitedLocations();
//		List<Attraction> attractions = gpsUtil.getAttractions();
//		List<Attraction> unvisitedAttractions = attractions.stream()
//				.filter(attraction -> user.getUserRewards().stream()
//						.noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName)))
//				.collect(Collectors.toList());
//
//		ExecutorService executor = Executors.newFixedThreadPool(4); // Définissez le nombre optimal de threads
//
//		List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
//
//		for (VisitedLocation visitedLocation : userLocations) {
//			for (Attraction attraction : unvisitedAttractions) {
//				CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//					if (nearAttraction(visitedLocation, attraction)) {
//						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
//					}
//				}, executor);
//				completableFutures.add(future);
//			}
//		}
//
//		CompletableFuture<Void> allTasks = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]));
//
//		try {
//			allTasks.get(); // Attendre la fin de toutes les tâches
//		} catch (InterruptedException | ExecutionException e) {
//			// Gérer les exceptions appropriées
//		}
//
//		executor.shutdown();
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
