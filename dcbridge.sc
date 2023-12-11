//         ██
//    ▄██████  ▄█████▄    ▄█▀     ▀█▄     ▄███▄▄███▄   ▄█████▄   dcbridge v2
//   ██▀   ██ ██▀   ▀▀  ▄██▄▄▄▄▄▄▄▄▄██▄  ██▀ ▀██▀ ▀██ ██▀   ▀▀   half-baked discord <-> minecraft bridge
//   ██▄  ▄██ ██▄   ▄▄  ▀██▀▀▀▀▀▀▀▀▀██▀  ██   ██   ██ ██▄   ▄▄
//    ▀███▀██  ▀█████▀    ▀█▄     ▄█▀    ▀██  ██  ██▀  ▀█████▀   quite terrible!
//             

__config() -> {
	'scope' -> 'global',
	'bot' -> 'dcbridge',
	'event_priority' -> 42,
	'commands' -> {
		'' -> 'help',
		'info' -> 'info',
	}
};

import('mdqt',
	'switch', 'fjoin', 'parse_md', 'escape_md', 'escape_regex');

init() -> (
	config = read_file('config', 'json');
	config:'channel' = dc_channel_from_id(config:'channel');
	config:'server' = config:'channel'~'server';
	config:'prefix' = str('^%s', config:'prefix' || '!');
	config:'admins' = config:'admins' || [];

	if (!config:'channel', exit('no channel?'));

	global_config = config;
	global_alive_quotes = read_file('alive_quotes', 'json');
	global_bridge_quotes = read_file('bridge_quotes', 'json');
);

init();



help() -> (
	print(fjoin('\n', [
		'/dcbridge §7-§e half-baked discord <-> minecraft bridge§r',
		'this slash command exists for debug stuff'
	]));
);

info() -> (
	list = [];

	list += '\n§lconfig§r';
	for (pairs(global_config), 
		list += str('§6%s:§r %s', _);
	);

	list += '\n§lcommands§r';
	for (pairs(system_variable_get('dcbridge_commands')), 
		list += str('§6%s:§r %s', _);
	);

	print(fjoin('\n', list));
);

reload() -> (
	system_variable_set('dcbridge_commands', {});
	run('reload');
);



create_message(content, callback) -> (
	if (type(content) == 'map' && content:'allowed_mentions' == null,
		content:'allowed_mentions' = { 'mention_users' -> true };
	);

	msg = dc_send_message(global_config:'channel', content);

	if (callback, call(callback, msg));
);
send_message(text) -> task(_(outer(text)) -> (
	create_message(text, null);
));
send_message_callback(text, callback) -> task(_(outer(text)) -> (
	create_message(text, callback);
));
send_message_sync(text) -> (
	create_message(text, null);
);
send_reply(msg, text) -> (
	create_message({ 'reply_to' -> msg, 'content' -> text }, null);
);
send_reply_callback(msg, text, callback) -> (
	if (type(text) == 'map', 
		text:'reply_to' = msg,
		text = { 'reply_to' -> msg, 'content' -> text };
	);
	create_message(text, callback);
);

display_message(msg, is_msg_ref) -> (
	user = msg~'user';

	msg_text = msg~'readable_content';
	msg_text = format(
		str('#5891fc [%s]', user~'name'),
		str('^w %s§r\n§6Click to copy', user~'mention_tag'),
		str('&%s', user~'mention_tag'),
		//str('^w %s§r\n§6Click to reply', msg~'id'),
		//str('?/dcbridge reply %s\ ', msg~'id'),
		str('#d7f0f2 \ %s', parse_md(msg_text || '§7§oThis message is empty.')),
	);

	msg_ref = msg~'referenced_message';
	msg_ref = if (msg_ref, display_message(msg_ref, true));

	attachments = display_attachments(msg~'attachments');
	stickers = display_sticker(msg~'sticker_ids':0);

	texts = [];

	if (msg_ref && !is_msg_ref, texts += '\ ╔☞\ ' + msg_ref);
	texts += msg_text;
	if (attachments, texts += if (is_msg_ref, '', 'attachments: ') + attachments);
	if (stickers, texts += if (is_msg_ref, '', 'stickers: ') + stickers);

	return (fjoin(if (is_msg_ref, ' ', '\n'), texts));
);

display_attachments(attachments) -> (
	if (!attachments, return());

	line = '';
	for (attachments,
		text = format(
			str('#91C8F7 [%s]', _~'file_name'),
			str('^w %s\n§7%s\n§6Click to open', _~'file_name', replace(_~'url', '\\\?.+', '')),
			str('@%s', _~'url')
		);

		line += text + ' ';
	);

	return (line);
);

display_sticker(sticker) -> (
	if (!sticker, return());

	sticker = dc_sticker_from_id(sticker);
	line = '';
	text = format(
		str('#91C8F7 [%s]', sticker~'name'),
		str('@https://media.discordapp.net/stickers/%s.png', sticker~'id'),
		str('^w %s\n§6Click to open', sticker~'description' || sticker~'name')
	);

	line += text + ' ';

	return (line);
);

display_items(nbt) -> (
	items = {};
	display_items = '';

	for (nbt:'Items', items:(_:'id') += _:'Count');
	for (sort_key(pairs(items), -_:1),
		text = str('**%s**', item_display_name(_:0));
		if (_:1 > 1, text += str(' (%d)', _:1));
		text += '\n';
		display_items += text;
	);

	return (display_items);
);

display_enchantments(nbt_tag, label, nbt) -> (
	enchantments = '';
	
	for (nbt:nbt_tag,
		_:'id' = replace(_:'id', '(.+):', '');
		enchantments += str('**%s** %d\n', _:'id', _:'lvl');
	);

	enchantments = replace(enchantments, '_', ' ');

	return (enchantments);
);

display_flex = (_(p) -> (
	item = p~'holds';
	if (!item, return());

	fields = [
		{ 'name' -> 'item type', 'value' -> str('**%s**\n%s', item_display_name(item:0), item:0), 'inline' -> true },
		{ 'name' -> 'count', 'value' -> str('**%d**', item:1), 'inline' -> true },
		{ 'name' -> emoticon(), 'value' -> '' }
	];

	nbt = item:2 || nbt('{}');
	nbt = nbt:'BlockEntityTag' || nbt;
	nbt = parse_nbt(nbt);
	nbt_tags = {
		'Items' -> {
			'name' -> 'items',
			'value' -> display_items(nbt)
		},
		'Enchantments' -> {
			'name' -> 'enchantments',
			'value' -> display_enchantments('Enchantments', 'enchantments', nbt)
		},
		'StoredEnchantments' -> {
			'name' -> 'stored enchantments',
			'value' -> display_enchantments('StoredEnchantments', 'stored nchantments', nbt)
		}
	};

	for (keys(nbt_tags), if (nbt:_, fields += {
		'name' -> nbt_tags:_:'name',
		'value' -> nbt_tags:_:'value',
		'inline' -> true
	}));

	embed = {
		'title' -> item_display_name(item),
		'fields' -> fields,
		'color' -> [255, 255, 255]
	};

	send_message({
		'content' -> str('%s is flexing their [%s]', p~'display_name', item_display_name(item)),
		'embeds' -> [embed] }
	);
));

random_quote(quotes, data) -> (
	quotes = quotes || ['no quotes?'];
	quote = quotes:rand(length(quotes));
	if (type(quote) == 'string', return (quote));

	text = quote:'text';
	substitutes = pairs(quote:'substitutes');

	// very bad!
	for (substitutes, shit = _:0; fuck = _:1;
		fuck = switch (fuck:'type', {
			'range' -> _(outer(fuck)) -> (
				range = rand(fuck:'max' - fuck:'min') + fuck:'min';
				range = ceil(range);
				return (range);
			),
			'phrase' -> _(outer(fuck)) -> (
				phrase = fuck:'phrases':(rand(length(fuck:'phrases')));
				return (phrase);
			),
			'evoker_user' -> _(outer(data)) -> (
				return (data:'msg'~'user'~'name');
			),
			'random_user' -> _(outer(fuck)) -> (
				display = fuck:'display';
				users = global_config:'server'~'users';
				user = users:rand(length(users))~display;
				return (user);
			),
			'callback' -> _(outer(fuck)) -> (
				return (call(fuck:'function'));
			)
		});

		text = replace(text, escape_regex(shit), fuck);
	);

	return (text);
);

emoticon() -> (
	emotes = [
		':)', ':(', '>:)', '>:(',
		':D', 'D:', '>:D', 'D:<',
		'owo', 'uwu', '^w^', '.w.',
	];

	return (emotes:rand(length(emotes)));
);

id_is_admin(id) -> (
	return (global_config:'admins'~id != null);
);

mc_run_command(command) -> (
	texts = run(command);
	texts = if (texts:1 != [], fjoin('\n', texts:1), texts:2);
	texts = fjoin('\n', ['```yaml', texts || 'yaml', '```']);
	texts = replace(texts, '§.', '');

	if (length(texts) > 2000,
		return (send_message('sorry, text output is above 2k characters'));
	);

	send_message(texts);
);

dcbridge_run_command(msg) -> (
	user = msg~'user';
	text = msg~'readable_content';
	name = lower(text~'^\\S+');
	name = replace(name, global_config:'prefix', '');
	commands = system_variable_get('dcbridge_commands');
	command = commands:name;
	args = replace(text, '^\\S+\\s?', '');

	if (!command,
		emoji = global_config:'reject_emoji';
		return (dc_react(msg, emoji || '❌'));
	);

	if (command:'admin' && !id_is_admin(user~'id'),
		return (send_reply(msg, 'you\'re not an admin'));
	);

	call(command:'run', args, msg);

	logger(print(player('*'), format(
		str('g [%s] ran §6%s', user~'name', name),
		str('^ %s', msg~'content')
	)));
);



commands = system_variable_get('dcbridge_commands', {});

command_help = (_(args, msg) -> (
	commands = system_variable_get('dcbridge_commands', {});

	if (args, (
		command = commands:args;
		
		if (!command,
			return (send_reply(msg, 'that command doesn\'t exist'));
		);

		list = [str('**%s**', args)];
		for (command:'help',
			list += _;
		);
	), (
		list = ['**commands**'];
		for (pairs(commands),
			list += str('**%s** - %s', _:0, _:1:'help':0);
		);
	));

	send_reply(msg, fjoin('\n', list));
));
commands:'help' = {
	'run' ->  command_help,
	'help' -> ['sends this message']
};

command_players = (_(args, msg) -> (
	text = 'Players: ';
	for (player('*'), _ = player(_);
		text += str('\n\\- **%s**', escape_md(_~'display_name'));
		if (_~'display_name' != _, text += str(' (%s)', _));
		if (_~'team'~'afkDis.afk', text += ' [AFK]');
	);
	if (!rand(69), text += '\n\\- **Herobrine**'); // ignore this line.
	return (send_reply(msg, text));
));
commands:'players' = {
	'run' -> command_players,
	'help' -> ['show player list']
};

command_alive = (_(args, msg) -> (
	data = { 'msg' -> msg };
	send_reply_callback(msg, {
		'content' -> random_quote(global_alive_quotes, data),
		'allowed_mentions' -> {}
	}, _(msg) -> (
		schedule(0, _(outer(msg)) -> print(player('*'), display_message(msg, true)));
	));
));
commands:'alive' = {
	'run' -> command_alive,
	'help' -> ['check if it\'s alive']
};

commands:'deathboard' = {
	'run' -> _(args, msg) -> mc_run_command('deathboard'),
	'help' -> ['show the deathboard leaderboard board']
};

commands:'run' = {
	'run' -> _(args, msg) -> mc_run_command(args),
	'help' -> ['execute minecraft commands (admins only)'],
	'admin' -> true
};

commands:'script' = {
	'run' -> _(args, msg) -> mc_run_command(str('script %s', args)),
	'help' -> ['execute scarpet things (admins only)'],
	'admin' -> true
};

system_variable_set('dcbridge_commands', commands);



__on_start() -> send_message(str('🌄  %s', random_quote(global_bridge_quotes:'start', null)));
__on_close() -> send_message_sync(str('🎑  %s', random_quote(global_bridge_quotes:'close', null)));

__on_player_message(p, content) -> (
	send_message(str('**<%s>** %s', escape_md(p~'display_name'), content));
);

__on_player_command(p, command, outer(display_flex)) -> (
	if (command~'^say' && !p,
		text = replace(command, '^say ', '');
		send_message(str('[Server] %s', text));
	);

	if (command~'^flex', call(display_flex, p));
);

__on_system_message(text, type) -> (
	if (type~'chat.type.(text|admin)', return());

	send_message(text);
);

__on_discord_message(msg) -> (
	user = msg~'user';

	if (!user || user~'is_self' || msg~'channel' != global_config:'channel', return());
	if (msg~'content'~(global_config:'prefix'), return(dcbridge_run_command(msg)));

	text = display_message(msg, false);

	print(player('*'), text);
	logger(text);
);