// claude
// cureable zombie piglins

__config() -> {
	'commands' -> {
		'' -> 'help',
		'help' -> 'help'
	}
};

help() -> (
	print('/claude §6-§e cureable zombie piglins');
	print('Throw a weakness potion at a zombie piglin, feed it a §bGolden Apple§f, and it slowly converts back into a piglin, additionally being immune to zombification!');
	print('You can also feed a regular piglin instead to make it immune instantly, so you don\'t have to convert it into a zombie!');
	print('§6§lNOTE: §r§6Do not let a zombie piglin unload while being converted, or else it might disappear. Conversion only takes 30 seconds.')
);

__on_player_interacts_with_entity(p, entity, hand) -> (
	if (hand != 'mainhand', return());

	if (p~'holds':3 != 'golden_apple', return());
	if (entity~['has_scoreboard_tag', 'curing'], print('§cThis piglin is already being cured!'); return());
	if (entity~['has_scoreboard_tag', 'immune'], print('§cThis piglin is already immune!'); return());
	if (entity~'type' == 'piglin',
		if (p~'gamemode'~'survival', inventory_remove(p, p~'holds':3));

		modify(entity, 'nbt_merge', encode_nbt({
			'IsImmuneToZombification' -> true
		}));
		sound('minecraft:entity.zombie_villager.converted', entity~'pos');
		return ()
	);
	if (!entity~'type'~'zombified_piglin', return());
	if (!entity~['effect', 'weakness'], return());

	nbt = parse_nbt(entity~'nbt');
	nbt:'IsImmuneToZombification' = true;
	nbt:'ActiveEffects' = [{
		'ShowParticles' -> true,
		'Duration' -> 200,
		'Id' -> 9;
	}];

	if (p~'gamemode'~'survival', inventory_remove(p, p~'holds':0));
	modify(entity, 'tag', 'curing');
	sound('minecraft:entity.zombie_villager.cure', entity~'pos');
	
	schedule(600, _(outer(entity), outer(nbt)) -> (
		spawn('piglin', entity~'pos', encode_nbt(nbt));
		sound('minecraft:entity.zombie_villager.converted', entity~'pos');
		modify(entity, 'remove');
	));
);