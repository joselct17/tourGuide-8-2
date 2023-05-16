package tourGuide.model.request;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@NoArgsConstructor
@AllArgsConstructor
public class ListOfFiveAttractionsCloseToUser {
    private ArrayList<AttractionWithDistanceToUser> listOfAttractionsCloseToUser;

    public ArrayList<AttractionWithDistanceToUser> getListOfAttractionsCloseToUser(){
        return listOfAttractionsCloseToUser;
    }
    public void setListOfAttractionsCloseToUser(ArrayList<AttractionWithDistanceToUser> listOfAttractionsCloseToUser){
        this.listOfAttractionsCloseToUser = listOfAttractionsCloseToUser;
    }
}
