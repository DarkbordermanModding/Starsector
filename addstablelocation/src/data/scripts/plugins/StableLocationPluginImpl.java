package data.scripts.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Sounds;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.DumpMemory;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.loading.Description.Type;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.CustomCampaignEntity;

public class StableLocationPluginImpl implements InteractionDialogPlugin {

	public static int STABLE_FUEL_REQ = 500;
	public static int STABLE_MACHINERY_REQ = 200;

	private static enum OptionId {
		INIT,
		ADD_STABLE_CONFIRM,
		ADD_STABLE_DESCRIBE,
		ADD_STABLE_NEVER_MIND,
        SHUFFLE_STABLE_LOCATIONS,
		LEAVE,
	}

	private InteractionDialogAPI dialog;
	private TextPanelAPI textPanel;
	private OptionPanelAPI options;
	private VisualPanelAPI visual;
	private CampaignFleetAPI playerFleet;
	private PlanetAPI planet;

	public void init(InteractionDialogAPI dialog) {
		this.dialog = dialog;
		textPanel = dialog.getTextPanel();
		options = dialog.getOptionPanel();
		visual = dialog.getVisualPanel();

		playerFleet = Global.getSector().getPlayerFleet();
		planet = (PlanetAPI) dialog.getInteractionTarget();
		visual.setVisualFade(0.25f, 0.25f);

		if (planet.getCustomInteractionDialogImageVisual() != null) {
			visual.showImageVisual(planet.getCustomInteractionDialogImageVisual());
		} else {
			if (!Global.getSettings().getBoolean("3dPlanetBGInInteractionDialog")) {
				visual.showPlanetInfo(planet);
			}
		}
		dialog.setOptionOnEscape("Leave", OptionId.LEAVE);
		optionSelected(null, OptionId.INIT);
	}

	public Map<String, MemoryAPI> getMemoryMap() {return null;}
	public void backFromEngagement(EngagementResultAPI result) {}

	public void optionSelected(String text, Object optionData){
		if (optionData == null) return;

		if (optionData == DumpMemory.OPTION_ID) {
			Map<String, MemoryAPI> memoryMap = new HashMap<String, MemoryAPI>();
			MemoryAPI memory = dialog.getInteractionTarget().getMemory();

			memoryMap.put(MemKeys.LOCAL, memory);
			if (dialog.getInteractionTarget().getFaction() != null) {
				memoryMap.put(MemKeys.FACTION, dialog.getInteractionTarget().getFaction().getMemory());
			} else {
				memoryMap.put(MemKeys.FACTION, Global.getFactory().createMemory());
			}
			memoryMap.put(MemKeys.GLOBAL, Global.getSector().getMemory());
			memoryMap.put(MemKeys.PLAYER, Global.getSector().getCharacterData().getMemory());

			if (dialog.getInteractionTarget().getMarket() != null) {
				memoryMap.put(MemKeys.MARKET, dialog.getInteractionTarget().getMarket().getMemory());
			}

			new DumpMemory().execute(null, dialog, null, memoryMap);

			return;
		}

		OptionId option = (OptionId) optionData;

		if (text != null) {
			//textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
			dialog.addOptionSelectedText(option);
		}
        StarSystemAPI system = planet.getStarSystem();

		switch (option) {
		case INIT:
			addText(getString("approach"));

			Description desc = Global.getSettings().getDescription(planet.getCustomDescriptionId(), Type.CUSTOM);
			if (desc != null && desc.hasText3()) {
				addText(desc.getText3());
			}
			createInitialOptions();
			break;
		case ADD_STABLE_CONFIRM:
			if (system != null) {
				CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
				cargo.removeFuel(STABLE_FUEL_REQ);
				AddRemoveCommodity.addCommodityLossText(Commodities.FUEL, STABLE_FUEL_REQ, dialog.getTextPanel());
				StarSystemGenerator.addStableLocations(system, 1);
				addText("Preparations are made, and you give the go-ahead. " +
						"A few tense minutes later, the chief engineer reports success. " +
						"The resulting stable location won't last for millennia, like " +
						"naturally-occurring ones - but it'll do for your purposes.");
			}
			createInitialOptions();
			break;
		case ADD_STABLE_DESCRIBE:
			addText("The procedure requires spreading prodigious amounts of antimatter in the star's corona, " +
					"according to calculations far beyond the ability of anything on the right side of the " +
					"treaty that ended the Second AI War.");
			boolean canAfford = dialog.getTextPanel().addCostPanel("Resources required (available)", 
					Commodities.ALPHA_CORE, 1, false,
					Commodities.HEAVY_MACHINERY, STABLE_MACHINERY_REQ, false,
					Commodities.FUEL, STABLE_FUEL_REQ, true
					);

			options.clearOptions();

			int stableLocationCount = 2;
			try {
				stableLocationCount = Global.getSettings().getJSONObject("addstablelocation").getInt("stableLocationCount");
			} catch (Exception e) {}

			int num = Misc.getNumStableLocations(planet.getStarSystem());
			boolean alreadyCant = false;
			if (num <= 0) {
				options.addOption("Proceed with the operation", OptionId.ADD_STABLE_CONFIRM, null);
			} else if (num < stableLocationCount) {
				addText("Normally, this procedure can only be performed in a star system without any " +
						"stable locations. However, your chief engineer suggests an unorthodox workaround.");
				options.addOption("Proceed with the operation", OptionId.ADD_STABLE_CONFIRM, null);
				SetStoryOption.set(
					dialog,
					Global.getSettings().getInt("createStableLocation"),
					OptionId.ADD_STABLE_CONFIRM,
					"createStableLocation",
					Sounds.STORY_POINT_SPEND_TECHNOLOGY,
					"Created additional stable location in " + planet.getStarSystem().getNameWithLowercaseType() + ""
				);
			} else {
				alreadyCant = true;
				String reason = "This procedure can not performed in a star system that already has " +
								"numerous stable locations.";
				options.addOption("Proceed with the operation", OptionId.ADD_STABLE_CONFIRM, null);
				options.setEnabled(OptionId.ADD_STABLE_CONFIRM, false);
				addText(reason);
				options.setTooltip(OptionId.ADD_STABLE_CONFIRM, reason);
			}

			if (!canAfford && !alreadyCant) {
				String reason = "You do not have the necessary resources to carry out this procedure.";
				options.setEnabled(OptionId.ADD_STABLE_CONFIRM, false);
				addText(reason);
				options.setTooltip(OptionId.ADD_STABLE_CONFIRM, reason);
			}
			options.addOption("Never mind", OptionId.ADD_STABLE_NEVER_MIND, null);
			break;
		case ADD_STABLE_NEVER_MIND:
			createInitialOptions();
			break;
		case LEAVE:
			Global.getSector().setPaused(false);
			dialog.dismiss();
			break;
        case SHUFFLE_STABLE_LOCATIONS:
            int stable_count = 0;
			// use a list to record enetities, remove list item in place will raise exception
			List<SectorEntityToken> entities = new ArrayList<SectorEntityToken>();
            for(SectorEntityToken entity: planet.getStarSystem().getAllEntities()){
                if(entity instanceof CustomCampaignEntity && entity.getCustomEntityType().equals("stable_location")){
                    stable_count++;
					entities.add(entity);
                }
            }
			for(SectorEntityToken entity: entities){
				system.removeEntity(entity);
			}
			addText("You tilt your head and locations look slightly different.");
            StarSystemGenerator.addStableLocations(system, stable_count);
            break;
        }
	}

	protected void createInitialOptions() {
		options.clearOptions();

		StarSystemAPI system = planet.getStarSystem();
		if (system != null && planet == system.getStar()) {
			options.addOption("Consider inducing a resonance cascade in the star's hyperfield, creating a stable location", OptionId.ADD_STABLE_DESCRIBE, null);
		}
        options.addOption("Shuffle stable location", OptionId.SHUFFLE_STABLE_LOCATIONS, null);
		options.addOption("Leave", OptionId.LEAVE, null);
		options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);

	}

	public void optionMousedOver(String optionText, Object optionData) {}
	public void advance(float amount) {}
	private void addText(String text) {textPanel.addParagraph(text);}
	public Object getContext() {return null;}

	private String getString(String id) {
		String str = Global.getSettings().getString("planetInteractionDialog", id);

		String fleetOrShip = "fleet";
		if (playerFleet.getFleetData().getMembersListCopy().size() == 1) {
			fleetOrShip = "ship";
			if (playerFleet.getFleetData().getMembersListCopy().get(0).isFighterWing()) {
				fleetOrShip = "fighter wing";
			}
		}
		str = str.replaceAll("\\$fleetOrShip", fleetOrShip);
		str = str.replaceAll("\\$planetName", planet.getName());

		return str;
	}
}
