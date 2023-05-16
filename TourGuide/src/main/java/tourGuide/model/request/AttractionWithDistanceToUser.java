package tourGuide.model.request;

import gpsUtil.location.Location;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class AttractionWithDistanceToUser {

    private String nameOfTouristAttraction;

    private Location locationOfTouristAttraction;

    private Location locationOfUser;

    private Double distanceInMilesBetweenTheUsersLocationAndThisAttraction;

    private int rewardsPointsForVisitingThisAttraction;

    public String getNameOfTouristAttraction() {
        return nameOfTouristAttraction;
    }

    public void setNameOfTouristAttraction(String nameOfTouristAttraction) {
        this.nameOfTouristAttraction = nameOfTouristAttraction;
    }

    public Location getLocationOfTouristAttraction() {
        return locationOfTouristAttraction;
    }

    public void setLocationOfTouristAttraction(Location locationOfTouristAttraction) {
        this.locationOfTouristAttraction = locationOfTouristAttraction;
    }

    public Location getLocationOfUser() {
        return locationOfUser;
    }

    public void setLocationOfUser(Location locationOfUser) {
        this.locationOfUser = locationOfUser;
    }

    public Double getDistanceInMilesBetweenTheUsersLocationAndThisAttraction() {
        return distanceInMilesBetweenTheUsersLocationAndThisAttraction;
    }

    public void setDistanceInMilesBetweenTheUsersLocationAndThisAttraction(Double distanceInMilesBetweenTheUsersLocationAndThisAttraction) {
        this.distanceInMilesBetweenTheUsersLocationAndThisAttraction = distanceInMilesBetweenTheUsersLocationAndThisAttraction;
    }

    public int getRewardsPointsForVisitingThisAttraction() {
        return rewardsPointsForVisitingThisAttraction;
    }

    public void setRewardsPointsForVisitingThisAttraction(int rewardsPointsForVisitingThisAttraction) {
        this.rewardsPointsForVisitingThisAttraction = rewardsPointsForVisitingThisAttraction;
    }
}
