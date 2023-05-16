package tourGuide.model.request;


import java.util.ArrayList;

public class ListOfFiveAttractionsCloseToUser {
    private ArrayList<AttractionWithDistanceToUser> listOfAttractionsCloseToUser;

    public ArrayList<AttractionWithDistanceToUser> getListOfAttractionsCloseToUser(){
        return listOfAttractionsCloseToUser;
    }
    public void setListOfAttractionsCloseToUser(ArrayList<AttractionWithDistanceToUser> listOfAttractionsCloseToUser){
        this.listOfAttractionsCloseToUser = listOfAttractionsCloseToUser;
    }
}
