__config() -> {
	'commands' -> {
		'' -> 'help',
		'help' -> 'help',
		'<name>' -> 'rename',
		'reset' -> 'reset',
		'preview <name>' -> 'preview',
		'pr <name>' -> 'preview',
		'formatting' -> 'formatting'
	},
	'arguments' -> {
		'name' -> { 'type' -> 'text', 'suggest' -> [] }
	}
};



help() -> (
	print('/rename §7-§e a half-baked, alternate way of renaming items');
	print('§8/rename §rreset §7- Remove custom name of item');
	print('§8/rename §rpreview/pr §7- Preview name into chat');
	print('§8/rename §rformatting §7- Formatting cheatsheet');
	print('');
	print(str('Example: /rename §e&§bb§rthe %s tool', player()));
	print(str('                   §7->§b the %s tool', player()));
	print('Requires 1 xp level to rename an item.\nResetting item name does not use xp.')
);

formatting() -> (
	print(str('Formatting codes are used with §7[§e%s§7]', '&'));
	print('');
	print('Formatting codes cheatsheet');
	print('§00 §11 §22 §33 §44 §55 §66 §77 §88 §99 §aa §bb §cc §dd §ee §ff§r');
	print('r §7-§f Reset§r         l §7-§f §lBold§r               o §7-§f §oItalic§r');
	print('n §7-§f §nUnderline§r    m §7-§f §mStrikethrough§r    k §7-§f §kObfuscated')
);

rename(name) -> (
	p = player();
	item = p~'holds';
	nbt = item:2;
	if (nbt == null, nbt = nbt('{}'));
	put(nbt, 'display.Name', str('\'{"text":"%s"}\'', escapify(formatify(name))));

	if (!item, return(throw_error('You aren\'t holding an item!')));
	if (item:2 == nbt, return(throw_error('Item name is identical!')));
	if (p~'xp_level' == 0 && p~'gamemode' == 'survival', return(throw_error('You need at least 1 xp level!')));

	inventory_set(p, p~'selected_slot', item:1, item:3, nbt);
	if (p~'gamemode' == 'survival', modify(p,'xp_level',p~'xp_level'-1));

	sound('block.conduit.activate', p~'pos');
	particle('ambient_entity_effect',p~'pos', 100, 0.1, 0.1);
	particle('firework', p~'pos', 100, 0, 0.2)
);

reset() -> (
	p = player();
	item = p~'holds';
	nbt = item:2;
	if (nbt == null, nbt = nbt('{}'));

	if (!item, return(throw_error('You aren\'t holding an item!')));
	if (!has(nbt, 'display.Name'), return(throw_error('Item does not have a custom name!')));

	delete(nbt, 'display.Name');
	if (get(nbt, 'display') == {}, delete(nbt, 'display'));
	inventory_set(p, p~'selected_slot', item:1, item:3, nbt);

	sound('item.axe.scrape', p~'pos');
	particle('squid_ink',p~'pos', 24, 0.1, 0.1);
);

preview(name) -> print(formatify(name));



formatify(s) -> (
	while (s~'&\\S', 65536, s = replace(s, s~'&\\S', replace(s~'&\\S', '&', '§')));
	return (s);
);

escapify(s) -> (
	bs = '\\\\';
	s = replace(s, bs, bs+bs+bs+bs);
	s = replace(s, '"', bs+bs+'"');
	s = replace(s, '\'', bs+bs+bs+'\'');
	return (s);
);

throw_error(text) -> (
	p = player();
	display_title(p, 'actionbar', format(str('#FF7054 %s', text)));
	sound('block.shroomlight.break', p~'pos');
);