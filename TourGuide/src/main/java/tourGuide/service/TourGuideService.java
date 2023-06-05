package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tourGuide.helper.InternalTestHelper;
import tourGuide.model.request.AttractionWithDistanceToUser;
import tourGuide.model.request.ListOfFiveAttractionsCloseToUser;
import tourGuide.tracker.Tracker;
import tourGuide.model.user.User;
import tourGuide.model.user.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;

	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;

	private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	
	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}
	
	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size()>0)?
				user.getLastVisitedLocation():trackUserLocation(user);
		return visitedLocation;
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}
	
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}
	
	public void addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}
	
	public List<Provider> getTripDeals(User user) {

		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();

		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(),

				user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);

		user.setTripDeals(providers);

		return providers;
	}

//	public VisitedLocation trackUserLocation(User user) {
//		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
//		user.addToVisitedLocations(visitedLocation);
//		rewardsService.calculateRewards(user);
//		return visitedLocation;
//	}


	public VisitedLocation trackUserLocation(User user) {
		List<User> listUser = new ArrayList<>();
		listUser.add(user);

		trackUserLocationMultiThread(listUser);

		// Récupère la dernière localisation visitée par l'utilisateur
		return user.getVisitedLocations().get(user.getVisitedLocations().size() - 1);
	}

	public void trackUserLocationMultiThread(List<User> userList) {
		List<Future<?>> listFuture = new ArrayList<>();
		//AtomicInteger count = new AtomicInteger(); // Variable pour compter le nombre de fois où la boucle est exécutée

		for (User u : userList) {
			//System.out.println("Soumission de la tâche pour l'utilisateur : " + u.getUserId());

			Future<?> future = executorService.submit(() -> {
				// Obtient la localisation de l'utilisateur à partir de GPSUtil
				VisitedLocation visitedLocation = gpsUtil.getUserLocation(u.getUserId());
				// Ajoute la localisation visitée à l'utilisateur
				u.addToVisitedLocations(visitedLocation);
			});

			listFuture.add(future);
			//count.getAndIncrement(); // Incrémente le compteur à chaque exécution
			//System.out.println("trackUserLocation - Execution #" + count); // Affiche le numéro de l'exécution
		}

		listFuture.stream().forEach(f -> {
			try {
				f.get();
			} catch (InterruptedException | ExecutionException e) {
				// Gestion des exceptions
			}
		});

		// Lancer le calcul des récompenses en multithread :
		rewardsService.calculateRewardsMultiThread(userList);
	}


	public Map<String, Location> getAllCurrentLocations() {
		Map<String, Location> mapUserUuidLocation = new HashMap<>();
		internalUserMap.forEach((id, user) ->
				mapUserUuidLocation.put(user.getUserId().toString(), getUserLocation(user).location)
		);
		return mapUserUuidLocation;
	}

	public ListOfFiveAttractionsCloseToUser getNearByAttractions(VisitedLocation visitedLocation) {

		// Créer une liste vide pour stocker les attractions avec leur distance par rapport à l'utilisateur
		ArrayList<AttractionWithDistanceToUser> listOfAttractionsWithDistance = new ArrayList<>();

		// Récupérer la liste de toutes les attractions depuis gpsUtil
		List<Attraction> allAttractions = gpsUtil.getAttractions();

		// Parcourir toutes les attractions
		for (Attraction attraction : allAttractions) {
			// Créer un objet Location pour l'emplacement de l'attraction
			Location attractionLocation = new Location(attraction.latitude, attraction.longitude);

			// Créer un objet Location pour l'emplacement du VisitedLocation fourni
			Location locationOfVisitedLocation = new Location(visitedLocation.location.latitude, visitedLocation.location.longitude);

			// Calculer la distance entre le VisitedLocation et l'attraction en utilisant rewardsService
			double distance = rewardsService.getDistance(locationOfVisitedLocation, attractionLocation);

			// Créer un nouvel objet AttractionWithDistanceToUser
			AttractionWithDistanceToUser attractionWithDistanceToUser = new AttractionWithDistanceToUser();

			// Définir le nom de l'attraction dans AttractionWithDistanceToUser
			attractionWithDistanceToUser.setNameOfTouristAttraction(attraction.attractionName);

			// Définir l'emplacement de l'attraction dans AttractionWithDistanceToUser
			attractionWithDistanceToUser.setLocationOfTouristAttraction(attractionLocation);

			// Définir la distance entre l'emplacement de l'utilisateur et l'attraction dans AttractionWithDistanceToUser
			attractionWithDistanceToUser.setDistanceInMilesBetweenTheUsersLocationAndThisAttraction(distance);

			// Ajouter l'objet AttractionWithDistanceToUser à la liste listOfAttractionsWithDistance
			listOfAttractionsWithDistance.add(attractionWithDistanceToUser);
		}

		// Définir le comparateur pour trier les attractions par distance
		Comparator<AttractionWithDistanceToUser> byDistance = Comparator.comparing(AttractionWithDistanceToUser::getDistanceInMilesBetweenTheUsersLocationAndThisAttraction);

		// Trier la liste des attractions avec leur distance en utilisant le comparateur
		Collections.sort(listOfAttractionsWithDistance, byDistance);

		// Créer un nouvel objet ListOfFiveAttractionsCloseToUser
		ListOfFiveAttractionsCloseToUser listOfFiveAttractionsCloseToUser = new ListOfFiveAttractionsCloseToUser();

		// Créer une liste vide pour stocker les cinq attractions les plus proches
		ArrayList<AttractionWithDistanceToUser> listOfObjects = new ArrayList<>();

		// Parcourir les attractions triées et ajouter les cinq premières à la liste des cinq attractions les plus proches
		for (int i = 0; i < 5 && i < allAttractions.size(); i++) {
			listOfObjects.add(listOfAttractionsWithDistance.get(i));
		}

		// Définir la liste des cinq attractions les plus proches dans l'objet listOfFiveAttractionsCloseToUser
		listOfFiveAttractionsCloseToUser.setListOfAttractionsCloseToUser(listOfObjects);

		// Retourner l'objet listOfFiveAttractionsCloseToUser
		return listOfFiveAttractionsCloseToUser;
	}

	
	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() { 
		      public void run() {
		        tracker.stopTracking();
		      } 
		    }); 
	}
	
	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();
	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);
			
			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}
	
	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}
	
	private double generateRandomLongitude() {
		double leftLimit = -180;
	    double rightLimit = 180;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
	    double rightLimit = 85.05112878;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
	    return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}


	
}
