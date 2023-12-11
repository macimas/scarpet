help(section) -> (
	if (section == null, return(
		print('/pingmen §7-§e half-baked, simple discord-like §6@§e§lpings§r§e.');
		print('§8/pingmen§r add/remove §7- add or remove your pingable');
		print('§8/pingmen§r reset §7- reset your pingables to default');
		print('§8/pingmen§r list §7- list someones pingables');
		print('§8/pingmen§r config §7- tweak config');
		print('§8/pingmen§r debug §7- debugging and miscellaneous');
	));
	if (section == 'config', return(
		print('/pingmen config §7-§e tweakery tweekaroo');
		print('§6very wip.');
		print('§8/pingmen config§r pingSound §7- change ping sound');
	));
	bell_say('invalid page');
);

__config() -> {
	'scope' -> 'global',
	'commands' -> {
		'' -> ['help', null],
		'help' -> ['help', null],
		'help <help>' -> 'help',
		'add <text>' -> 'add_pingable',
		'remove' -> ['remove_pingable', null],
		'remove <your_pingable>' -> 'remove_pingable',
		'reset' -> ['reset_pingables', null],
		'reset <text>' -> 'reset_pingables',
		'list' -> ['list_pingables', null],
		'list <player>' -> 'list_pingables',
		'config' -> ['help', 'config'],
		'config <config>' -> ['tweaky_config', null],
		'config <config> <text>' -> 'tweaky_config',
		'debug' -> 'debug',
		'debug init' -> 'init',
		'debug save_data' -> 'save_data',
		'debug bell' -> 'bell'
	},
	'arguments' -> {
		'player' -> { 'type' -> 'term', 'suggester' -> _(args) -> (
			players = {};
			for (global_all_pingables, players += _);
			keys(players);
		)},
		'your_pingable' -> { 'type' -> 'term', 'suggester' -> _(args) -> (
			p = str(player());
			pingables = {};
			for (global_all_pingables:p, pingables += _);
			keys(pingables)
		)},
		'help' -> { 'type' -> 'term', 'suggest' -> [] },
		'config' -> { 'type' -> 'term', 'suggest' -> ['pingSound'] },
		'text' -> { 'type' -> 'text', 'suggest' -> [] },
	}
};

sounds = sound();



__on_player_message(sender, content) -> ( // the main thing. looks really iffy
	if (!content~'@', return());
	the_rest = player('all');
	load_data();

	for (global_all_pingables,
		msg = content;
		p = player(_);
		isPing = false;

		for (global_all_pingables:str(p),
			pingable = _;
			msg_split = split(' ', msg);
			for (msg_split,
				if (_ == '@'+pingable && (pingable != 'here' || !p~'team'~'afkDis.afk'),
					msg = replace(msg, '@'+pingable, '§6@§e§l'+pingable+'§r');
					isPing = true;
				)
			)
		);

		if (isPing && for (player('all'), str(_)~p),
			delete(the_rest, the_rest~_);
			mimicry(_, sender, msg);
			run('/playsound '+global_all_configs:str(p):'pingSound'+' player '+p+' '+str('%f %f %f', p~'pos'));
		)
	);

	mimicry(the_rest, sender, content);

	'cancel'
);

mimicry(p, sender, content) -> (
	print(p, '<'+player(sender)~'display_name'+'> '+content);
);



__on_player_connects(p) -> init();
__on_start() -> init_all(); 

init() -> (
	load_data();
	bell_say('§e/pingmen in your chat!')
);

init_all() -> ( // reinitialize /pingmen for everyone after /script load pingmen
	for (player('all'), run('execute as '+_+' run pingmen debug init'));
);

bell_say(msg) -> (
	msg = replace(msg, '\n', '\n§8---§r');
	print(format('y 🔔', '^y ding\n§6pls note that non-op players might be kicked for spam if clikity bell too much', '!/pingmen debug bell', 'd  - ', 'w '+msg));
);

bell() -> (
	p = player();
	bell = p~'pos' - [rand(2)-0.5, rand(-2.48), rand(2)-0.5];
	draw_shape('block', 6, ['block', 'bell', 'pos', bell, 'lean', rand(180), 'turn', rand(180)]);
	sound('block.bell.use', bell);
);



debug() -> (
	print('all pingables: §e'+global_all_pingables);
	print('your pingables: §e'+global_all_pingables:str(player()));
);

wipe_everything() -> (
	if (!player()~'permission_level'~'4', return(bell_say('youre not an op')));
	write_file('pingables', 'json', '');
	write_file('playerConfigs', 'json', '');
	init_all();
	bell_say('ok, wiped everything');
);



save_data() -> (
	write_file('pingables', 'json', global_all_pingables);
	write_file('playerConfigs', 'json', global_all_configs);
);

load_data() -> (
	p = str(player());
	global_all_pingables = read_file('pingables', 'json') || {};
	global_all_configs = read_file('playerConfigs', 'json') || {};

	if (!global_all_pingables~p,
		global_all_pingables:p = [p, 'everyone', 'here'];
		save_data();
		bell_say('§6no pingables; adding defaults')
	);
	if (!global_all_configs~p,
		global_all_configs:p = {
			'pingSound' -> 'entity.arrow.hit_player',
		};
		save_data();
		bell_say('§6no config, adding defaults')
	)
);

tweaky_config(config, value) -> (
	p = str(player());
	q = global_all_configs:p:config;
	if (!q, return(bell_say('config '+config+' doesnt exist')));
	if (!value, return(bell_say(config+' value: §e'+q)));

	global_all_configs:p:config = value;
	save_data();
	bell_say('ok, set '+config+' to '+value);
);



add_pingable(pingable) -> (
	p = str(player());
	if (pingable~'§', return(bell_say('do not use section signs, or i will kick your ass')));
	if (global_all_pingables:p~pingable != null, return(bell_say('you already have §e'+pingable)));
	global_all_pingables:p += pingable;
	save_data();
	bell_say('ok, added §e'+pingable);
);

remove_pingable(pingable) -> (
	p = str(player());
	if (pingable == null, return(
		bell_say('click one of your pingables to remove it');
		for (global_all_pingables:p,
			print('§8--- §6'+(_i+1)+' '+format('yu '+_, '^w remove §6@§e§l'+_, '?/pingmen remove '+_))
		)
	));
	if (global_all_pingables:p~pingable == null, return(bell_say('§e'+pingable+'§r doesnt exist')));
	global_all_pingables:p = filter(global_all_pingables:p, _ != pingable);
	save_data();
	bell_say('ok, removed §e'+pingable);
);

list_pingables(p) -> (
	load_data();
	if (p == null, p = str(player()));

	pingables = global_all_pingables:p;
	text = p+' pingables:\n §e'+pingables;

	if (pingables == null || pingables == [], text = p+' has no pingables');
	if (p == player(), text = 'your pingables:\n §e'+pingables);
	bell_say(text);
);

reset_pingables(confirm) -> ( 
	ok = 'yes pls do';
	if (confirm == null, return(bell_say('§oare you sure that you want to reset pingables to default?§r\n if so, do §6/pingmen reset '+ok)));
	if (confirm != ok, return(bell_say('wrong confirmation')));

	p = str(player());
	global_all_pingables:p = [p, 'everyone', 'here'];
	save_data();
	bell_say('ok, pingables have been reset to default');
);