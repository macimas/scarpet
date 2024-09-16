help() -> (
	print('/claude §6-§e cureable zombie piglins');
	print('Throw a weakness potion at a zombie piglin, feed it a §bGolden Apple§f, and it slowly converts back into a piglin, additionally being immune to zombification!');
	print('You can also feed a regular piglin instead to make it immune instantly, so you don\'t have to convert it into a zombie!');
	print('§6§lNOTE: §r§6Do not let a zombie piglin unload while being converted, or else it might dissapear. Conversion only takes 30 seconds.')
);

__on_player_interacts_with_entity(p, e, h) -> (
	if (h != 'mainhand', return());
	if (p~'holds':3 != 'golden_apple', return());
	if (e~['has_scoreboard_tag', 'curing'], print('§cThis piglin is already being cured!'); return());
	if (e~['has_scoreboard_tag', 'immune'], print('§cThis piglin is already immune!'); return());
	if (e~'type' == 'piglin',
		if (p~'gamemode' == 'survival', inventory_remove(p, p~'holds':3));
		modify(e, 'nbt_merge', nbt('{IsImmuneToZombification:1,Tags:["immune"]}'));
		sound('minecraft:entity.zombie_villager.converted', e~'pos');
		return()
	);
	if (e~'type' == 'zombified_piglin', null, return());
	if (e~['effect', 'weakness'] == null, return());

	nbt = nbt('{
		Health:		'+e~'health'+',
		CustomName:	'+if (e~['nbt','CustomName'] == null, null, encode_nbt(e~['nbt','CustomName']))+',
		Rotation:	'+e~['nbt','Rotation']+',
		HandItems:	'+e~['nbt','HandItems']+',
		ArmorItems:	'+e~['nbt','ArmorItems']+',
		ActiveEffects: [{ShowParticles:1, Duration:200, Id:9}],
		IsBaby:		'+e~'is_baby'+',
		CanPickUpLoot: 1,
		IsImmuneToZombification: 1
	}');

	if (p~'gamemode' == 'survival', inventory_remove(p, p~'holds':3));
	modify(e, 'tag', 'curing');
	modify(e, 'nbt_merge', nbt('{ActiveEffects:[{ShowParticles:1,Duration:2000,Id:5}]}'));
	sound('minecraft:entity.zombie_villager.cure', e~'pos');
	
	schedule(600, _(immune, e, nbt) -> (
		spawn('piglin', e~'pos', nbt);
		sound('minecraft:entity.zombie_villager.converted', e~'pos');
		modify(e, 'remove');
	), immune, e, nbt)
);

__config() -> {
	'commands' -> {
		'' -> 'help',
		'help' -> 'help'
	}
}