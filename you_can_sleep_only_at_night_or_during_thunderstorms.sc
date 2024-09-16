__config() -> {
	'scope' -> 'global'
};

say(msg) -> (
	print(player('*'), msg);
);

__on_player_interacts_with_block(p, h, block, face, hitvec) -> (
	if (block~'_bed' && p~'pose' != 'sleeping', (
		say(str('%s, you can sleep only at night or during thunderstorms', p));
	));
);