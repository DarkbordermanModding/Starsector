package data.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import org.json.JSONObject;

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
        String requireParagraph = "";
        for(Map.Entry<String, Number> cost : PrintStationCost.getCost("required").entrySet()){
            String name = Global.getSettings().getCommoditySpec(cost.getKey()).getName();
            requireParagraph += name + " x " + (int)(float)cost.getValue() + "\n";
        }
        text.addPara(requireParagraph);
        text.addPara("Create a station consumes the objectives below:");
        String consumeParagraph = "";
        for(Map.Entry<String, Number> cost : PrintStationCost.getCost("consumed").entrySet()){
            String name = Global.getSettings().getCommoditySpec(cost.getKey()).getName();
            consumeParagraph += name + " x " + (int)(float)cost.getValue() + "\n";
        }
        text.addPara(consumeParagraph);
        return true;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Number> getCost(String type){
        Map<String, Number> costs = new HashMap<>();
        try {
            JSONObject consumed = Global.getSettings().getJSONObject("addstation").getJSONObject(type);
            Iterator<String> keys = consumed.keys();
            while(keys.hasNext()){
                String key = keys.next();
                costs.put(key , (float)consumed.getDouble(key));
            }
        } catch (Exception e) {}
        return costs;
    }
}