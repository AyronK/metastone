package net.pferdimanzug.hearthstone.analyzer.game.heroes.powers;

import net.pferdimanzug.hearthstone.analyzer.game.GameContext;
import net.pferdimanzug.hearthstone.analyzer.game.actions.ActionType;
import net.pferdimanzug.hearthstone.analyzer.game.actions.HeroPowerAction;
import net.pferdimanzug.hearthstone.analyzer.game.actions.PlayCardAction;
import net.pferdimanzug.hearthstone.analyzer.game.cards.CardType;
import net.pferdimanzug.hearthstone.analyzer.game.cards.Rarity;
import net.pferdimanzug.hearthstone.analyzer.game.cards.SpellCard;
import net.pferdimanzug.hearthstone.analyzer.game.entities.heroes.HeroClass;
import net.pferdimanzug.hearthstone.analyzer.game.spells.Spell;
import net.pferdimanzug.hearthstone.analyzer.game.targeting.TargetSelection;

public abstract class HeroPower extends SpellCard {

	private boolean used;

	public HeroPower(String name, HeroClass heroClass) {
		super(name, CardType.HERO_POWER, Rarity.FREE, heroClass, 2);
	}

	@Override
	public HeroPower clone() {
		return (HeroPower) super.clone();
	}

	public boolean hasBeenUsed() {
		return used;
	}
	
	@Override
	public PlayCardAction play() {
		return new HeroPowerAction(this) {
			{
				setTargetRequirement(getTargetRequirement());
				setActionType(ActionType.HERO_POWER);
			}

			@Override
			protected void play(GameContext context, int playerId) {
				if (getTargetRequirement() != TargetSelection.NONE) {
					getSpell().setTarget(getTargetKey());
				}
				context.getLogic().castSpell(playerId, getSpell());
			}
		};
	}
	
	public void setSpell(Spell spell) {
		super.setSpell(spell);
		spell.setApplySpellpower(false);
	}
	
	public void setUsed(boolean used) {
		this.used = used;
	}
	

}
