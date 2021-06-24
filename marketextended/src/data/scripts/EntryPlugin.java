package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

import org.json.JSONObject;

public class EntryPlugin extends BaseModPlugin {

    public static JSONObject CONFIG;
    public static boolean ENABLED;
    static {
        try {
            validateConfig();
            CONFIG = Global.getSettings().getJSONObject("marketextended");
            ENABLED = CONFIG.getBoolean("enabled");
        } catch (Exception e) {
            ENABLED = false;
        }
    }

    public static void validateConfig() throws Exception{
        try {
            return;
        } catch (Exception e) {
            throw new Exception("Config validation failed");
        }
    }

    @Override
    public void onNewGame(){
        Global.getSector().addScript(new MarketExtended());
    }
}
