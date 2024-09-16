// afksp.sc -> half-baked /player wrapper for shadow players that just stand there

__config() -> {
	'commands' -> {
		'' -> _() -> main(player()),
		'<name>' -> 'main'
	},
	'arguments' -> {
		'name' -> {
			'type' -> 'term',
			'suggester' -> _(args) -> (
				list = [];

				for (player('*'), (
					p = str(_);
					regex = '^afksp\.';
					is_afksp = p~regex;

					if (regex, list += replace_first(p, regex, ''));	
				));

				return (list);
			)
		}
	}
};

main(name) -> (
	name = str('afksp.%s', str(name));
	
	cmd = if (player(name), (
		str('player %s kill', name),
		str('player %s spawn', name);
	));

	result = run(cmd);
);