package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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
	
//	public void calculateRewardss(User user) {
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
//
//	public CompletableFuture<Void> calculateRewards(User user) {
//		// Récupération des localisations visitées par l'utilisateur
//		List<VisitedLocation> userLocations = user.getVisitedLocations();
//
//		// Récupération des attractions disponibles
//		List<Attraction> attractions = gpsUtil.getAttractions();
//
//		// Liste des CompletableFuture pour les tâches asynchrones
//		List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
//
//		// Parcours des localisations visitées
//		for (VisitedLocation visitedLocation : userLocations) {
//			// Parcours des attractions
//			for (Attraction attraction : attractions) {
//				// Vérification si l'utilisateur n'a pas déjà une récompense pour cette attraction
//				if (user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
//					// Vérification si l'attraction est proche de la localisation visitée
//					if (nearAttraction(visitedLocation, attraction)) {
//						// Création d'un CompletableFuture pour ajouter une récompense à l'utilisateur
//						CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
//							// Création de la récompense utilisateur
//							UserReward userReward = new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user));
//							// Ajout de la récompense à l'utilisateur
//							user.addUserReward(userReward);
//						}, executorService);
//						// Ajout du CompletableFuture à la liste
//						completableFutures.add(completableFuture);
//					}
//				}
//			}
//		}
//
//		// Attente de la complétion de tous les CompletableFuture
//		CompletableFuture<Void> allCompletableFutures = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]));
//		return allCompletableFutures;
//	}


	public void calculateRewardsMultiThread(List<User> userList) {
		// Obtention de la liste des attractions à partir de GPSUtil
		List<Attraction> attractions = gpsUtil.getAttractions();
		List<Future<?>> listFuture = new ArrayList<>();

		// Soumission des tâches de calcul des récompenses pour chaque utilisateur
		for (User user : userList) {
			Future<?> future = executorService.submit(() -> {
				for (Attraction attraction : attractions) {
					// Cette condition est nécessaire pour éviter les appels inutiles au calcul de récompenses qui ne seront pas stockées
					if (user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
						// Les récompenses sont calculées uniquement sur la dernière localisation visitée
						VisitedLocation lastVisitedLocation = user.getVisitedLocations().get(user.getVisitedLocations().size() - 1);
						if (nearAttraction(lastVisitedLocation, attraction)) {
							// Ajout de la récompense à l'utilisateur
							user.addUserReward(new UserReward(lastVisitedLocation, attraction, getRewardPoints(attraction, user)));
						}
					}
				}
			});
			listFuture.add(future);
		}

		// Attente de la fin de toutes les tâches de calcul des récompenses
		listFuture.stream().forEach(f -> {
			try {
				f.get(); // Attend la fin de la tâche
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace(); // Affiche la trace de l'erreur
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
