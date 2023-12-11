__config() -> {
	'bot' -> 'dcbridge',
	'scope' -> 'global',
	'commands' -> {
		'info' -> 'show_info'
	}
};

load_config() -> (
	config = read_file('config', 'json');
	if (!config, return(print('§cuh oh, no config?!')));

	config:'channel' = dc_channel_from_id(config:'channel');
	global_config = config;

	print('§6loaded bridge config');
);

show_info() -> print(bulk_text([
	str('channel: %s', global_config:'channel'),
	str('admins: %s', str(global_config:'admins')),
	str('comamnds: %s', global_commands)
]));

load_config();



// text manipulation //

bulk_text(list) -> (
	text = '';
	for (list, text += str('%s\n', _));
	return (text);
);

// TODO: improve markdown formatting
formatify(text) -> (
	rules = {
		'(\\*(.*?)\\*)' -> '§o:§r',
		'(\\*\\*(.*?)\\*\\*)' -> '§l:§r',
		'(__(.*?)__)' -> '§n:§r',
		'(~~(.*?)~~)' -> '§m:§r'
	};

	for (keys(rules),
		regex = _;
		while (shit = text~regex, 20,
			print('macimas', [fuck, shit]);
			fuck = rules:regex;
			fuck = replace(fuck, ':', shit:1);
			text = replace(text, shit:0, fuck);
		);
	);

	return (text);
);

escapify(text) -> (
	specials = [
		'\\', '[', ']', '/', '{', '}', '(', ')',
		'*', '+', '?', '.', '-', '^', '$', '|',
	];
	bk = '\\';
	for (specials, text = replace(text, bk+_, bk+bk+_));
	return (str('%s', text));
);

emoticon() -> (
	emotes = [
		':)', ':(', '>:)', '>:(',
		':D', 'D:', '>:D', 'D:<',
		'owo', 'uwu', '^w^', '.w.',
	];

	return (emotes:rand(length(emotes)));
);



// functions!!! //

send_message(content) -> task(_(outer(content)) -> (
	dc_send_message(global_config:'channel', content);
));

run_mc_command(command) -> (
	executed = run(command);
	if (executed:1 != [],
		executed = bulk_text(executed:1),
		// else
		executed = executed:2;
	);
	send_message(bulk_text(['```yaml', executed, '```']));
);

run_dc_command(msg) -> ( // awful
	input = replace(msg~'content', '^'+global_config:'prefix', '');
	name = input~'^([\\w]+)';
	command = global_commands:name;
	text = replace(input, '^([\\w]+)\\s', '');
	user = msg~'user';

	if (!command, return());
	if (command:'admin_only' && !user_is_admin(user), return(send_message('you\'re not an admin')));

	call(command:'run', text);
	logger(print(player('all'), format(
		str('g [%s] ran §6b!§7%s', user~'name', name),
		str('^ %s', msg~'content')
	)));
);

user_is_admin(user) -> (
	for (global_config:'admins', if (user~'id' == _, return(true)));
	return (false);
);

deping(text) -> (
	return (replace(text, '@(everyone|here)', '(no)'));
);



// commands //

global_commands = {};

register_command(name, info, callback) -> (
	global_commands:name = {
		'run' -> callback,
		'help' -> info:'help',
		'admin_only' -> info:'admin_only'
	};
);



register_command('help', {
	'help' -> ['sends this message']
}, _(foo) -> send_message(bulk_text([
	'**help** - sends this message',
	'**players** - show player list',
	'**run** - execute minecraft commands (admins only)',
	'**script** - execute scarpet things (admins only)'
])));

register_command('players', {
	'help' -> ['show player list']
}, _(foo) -> (
	string = 'Players: ';
	for (player('all'), _ = player(_);
		string += str('\n\\- **%s**', _~'display_name');
		if (_~'display_name' != _, string += str(' (%s)', _));
		if (_~'team'~'afkDis.afk', string += ' [AFK]');
	);
	return (send_message(string));
));

register_command('run', {
	'help' -> ['execute minecraft commands (admin only)'],
	'admin_only' -> true
}, _(command) -> (
	run_mc_command(command);
));

register_command('script', {
	'help' -> ['execute scarpet things (admin only)'],
	'admin_only' -> true
}, _(command) -> (
	run_mc_command(str('script %s', command));
));



// event stuff //

__on_start() -> dc_send_message('bridge has been established!');
__on_close() -> dc_send_message('bridge is now closed.');

__on_player_message(p, content) -> send_message(str('**<%s>** %s', p~'display_name', content));
__on_system_message(text, type) -> if (!type~'chat.type.(text|admin)', send_message(deping(text)));

__on_discord_message(msg) -> (
	if (msg~'channel' != global_config:'channel' || msg~'user' == dc_get_bot_user(), return());
	user = msg~'user';

	if (msg~'content'~str('^%s', global_config:'prefix'), return (run_dc_command(msg)));
	
	text = format(
		str('#5891fc [%s]', user~'name'),
		str('^#5891fc ping %s in Discord', user~'name'),
		str('?<@%s>\ ', user~'id'),
		str('#D7F0F2 \ %s', msg~'readable_content')
	);

	if (length(msg~'attachments'),
		text += '\nattachments: ';
		for (msg~'attachments',
			text += format(str('w#91C8F7 [%s]', _~'file_name'), str('^w %s', _~'url'), '@'+_~'url')+' ';
		);
	);

	print(player('all'), text);
	logger(text);
);

__on_player_command(p, command) -> (
	if (!p && command~'^say',
		text = replace(command, '^say ', '');
		send_message(str('[Server] %s', text));
	);

	if (command~'^flex',
		item = p~'holds';
		nbt = parse_nbt(item:2);
		text = '';

		if (!item, return());
		if (!nbt, nbt = nbt('{}'));
		if (nbt:'BlockEntityTag', nbt = nbt:'BlockEntityTag');

		fields = [
			{ 'name' -> 'item type', 'value' -> str('**%s**\n%s', item_display_name(item:0), item:0), 'inline' -> true },
			{ 'name' -> 'count', 'value' -> str('**%d**', item:1), 'inline' -> true },
			{ 'name' -> emoticon(), 'value' -> '' }
		];

		enchantment(nbt_tag, label, outer(nbt), outer(fields)) -> (
			enchantments = '';
			
			for (nbt:nbt_tag,
				_:'id' = replace(_:'id', '(.+):', '');
				enchantments += str('**%s** %d\n', _:'id', _:'lvl');
			);

			enchantments = replace(enchantments, '_', ' ');

			fields += { 'name' -> label, 'value' -> enchantments, 'inline' -> true };
		);

		tags = {
			'Items' -> _(outer(nbt), outer(fields)) -> (
				items = {};
				display_items = '';

				for (nbt:'Items', items:(_:'id') += _:'Count');
				for (sort_key(pairs(items), -_:1),
					text = str('**%s**', item_display_name(_:0));
					if (_:1 > 1, text += str(' (%d)', _:1));
					text += '\n';
					display_items += text;
				);

				fields += { 'name' -> 'inventory', 'value' -> display_items, 'inline' -> true };
			),
			'Inventory' -> _(outer(nbt), outer(fields)) -> (
				items = {};
				display_items = '';

				for (nbt:'Inventory', items:(_:'Stack':'id') += _:'Stack':'Count');
				for (sort_key(pairs(items), -_:1),
					text = str('**%s**', item_display_name(_:0));
					if (_:1 > 1, text += str(' (%d)', _:1));
					text += '\n';
					display_items += text;
				);

				fields += { 'name' -> 'inventory', 'value' -> display_items, 'inline' -> true };
			),
			'Enchantments' -> _() -> enchantment('Enchantments', 'enchantments'),
			'StoredEnchantments' -> _() -> enchantment('StoredEnchantments', 'stored enchantments')
		};

		for (keys(tags), if (nbt:_, call(tags:_)));

		embed = {
			'title' -> item_display_name(item),
			'fields' -> fields,
			'color' -> [255, 255, 255]
		};

		send_message({
			'content' -> str('%s is flexing their [%s]', p~'display_name', item_display_name(item)),
			'embeds' -> [embed]
		});
	);
);