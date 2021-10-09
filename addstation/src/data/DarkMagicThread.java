package data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public class DarkMagicThread implements Runnable{
    // Implement this to bypass a bug that haven't fix currently.
    // If the player doesn't view the colony management screen within a few days of market creation
    // There can be a bug related to population growth (Instantly grow to maximum)

    private SectorEntityToken token;

    public DarkMagicThread(SectorEntityToken token){
        this.token = token;
    }
    public void run(){
        try{
            System.out.println("go here");
            Thread.sleep(300L); // Tested time
            System.out.println("wait done");
        }catch (Exception a){}
        Global.getSector().getCampaignUI().showInteractionDialog(token);
    }
}
