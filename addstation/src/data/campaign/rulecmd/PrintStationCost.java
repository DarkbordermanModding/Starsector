package data.campaign.rulecmd;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.*;
import java.util.List;

public class PrintStationCost extends BaseCommandPlugin
{
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap)
    {
        if(dialog == null) return false;

        TextPanelAPI text = dialog.getTextPanel();

        text.addPara("Create a station requires the objectives below:");
        String costParagraph = "";
        for(Map.Entry<String, Number> cost : PrintStationCost.getCost().entrySet()){
            costParagraph += cost.getKey() + " x " + (int)(float)cost.getValue() + "\n";
        }
        text.addPara(costParagraph);
        return true;
    }

    public static Map<String, Number> getCost(){
        Map<String, Number> costs = new HashMap<>();
        costs.put(Commodities.CREW, 1000f);
        costs.put(Commodities.FUEL, 5000f);
        costs.put(Commodities.SUPPLIES, 1000f);
        costs.put(Commodities.HEAVY_MACHINERY, 500f);
        costs.put(Commodities.METALS, 1500f);
        costs.put(Commodities.RARE_METALS, 500f);
        costs.put(Commodities.ORE, 500f);
        costs.put(Commodities.RARE_ORE, 500f);
        costs.put(Commodities.FOOD, 500f);
        costs.put(Commodities.VOLATILES, 500f);
        costs.put(Commodities.ORGANICS, 500f);
        return costs;
    }
}