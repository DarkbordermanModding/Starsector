package data.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MarketExtended implements EveryFrameScript {
    public static Random RAND = new Random();
    public static IntervalUtil SPAWN_INTERVAL;
    public static int SPAWN_TIMES;
    public static int SPAWN_MAX_COUNT;
    public static List<String> SPAWN_FACTIONS = new ArrayList<String>();
    public static boolean REVIVE_EXTINCT_FACTION;
    public static List<String> REVIVE_FACTIONS = new ArrayList<String>();
    // Config setup
    static {
        try {
            SPAWN_INTERVAL = new IntervalUtil(
                EntryPlugin.CONFIG.getInt("spawnInterval"),
                EntryPlugin.CONFIG.getInt("spawnInterval")
            );
            SPAWN_TIMES = EntryPlugin.CONFIG.getInt("spawnTimes");
            SPAWN_MAX_COUNT = EntryPlugin.CONFIG.getInt("spawnMaxCounts");
            for(int i=0; i < EntryPlugin.CONFIG.getJSONArray("spawnFactions").length(); i++){
                SPAWN_FACTIONS.add(
                    EntryPlugin.CONFIG.getJSONArray("spawnFactions").getString(i)
                );
            }
            REVIVE_EXTINCT_FACTION = EntryPlugin.CONFIG.getBoolean("reviveExtinctFaction");
            for(int i=0; i < EntryPlugin.CONFIG.getJSONArray("reviveFactions").length(); i++){
                REVIVE_FACTIONS.add(
                    EntryPlugin.CONFIG.getJSONArray("reviveFactions").getString(i)
                );
            }
        } catch (Exception e) {
            //TODO: handle exception
        }
    }

    @Override
    public void advance(float amount) {
        if(!EntryPlugin.ENABLED) return;
        SPAWN_INTERVAL.advance(amount);
        if(SPAWN_INTERVAL.intervalElapsed()){
            System.out.println("Start generation");
            for(int i = 0; i<SPAWN_TIMES; i++){
                if(!reachMaxCounts()) spawnMarket();
            }
            if(REVIVE_EXTINCT_FACTION) reviveExtinctFactions();
        }
    }

    public boolean reachMaxCounts(){
        int count = 0;
        for(MarketAPI market: Global.getSector().getEconomy().getMarketsCopy()){
            if(market.getId().contains("marketextended")) count++;
        }
        if (count >= SPAWN_MAX_COUNT) return true;
        return false;
    }

    public void spawnMarket(){
        List<StarSystemAPI> systems = Global.getSector().getStarSystems();
        StarSystemAPI randomSystem = systems.get(RAND.nextInt(systems.size()));
        List<PlanetAPI> planets = randomSystem.getPlanets();
        if(planets.size() == 0) return;
        PlanetAPI randomPlanet = planets.get(RAND.nextInt(planets.size()));
        String randomFaction = SPAWN_FACTIONS.get(RAND.nextInt(SPAWN_FACTIONS.size()));
        if(randomPlanet.getMarket() != null){
            if(randomPlanet.getMarket().getFactionId() == "neutral"){
                MarketAPI market = createMarket(randomPlanet, randomFaction);
                randomPlanet.setMarket(market);
                randomPlanet.setFaction(randomFaction);
                System.out.println("Setup" + " " + randomPlanet.getName() + " " + randomSystem.getName() + " " + randomFaction + " " + market.getId());
            }
        }
    }

    public void reviveExtinctFactions(){
        List<StarSystemAPI> systems = Global.getSector().getStarSystems();
        for(String faction: REVIVE_FACTIONS){
            // Generate extinct factions
            if(Misc.getFactionMarkets(faction).isEmpty()){
                StarSystemAPI randomSystem = systems.get(RAND.nextInt(systems.size()));
                List<PlanetAPI> planets = randomSystem.getPlanets();
                if(planets.size() == 0) return;
                PlanetAPI randomPlanet = planets.get(RAND.nextInt(planets.size()));
                if(randomPlanet.getMarket() != null){
                    if(randomPlanet.getMarket().getFactionId() == "neutral"){
                        MarketAPI market = createMarket(randomPlanet, faction);
                        randomPlanet.setMarket(market);
                        randomPlanet.setFaction(faction);
                        System.out.println("Setup" + " " + randomPlanet.getName() + " " + randomSystem.getName() + " " + faction + " " + market.getId());
                    }
                }
            }
        }
    }

    public MarketAPI createMarket(PlanetAPI planet, String faction) {
        // Identity suffix
        MarketAPI market = Global.getFactory().createMarket(
            "marketextended_" + getPrefix(),
            planet.getName() + " market",
            3
        );
        market.setSize(3);
        market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        market.setPrimaryEntity(planet);
        market.setFactionId(faction);
        for(MarketConditionAPI conditon: planet.getMarket().getConditions()){
            market.addCondition(conditon);
        }
        market.setPlayerOwned(false);
        market.addCondition("population_3");
        market.addIndustry("population");
        market.addIndustry("spaceport");
        market.addIndustry("militarybase");
        market.getTariff().modifyFlat(market.getId(), 0.3f);
        market.addSubmarket("open_market");
        market.addSubmarket("black_market");
        Global.getSector().getEconomy().addMarket(market, true);
        for (MarketConditionAPI condition: market.getConditions()){
            condition.setSurveyed(true);
        }
        for (Industry industry: market.getIndustries()){
            industry.doPreSaveCleanup();
            industry.doPostSaveRestore();
        }
        return market;
    }

    public String getPrefix(){
        Random rand = new Random();
        String result = Integer.toHexString(rand.nextInt());
        result = "00000000".substring(result.length()) + result;
        return result;
    }

    @Override
    public boolean runWhilePaused() {return false;}

    @Override
    public boolean isDone() {return false;}
}
