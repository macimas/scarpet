__config() -> {
	'commands' -> {
		'' -> 'unsign',
		'help' -> 'help' 
	}
};

help() -> {
	print('/unsign §7-§e turns the written book you\'re holding back into a book n quill');
	print('this will work as long as you don\'t add formatting. not like you\'d need to have fancy §9c§ao§bl§co§dr§es§r in your spellbook anyway')
};

unsign() -> {
	p = player();
	item = p~'holds';
	nbt = parse_nbt(item:2);
	pages = [];

	if (!item:0~'written_book', return(print('you aren\'t holding a written book')));
	for (nbt:'pages', // TODO: make this better
		object = decode_json(_);
		values = values(object);
		pages += values:0;
	);
	inventory_set(p, p~'selected_slot', item:1, 'writable_book', encode_nbt({ 'pages' -> pages }));

	sound('item.trident.return', p~'pos');
	sound('item.ink_sac.use', p~'pos');
	particle('scrape', p~'pos', 100, 0.1, 6);
};