// dcmc.sc - half-baked & bloated dc <-> mc bridge

__config() -> {
	'scope' -> 'global',
	'bot' -> 'dcmc'
};

import('mmm', ...import('mmm'));



// this is stupid

global_pcc_template = {
	'use_chatimage' -> false
};

global_cc = read_file('dcmc', 'json');
if (!global_cc, throw('no config'));

global_cc:'channel' = dc_channel_from_id(global_cc:'channel');
if (!global_cc:'channel', throw('invalid channel'));

// these will probably be replaced with something else
global_cc:'dc_name_color' = '#5891fc';
global_cc:'dc_msg_color' = '#d7f0f2';

update_pcc() -> (
	write_file('player', 'json', global_pcc || {});
	global_pcc = global_pcc || read_file('player', 'json') || {};
);

update_pcc();



cc() -> global_cc;
pcc(p) -> (
	p = str(p);
	update = false;

	if (!global_pcc:p, (
		global_pcc:p = global_pcc_template;
		update = true;
	));

	for (keys(global_pcc_template), (
		if (!global_pcc:p:_, (
			global_pcc:p:_ = global_pcc_template:_;
		));
		update = true;
	));

	if (update, update_pcc());

	return (global_pcc:p);
);



create_dc_msg_sync(data, options) -> (
	cc = cc();

	msg = dc_send_message(cc:'channel', data);

	if (!msg, (
		print(player('*'), '§c§l§ouh oh!!! failed to bridge message!!!§r §c§omaybe it was too long, bad request, or smth?? i honestly dont know, discarpet wont tell me!!');
		dc_send_message(cc:'channel', '***uh oh!!! failed to bridge message!!!** maybe it was too long, bad request, or smth?? i honestly dont know, discarpet wont tell me!!*');
	));
);

create_dc_msg(data, options) -> (
	task(_(outer(data), outer(options)) -> (
		create_dc_msg_sync(data, options);
	));
);

create_dc_reply(data, reply_to, options) -> (
	if (type(data) == 'string', (
		data = { 'content' -> data };
	));

	data:'reply_to' = reply_to;
	
	create_dc_msg(data, options);
);

display_dc_msg(msg, is_ref_msg, p) -> (
	cc = cc();
	ref_msg = msg~'referenced_message';
	user = msg~'user';
	server = cc:'channel'~'server';
	dc_name_color = cc:'dc_name_color';
	dc_msg_color = cc:'dc_msg_color';

	to_send = if (p == 0, player('*'), [p]);

	for (to_send, (
		compose = [];

		if (ref_msg && !is_ref_msg, (
			display_dc_msg(ref_msg, 1, _);
		));

		if (is_ref_msg, (
			compose += format(str('%s ╔☞', dc_name_color));
		));

		space = if (msg~'content', ' ', '');

		compose += format(
			str('%s [', dc_name_color),

			str('%s %s', dc_get_user_color(user, server) || dc_name_color, dc_get_display_name(user, server)),
				str('^w %s\n§7Click to copy mention', user~'name'),
				str('&%s', user~'mention_tag' + ' '),

			str('%s ]', dc_name_color),

			str('%s %s', dc_msg_color, parse_md(space + msg~'readable_content'))
		);

		if (msg~'attachments', (
			compose += yoink_attachments(msg, _);
		));

		if (msg~'sticker_ids', (
			compose += yoink_stickers(msg, _);
		));

		delim = if (is_ref_msg || !msg~'content', ' ', '\n');

		print(_, fjoin(delim, compose));	
	));
);

display_mc_msg(p, msg) -> (
	data = {
		'content' -> str('**<%s>** %s', p, msg),
		'embeds' -> []
	};

	waypoint = parse_xaero_waypoint(msg);

	if (waypoint, data:'embeds' += {
		'title' -> str('\\📌 %s `[%s]`', waypoint:'name', waypoint:'marker'),
		'description' -> str(
			'located at **[ %s / %s / %s ]** within **%s**',
			waypoint:'x',
			waypoint:'y',
			waypoint:'z',
			waypoint:'dimension'
		),
		'color' -> color_code_to_hex(waypoint:'color', 0)
	});

	create_dc_msg(data, {});
);

display_mc_sys_msg(msg, type) -> (
	data = {
		'content' -> join('\n', msg),
		'embeds' -> []
	};

	raw_msg = decode_json(encode_json(msg));

	if (type~'^chat.type.advancement', (
		base = raw_msg:'with':1:'with':0:'hoverEvent':'contents';
		name = translate_key(base:'translate');
		description = translate_key(base:'extra':1:'translate');
		color = base:'color';

		data:'embeds' += {
			'title' -> ' ',
			'description' -> join('\n', [
				str('\\🏆️ **%s**', name),
				str('---> *%s*', description),
			]),
			'color' -> color_name_to_hex(color, 0);
		};
	));

	//data:'content' += str(' **`[%s]`**', type);

	create_dc_msg(data, {});
);

// for itemflexer <3
display_flex(p) -> (
	item = p~'holds';
	if (!item, return());

	nbt = parse_nbt(p~'holds':2 || {});
	item_display = yoink_item_name(item, { 'no_count' -> true, 'no_bold' -> true });
	item_title = yoink_item_name(item, { 'no_count' -> true });
	item_raw_name = str('**%s**\n%s', item_display_name(item:0), item:0); 
	contents_color = 0xd35e39;

	data = {};
	data:'content' = str('%s is flexing their [%s]', p, item_display);

	fields = [];

	if (nbt~'Enchantments' || nbt~'StoredEnchantments', (
		enchantments = yoink_enchantments(nbt:'Enchantments' || nbt:'StoredEnchantments', true);
		
		fields += {
			'name' -> 'ENCHANTMENTS \\📖',
			'value' -> enchantments
		};
	));

	data:'embeds' = [{
		'title' -> item_title,
		'description' -> item_raw_name,
		'fields' -> fields,
		'color' -> 0xb0b7b8
	}];

	create_dc_msg(data, {});

	// display travelers backpack info

	is_tb_item = item:0~'travelersbackpack:([a-z_]+)';
	is_tb_upgrade = item:0~'upgrade$';
	
	if (is_tb_item && !is_tb_upgrade, (
		tb_tiers = ['Leather', 'Iron', 'Gold', 'Diamond', 'Netherite'];

		tb_data = {};
		tb_data:'content' = ' ';

		tb_info = [
			str('**%s** (Tier %s)', tb_tiers:(nbt:'Tier'), nbt:'Tier' + 1)
		];

		has_crafting_table = (nbt:'CraftingSettings':0 == 1);

		if (has_crafting_table, (
			tb_info += '**Crafting Table**'
		));

		display_tank(tank, outer(nbt)) -> (
			tb_tank_content = cc():'travelersbackpack_tank_content';

			if (tank == 'left', tank = 'LeftTank');
			if (tank == 'right', tank = 'RightTank');

			content = nbt:tank:'variant':'fluid';
			content = if (content == 'minecraft:empty', (
				'*Empty tank*',
				str('**%s**', tb_tank_content:content || content);
			));

			calc_fluid(n) -> (round(n * 0.01234567890123));

			return (str(
				'%s\n**%s**/%s',
				content,
				calc_fluid(nbt:tank:'amount'),
				calc_fluid(nbt:tank:'capacity')
			));
		);
		
		fields = [{
			'name' -> 'LEFT TANK \\🌗',
			'value' -> display_tank('left'),
			'inline' -> 'true'
		}, {
			'name' -> 'RIGHT TANK \\🌓',
			'value' -> display_tank('right'),
			'inline' -> true
		}, {
			'name' -> 'TOOLS \\🛠️',
			'value' -> str(
				'**%s**/%s slots',
				length(nbt:'ToolsInventory':'Items'),
				nbt:'ToolsInventory':'Size'
			)
		}];

		for (nbt:'ToolsInventory':'Items', (
			list = [_:'id'];

			if (_:'tag':'Enchantments', (
				list += yoink_enchantments(_:'tag':'Enchantments', false);
			));

			fields += {
				'name' -> yoink_item_name([_:'id', _:'Count', encode_nbt(_:'tag')], { 'no_bold' -> 'false' }),
				'value' -> join('\n', list),
				'inline' -> true
			};
		));

		tb_data:'embeds' = [{
			'title' -> '\\🎒 backpack \\🎒',
			'description' -> join(', ', tb_info),
			'fields' -> fields,
			'color' -> contents_color
		}];

		if (!nbt:'Inventory', tb_data:'embeds' = [{
			'title' -> 'Hmmm... This backpack has not been used yet.',
			'description' -> ' ',
			'color' -> contents_color
		}]);

		create_dc_msg(tb_data, {});
	));

	// display item contents such as from shulkers, with support for other mods

	contents = {};
	contents_lookup = [
		'Items',
		'BlockEntityTag.Items',
		'Inventory.Items',
	];

	for (contents_lookup, (
		content = parse_nbt(get(item:2, _));
		if (content == 'null', continue());
		
		for (content, (
			id = _:'id';
			count = _:'Count';

			if (contents:id, (
				contents:id += count,
				contents:id = count;
			));
		));

		break();
	));

	if (contents, (
		contents = sort_key(pairs(contents), -_:1);
		list = [];

		for (contents, (
			list += yoink_item_name([_:0, _:1, null], {});
		));

		contents_data = {};
		contents_data:'content' = ' ';
		contents_data:'embeds' = [{
			'title' -> '\\🎒 contents \\🎒',
			'description' -> join('\n', list),
			'color' -> contents_color
		}];

		create_dc_msg(contents_data, {});
	));
);

yoink_item_name(item, options) -> (
	name = item_display_name(item);
	name = replace(name, '§.');

	if (!options:'no_bold', (
		name = str('**%s**', name);
	));

	if (!options:'no_count' && item:1 != 1, (
		name = str('%s × %s', name, item:1);
	));

	return (name);
);

yoink_enchantments(enchantments, horizontal) -> (
	list = [];

	for (enchantments, (
		enchantment = translate_key(str('enchantment.%s', replace(_:'id', ':', '.')));
		level = _:'lvl';

		list += if (_:'lvl' == 1, (
			str('[**%s**]', enchantment),
			str('[**%s %s**]', enchantment, level);
		));
	));

	delim = if (horizontal, '⠀', '\n');

	return (join(delim, list));
);

yoink_attachments(msg, p) -> (
	cc = cc();
	dc_name_color = '#'+cc:'dc_name_color';

	attachments = [format('y ✉ files:')];

	for (msg~'attachments', (
		if (pcc(p):'use_chatimage' && _~'is_image', (
			attachments += str(
				'[[CICode,name=%s,url=%s]]',
				_~'file_name',
				_~'url'
			);
			continue();
		));

		attachments += format(
			str('%s [%s]', dc_name_color, _~'file_name'),
			str('^g %s', _~'url'),
			str('@%s', _~'url')
		);
	));

	return (fjoin(' ', attachments));
);

yoink_stickers(msg, p) -> (
	cc = cc();
	dc_name_color = '#'+cc:'dc_name_color';

	stickers = [format('y ☀ stickers:')];

	for (msg~'sticker_ids', (
		_ = dc_sticker_from_id(_);

		stickers += format(
			str('%s [%s]', dc_name_color, _~'name'),
			str('^g %s', _~'description' || _~'name'),
			str('@https://media.discordapp.net/stickers/%s.png', _~'id')
		);
	));

	return (fjoin(' ', stickers));
);



cmds = {};

get_cmds(outer(cmds)) -> (
	return ({ ...cmds, ...system_variable_get('dcmc_cmds', {}) })
);


cmds:'help' = {};
cmds:'help':'help' = [
	'help',
	'help!!!'
];

cmds:'help':'args' = ['msg'];
cmds:'help':'callback' = (_(msg) -> (
	table = {};
	list = ['very wip'];

	for (pairs(get_cmds()), (
		cmd = _:0;
		data = _:1;
		category = (data:'category' || 'commands');

		if (!table:category, table:category = {});
		table:category:cmd = data;
	));

	for (pairs(table), (
		category = _:0;
		cmds = _:1;

		list += str('**%s**', category);

		for (pairs(cmds), (
			cmd = _:0;
			data = _:1;

			list += str('[**%s**] - %s', cmd, data:'help':0 || '*No help available*');
		));
	));

	return (join('\n', list));
));


cmds:'players' = {};
cmds:'players':'help' = [
	'list who is online'
];

cmds:'players':'callback' = (_() -> (
	list = ['**Players:**'];

	for (player('*'), (
		list += str('\\- **%s**', _);
	));

	if (!cc():'remove_herobrine' && !rand(64), (
		list += str('\\- **%s**', 'Herobrine');
	));

	return (join('\n', list));
));


cmds:'time' = {};
cmds:'time':'help' = [
	'show the time'
];
cmds:'time':'callback' = (_() -> (
	time = day_time() / 24000;
	cycle = if (round(time - floor(time)),
		'\\🌕️',
		'\\☀️'
	);

	// bad
	weather = weather();
	if (weather == 'clear', weather = '\\✨');
	if (weather == 'rain', weather = '\\🌧️');
	if (weather == 'thunder', weather = '\\⛈️');

	return (join('\n', [
		str('%s **Day %s** %s', cycle, floor(time), weather),
		str('(%s)', time)
	]));
));


cmds:'run' = {};
cmds:'run':'help' = [
	'run mc commands'
];

cmds:'run':'args' = ['user', 'input'];
cmds:'run':'callback' = (_(user, input) -> (
	if (cc():'admins'~(user~'id') == null, (
		return('you are not an admin.');
	));

	run = run(input);

	result = join('\n', [
		'```yaml',
		(run:2 || join('\n', run:1) || 'There is nothing.'),
		'```'
	]);

	return (result);
));


cmds:'script' = {};
cmds:'script':'help' = [
	'run scarpet functions'
];

cmds:'script':'args' = ['user', 'input'];
cmds:'script':'callback' = (_(user, input) -> (
	if (cc():'admins'~(user~'id') == null, (
		return('you are not an admin.');
	));

	run = run(str('script run %s', input));

	result = join('\n', [
		'```yaml',
		(run:2 || join('\n', run:1) || 'There is nothing.'),
		'```'
	]);

	return (result);
));



// this looks stupid

commander(msg, prefix, outer(cmds)) -> (
	input = msg~'readable_content';

	cmds = get_cmds();
	cmd_regex = str('^%s(\\S+)', prefix);
	cmd_name = input~cmd_regex;
	cmd_input = replace(input, (cmd_regex+'\\s?'), '');
	cmd = cmds:cmd_name;

	if (!cmd, (
		return (dc_react(msg, cc():'cmd_reject_emoji'));
	));

	list = [];

	args_lookup = {
		'msg' -> msg,
		'user' -> msg~'user',
		'input' -> cmd_input
	};

	show_output(output, outer(msg), outer(cmd_name)) -> (
		create_dc_reply(output, msg, {});

		print(player('*'), format(
			str(
				'#%s %s ran §6%s',
				cc():'dc_msg_color',
				msg~'user'~'name',
				cmd_name
			)
		));
	);

	for (keys(args_lookup), (
		if (cmd:'args'~_ != null, list += args_lookup:_);
	));

	if (type(cmd:'callback') == 'function', (
		show_output(call(cmd:'callback', ...list));
	));

	if (type(cmd:'callback') == 'string', (
		handle_event(str('return_%s', cmd:'callback'), _(output) -> (
			print('mimas', output);
			show_output(output);
		));

		if (list, (
			signal_event(cmd:'callback', null, ...list),
			'mimas', signal_event(cmd:'callback', null, null);
		));
	));
);



__on_system_message(msg, type) -> (
	cc = cc();

	// prevent select system messages from being bridged,
	// ex. player messages, since they are handled separately
	if (cc:'ignored_sys_msgs'~type != null || type == null, return());

	display_mc_sys_msg(msg, type);
);

__on_player_message(p, msg) -> (
	display_mc_msg(p, msg);
);

__on_player_command(p, cmd) -> (
	if (cmd~'^flex', display_flex(p));
);

__on_discord_message(msg) -> (
	cc = cc();
	input = msg~'readable_content';

	if (msg~'channel' != cc:'channel', return());
	if (msg~'user'~'is_self', return());

	for (cc:'cmd_prefixes', (
		prefix_raw = _;
		prefix = escape_regex(_);

		if (!input~('^' + prefix), continue());

		task(_(outer(msg), outer(prefix)) -> (
			commander(msg, prefix);
		));

		return();
	));

	display_dc_msg(msg, 0, 0);
);



__on_start() -> create_dc_msg('dcmc.sc - bridging intrusive thoughts', {});

__on_close() -> create_dc_msg_sync('no longer', {});

logger('dcmc.sc - bridging intrusive thoughts');