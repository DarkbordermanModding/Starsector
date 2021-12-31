package data.scripts.plugins;

import java.util.List;
import java.util.HashMap;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.combat.ai.FighterAI;
import com.fs.starfarer.combat.entities.Ship;
import com.fs.starfarer.loading.specs.FighterWingSpec;
import com.fs.starfarer.combat.ai.BasicShipAI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import org.lwjgl.util.vector.Vector2f;

public class MissileToFighterPlugin extends BaseEveryFrameCombatPlugin {

  private CombatEngineAPI engine;

  private static HashMap<String, String> summonMap = new HashMap<String, String>();
  static {
    summonMap.put("FW_claw", "FW_claw_wing");
    summonMap.put("FW_warthog", "FW_warthog_wing");
    summonMap.put("FW_xyphos", "FW_xyphos_wing");
    summonMap.put("FW_broadsword", "FW_broadsword_wing");
    summonMap.put("FW_wasp", "FW_wasp_wing");
    summonMap.put("FW_talon", "FW_talon_wing");
    summonMap.put("FW_talon_TT", "FW_talon_TT_wing");
  };

  @Override
  public void advance(float amount, List<InputEventAPI> events) {

    if (engine == null)
      return;
    if (engine.isPaused())
      return;

    List<DamagingProjectileAPI> projectiles = engine.getProjectiles();

    for (DamagingProjectileAPI projectile : projectiles) {
      if (projectile.getProjectileSpecId() != null) {
        if (summonMap.get(projectile.getProjectileSpecId()) != null) {
          ShipAPI ship = projectile.getSource();
          float angle = projectile.getFacing();
          int owner = projectile.getOwner();

          CombatFleetManagerAPI FleetManager = engine.getFleetManager(ship.getOwner());
          FleetManager.setSuppressDeploymentMessages(true);

          ShipAPI missile = engine.getFleetManager(owner).spawnShipOrWing(
            summonMap.get(projectile.getProjectileSpecId()).toString(),
            projectile.getLocation(),
            angle
          );
          missile.setOwner(owner);
          missile.setAlly(ship.isAlly());
          missile.setCRAtDeployment(0.5f);
          missile.setCollisionClass(CollisionClass.FIGHTER);
          missile.getVelocity().set(ship.getVelocity());
          missile.setAngularVelocity(ship.getAngularVelocity());
          FleetManager.setSuppressDeploymentMessages(false);
          engine.removeEntity(projectile);
        }
      }
    }
  }

  @Override
  public void init(CombatEngineAPI engine) {
    this.engine = engine;
  }
}