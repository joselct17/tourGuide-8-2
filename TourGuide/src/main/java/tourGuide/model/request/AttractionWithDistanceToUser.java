package tourGuide.model.request;

import gpsUtil.location.Location;

public class AttractionWithDistanceToUser {

    private String nameOfTouristAttraction;

    private Location locationOfTouristAttraction;

    private Location locationOfUser;

    private Double distanceInMilesBetweenTheUsersLocationAndThisAttraction;

    private int rewardsPointsForVisitingThisAttraction;
}
