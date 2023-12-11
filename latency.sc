__config() -> {
	'scope' -> 'global',
	'commands' -> {
		'' -> ['ping', null],
		'LIST_ALL' ->  ['ping', 'all'],
		'<player>' -> 'ping'
	},
	'arguments' -> {
		'player' -> {
			'type' -> 'term',
			'suggester' -> _(args) -> (
				players = player('all');
			)
		}
	}
};


ping(p) -> { // messily
	source = system_info('source_entity');
	if (!p, p = player());
	if (p == 'all' || !source, p = player('all'));
	if (type(p) != 'list', p = [p]);
	for (p, _ = player(_);
		if (_,
			print(no_section_signs(_~'display_name' + str('\ §eping:§r %s§7ms', _~'ping'), source)),
			print(str('that player doesnt exist'));
		);
	);
};

no_section_signs(text, source) -> (
	if (source,
		return (text),
		return (replace(text, '§.', ''));
	);
)