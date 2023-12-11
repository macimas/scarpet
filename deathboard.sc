__config() -> {
	'scope' -> 'global',
	'commands' -> {
		'' -> 'show_leaderboard'
	}
};



bulk_text(list) -> (
	text = '';
	for (list, text += str('%s\n', _));
	text = replace(text, '\n$', '');
	return (text);
);



global_count = read_file('count', 'json') || {};

show_leaderboard() -> (
	if (!global_count, return(print('no one has died yet')));

	lines = [
		'the deathboard leaderboard board',
		'§6- -- --- ☠ --- -- -§r',
	];

	for (sort_key(pairs(global_count), -_:1),
		lines += str('%d §6☠ §7-§r %s', _:1, _:0);
	);

	print(bulk_text(lines));
);



__on_player_dies(p) -> (
	p = str(p);
	global_count:p += 1;
	write_file('count', 'json', global_count);
);