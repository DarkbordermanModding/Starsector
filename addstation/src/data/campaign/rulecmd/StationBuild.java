package data.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.util.Misc;

import org.json.JSONArray;

import java.util.*;
import java.util.List;

public class StationBuild extends BaseCommandPlugin
{
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap)
    {
        if(dialog == null) return false;

        // Can be improved by selecting many kind of stations
        SectorEntityToken token = dialog.getInteractionTarget();
        String constructionType = "station_side03";
        build(constructionType, token);
        removeBuildCosts();

        return true;
    }

    public void build(String type, SectorEntityToken token){
        LocationAPI loc = token.getContainingLocation();

        // Identity suffix
        CampaignClockAPI clock = Global.getSector().getClock();
        String suffix = clock.getCycle() + "_" + clock.getMonth() + "_" + clock.getDay() + "_" + clock.getHour();

        SectorEntityToken built = loc.addCustomEntity(
            "station_" + suffix,
            "Side station",
            type,
            token.getFaction().getId()
        );
        if (token.getOrbit() != null) built.setOrbit(token.getOrbit().makeCopy());

        // Replace original token with built object
        built.setLocation(token.getLocation().x, token.getLocation().y);
        built.getMemoryWithoutUpdate().set("$originalStableLocation", built);
        loc.removeEntity(token);

        // Create market
        MarketAPI market = Global.getFactory().createMarket(
            "AddStation_" + suffix,
            "Side station",
            3
        );
        market.setSize(3);

        market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        market.setPrimaryEntity(built);

        market.setFactionId(Global.getSector().getPlayerFleet().getFaction().getId());
        market.setPlayerOwned(true);

        JSONArray industries = new JSONArray();
        JSONArray conditions = new JSONArray();
        try {
            industries = Global.getSettings().getJSONObject("addstation").getJSONArray("industries");
            conditions = Global.getSettings().getJSONObject("addstation").getJSONArray("conditions");
            for(int i = 0; i < industries.length(); i++){
                market.addIndustry(industries.getString(i));
            }
            for(int i = 0; i < conditions.length(); i++){
                market.addCondition(conditions.getString(i));
            }
        } catch (Exception e) {}

        market.addSubmarket("storage");
        StoragePlugin storage = (StoragePlugin)market.getSubmarket("storage").getPlugin();
        storage.setPlayerPaidToUnlock(true);
        market.addSubmarket("local_resources");

        Global.getSector().getEconomy().addMarket(market, true);
        built.setMarket(market);
        built.setFaction(Global.getSector().getPlayerFleet().getFaction().getId());

        // Update survey and industries
        for (MarketConditionAPI condition: market.getConditions()){
            condition.setSurveyed(true);
        }
        for (Industry industry: market.getIndustries()){
            industry.doPreSaveCleanup();
            industry.doPostSaveRestore();
        }
    }

    public void removeBuildCosts()
    {
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        for(Map.Entry<String, Number> cost : PrintStationCost.getCost("consumed").entrySet()){
            cargo.removeCommodity((String)cost.getKey(), (float)cost.getValue());
        }
    }
}