__config() -> {
	'scope' -> 'global',
	'commands' -> {
		'' -> _() -> print(deathboard(false));
	}
};

get_deaths() -> (
	deaths = read_file('deaths', 'json') || {};
	return (deaths);
);

// probably a bit overkill to check for every player
// probably...
update_deaths() -> (
	schedule(0, _() -> (
		task(_() -> (
			deaths = get_deaths();

			for (player('*'), (
				p = str(_);
				deaths:p = statistic(p, 'custom', 'deaths');
			));

			write_file('deaths', 'json', deaths);
		));
	));
);

deathboard(to_discord) -> (
	deaths = get_deaths();

	list = [if (to_discord, (
		'**\\💀  the deathboard leaderboard board  \\💀**',
		'§6=== ☠ §lthe deathboard leaderboard board§r §6☠ ===';
	))];

	for (sort_key(pairs(deaths), -_:1), (
		p = _:0;
		deaths = _:1;

		if (deaths == 0, continue());

		list += if (to_discord, (
			str('**%s** \\💀 %s', deaths, p),
			str('§6%s ☠ %s', deaths, p);
		));
	));

	if (length(list) == 1, list += 'no tracked deaths yet');

	return (join('\n', list));
);

__on_start(p) -> update_deaths();
__on_player_connects(p) -> update_deaths();
__on_player_dies(p) -> update_deaths();

// dcmc support

dcmc_cmds = system_variable_get('dcmc_cmds', {});

dcmc_cmds:'deathboard' = {};
dcmc_cmds:'deathboard':'category' = 'deathboard';
dcmc_cmds:'deathboard':'help' = [
	'show the deathboard leaderboard board'
];

dcmc_cmds:'deathboard':'callback' = 'dcmc_deathboard';

dcmc_cmds:'board' = {};
dcmc_cmds:'board':'category' = 'deathboard';
dcmc_cmds:'board':'help' = [
	'alias for the deathboard leaderboard board but assed'
];

dcmc_cmds:'board':'callback' = 'dcmc_deathboard';

handle_event('dcmc_deathboard', _(_) -> (
	signal_event('return_dcmc_deathboard', null, deathboard(true));
));

system_variable_set('dcmc_cmds', dcmc_cmds);