package data.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import org.json.JSONObject;

import java.util.*;

public class PrintStationCost extends BaseCommandPlugin
{
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap)
    {
        if(dialog == null) return false;

        TextPanelAPI text = dialog.getTextPanel();

        text.addPara("Create a station requires the objectives below:");
        ArrayList<Object> required = new ArrayList<Object>();
        for(Map.Entry<String, Number> cost : PrintStationCost.getCost("required").entrySet()){
            required.add(cost.getKey());
            required.add((int)(float)cost.getValue());
            required.add(false);
        }
        setCostPanel(dialog, required.toArray());

        text.addPara("Create a station consumes the objectives below:");
        ArrayList<Object> consumed = new ArrayList<Object>();
        for(Map.Entry<String, Number> cost : PrintStationCost.getCost("consumed").entrySet()){
            consumed.add(cost.getKey());
            consumed.add((int)(float)cost.getValue());
            consumed.add(true);
        }
        setCostPanel(dialog, consumed.toArray());

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
                costs.put(key, (float)consumed.getDouble(key));
            }
        } catch (Exception e) {}
        return costs;
    }

    public static void setCostPanel(InteractionDialogAPI dialog, Object[] displays){
        // Due to display limit, the display will cut each 3 display items(9 length array)
        int i = 0;
        while(i <= displays.length){
            if( i + 9 > displays.length){
                dialog.getTextPanel().addCostPanel("", Arrays.copyOfRange(displays, i, displays.length));
            }
            else{
                dialog.getTextPanel().addCostPanel("", Arrays.copyOfRange(displays, i, i + 9));
            }
            i += 9;
        }
    }
}
